package com.wizzardo.http;

import com.wizzardo.epoll.ByteBufferProvider;
import com.wizzardo.epoll.Connection;
import com.wizzardo.epoll.EpollCore;
import com.wizzardo.epoll.IOThread;
import com.wizzardo.epoll.readable.ReadableByteArray;
import com.wizzardo.http.mapping.Path;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;
import com.wizzardo.http.response.Status;
import com.wizzardo.http.utils.AsciiReader;
import com.wizzardo.tools.misc.ExceptionDrivenStringBuilder;
import com.wizzardo.tools.misc.Unchecked;
import com.wizzardo.tools.misc.pool.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by wizzardo on 28.05.15.
 */
public class ProxyHandler implements Handler {

    protected static final Pool<ExceptionDrivenStringBuilder> BUILDER_POOL = new PoolBuilder<ExceptionDrivenStringBuilder>()
            .supplier(ExceptionDrivenStringBuilder::new)
            .queue(PoolBuilder.createThreadLocalQueueSupplier())
            .resetter(sb -> sb.setLength(0))
            .build();

    protected static final MultiValue<String> EMPTY_VALUE = new MultiValue<>();

    protected Queue<ProxyConnection> connections = new LinkedBlockingQueue<>();
    protected EpollCore<ProxyConnection> epoll = new EpollCore<ProxyConnection>() {
        @Override
        protected ProxyConnection createConnection(int fd, int ip, int port) {
            return new ProxyConnection(fd, ip, port);
        }

        @Override
        protected IOThread<ProxyConnection> createIOThread(int number, int divider) {
            return new ProxyWorker(number, divider);
        }
    };

    static class ProxyWorker extends IOThread<ProxyConnection> implements Buffer {
        protected byte[] buffer = new byte[getBuffer().capacity()];
        protected int position;
        protected int limit;

        public ProxyWorker(int number, int divider) {
            super(number, divider);
        }

        @Override
        public void onRead(ProxyConnection connection) throws IOException {
            connection.onRead(this);
        }

        @Override
        public byte[] bytes() {
            return buffer;
        }

        @Override
        public int position() {
            return position;
        }

        @Override
        public void position(int position) {
            this.position = position;
        }

        @Override
        public int limit() {
            return limit;
        }

        @Override
        public void limit(int limit) {
            this.limit = limit;
        }

        @Override
        public int capacity() {
            return buffer.length;
        }

        @Override
        public boolean hasRemaining() {
            return position < limit;
        }

        @Override
        public int remains() {
            return limit - position;
        }

        @Override
        public void clear() {
            limit = 0;
            position = 0;
        }
    }

    {
        epoll.start();
    }

    protected String host;
    protected int port;

    public ProxyHandler(String host) {
        this(host, 80);
    }

    public ProxyHandler(String host, int port) {
        this.host = host;
        this.port = port;
    }

    class ProxyConnection extends Connection {
        Request srcRequest;
        Response srcResponse;
        volatile ResponseReader responseReader;
        final AtomicReference<Thread> processingBy = new AtomicReference<>();

        final byte[] buffer = new byte[16 * 1024];
        volatile boolean recursive = false;
        volatile long limit;
        volatile boolean continueWrite = false;
        volatile boolean chunked = false;
        volatile boolean lastChunk = false;

        public ProxyConnection(int fd, int ip, int port) {
            super(fd, ip, port);
        }

        public void onRead(ByteBufferProvider byteBufferProvider) throws IOException {
            if (recursive)
                return;

            Thread current = Thread.currentThread();
            Thread processing = processingBy.get();
            if (processing != current && processing != null)
                processing = null;

            if (!processingBy.compareAndSet(processing, current))
                return;

            if (!srcRequest.connection().isAlive()) {
                processingBy.set(null);
                end();
                return;
            }
            try {
                int r;
                byte[] buffer = this.buffer;
                while ((r = read(buffer, byteBufferProvider)) > 0) {
                    byteBufferProvider.getBuffer().clear();
                    int offset = 0;
                    if (!responseReader.isComplete()) {
                        int k = responseReader.read(buffer, 0, r);
                        if (k < 0)
                            continue;

                        srcResponse.headersReset();
                        for (Map.Entry<String, MultiValue<String>> entry : responseReader.headers.entrySet()) {
                            String name = entry.getKey();
                            if (entry.getValue().size() > 1)
                                for (String value : entry.getValue().getValues()) {
                                    srcResponse.appendHeader(name, value);
                                }
                            else
                                srcResponse.appendHeader(name, entry.getValue().getValue());
                        }

                        offset = k;
                        MultiValue<String> length = responseReader.getHeaders().get(Header.KEY_CONTENT_LENGTH.value);
                        if (!responseReader.status.equals("200"))
                            srcResponse.setStatus(Status.valueOf(Integer.parseInt(responseReader.getStatus())));

                        srcResponse.commit(srcRequest.connection(), byteBufferProvider);
                        srcRequest.connection().flush();
                        chunked = Header.VALUE_CHUNKED.value.equalsIgnoreCase(responseReader.getHeaders().getOrDefault(Header.KEY_TRANSFER_ENCODING.value, EMPTY_VALUE).getValue());
                        if (length == null && !chunked) {
                            processingBy.set(null);
                            end();
                            return;
                        } else {
                            if (!chunked)
                                limit = Long.valueOf(length.getValue());
                            else
                                limit = readChunkLength(buffer, offset, r);
                        }
                    }

                    int l = r - offset;
                    if (l <= limit) {
                        limit -= l;
                        if (limit == 0 && (!chunked || lastChunk))
                            proxyWrite(new CustomReadableByteArray(buffer, offset, r - offset), byteBufferProvider);
                        else
                            proxyWrite(new CustomReadableByteArray(Arrays.copyOfRange(buffer, offset, r)), byteBufferProvider);
                    } else {
                        int o = offset;

                        while (l > 0) {
                            if (limit == 0) {
                                limit = readChunkLength(buffer, o, r);
                            }

                            if (o + limit > r) {
                                proxyWrite(new CustomReadableByteArray(Arrays.copyOfRange(buffer, o, r)), byteBufferProvider);
                                limit -= r - o;
                                l -= r - o;
                                o += r - o;
                            } else {
                                proxyWrite(new CustomReadableByteArray(Arrays.copyOfRange(buffer, o, (int) (o + limit))), byteBufferProvider);
                                l -= limit;
                                o += limit;
                                limit = 0;
                            }
                        }
                    }

//                System.out.println("response: " + new String(buffer, offset, r - offset));
//                System.out.println("need to read: " + limit + "; r:" + r + " ,offset:" + offset);
                    if (limit == 0 && (!chunked || lastChunk)) {
                        processingBy.set(null);
                        end();
                        return;
                    }

                    if (!continueWrite)
                        break;
                }
            } catch (Exception e) {
                close();
                srcRequest.connection().close();
                e.printStackTrace();
            }
//            System.out.println("wait "+srcRequest.connection().hasDataToWrite());
            processingBy.set(null);
        }

        protected long readChunkLength(byte[] bytes, int offset, int end) {
            long l = 0;
            int i = offset;
            int limit = Math.min(bytes.length, end);
            while (i < limit) {
                byte b = bytes[i++];
                if (b >= '0' && b <= '9') {
                    l = l * 16 + (b - '0');
                } else if (b >= 'A' && b <= 'F') {
                    l = l * 16 + (b - 'A' + 10);
                } else
                    break;
            }

            l += 4 + (i - offset - 1);
            lastChunk = l == 5;
            return l;
        }

        protected void proxyWrite(CustomReadableByteArray readable, ByteBufferProvider bufferProvider) {
            recursive = true;
            srcRequest.connection().write(readable, bufferProvider);
            recursive = false;
            continueWrite = readable.isComplete();
        }

        protected void end() throws IOException {
            if (srcRequest.header(Header.KEY_CONNECTION).equalsIgnoreCase(Header.VALUE_CLOSE.value))
                srcRequest.connection().setCloseOnFinishWriting(true);

            srcRequest.connection().onFinishingHandling();
            limit = -1;
            connections.add(this);
        }

        @Override
        public void close() {
            try {
                onRead(ByteBufferProvider.current());
            } catch (IOException e) {
                e.printStackTrace();
            }
            super.close();
        }

        private class CustomReadableByteArray extends ReadableByteArray {
            public CustomReadableByteArray(byte[] bytes) {
                super(bytes);
            }

            public CustomReadableByteArray(byte[] bytes, int offset, int length) {
                super(bytes, offset, length);
            }

            @Override
            public void onComplete() {
                Unchecked.run(() -> ProxyConnection.this.onRead((ByteBufferProvider) Thread.currentThread()));
            }
        }
    }

    @Override
    public Response handle(Request request, Response response) throws IOException {
        ProxyConnection connection = connections.poll();
        if (connection == null || !connection.isAlive()) {
//            System.out.println("create new connection " + (connection == null ? "" : ", not alive"));
            connection = epoll.connect(host, port);
        }

        connection.srcRequest = request;
        connection.srcResponse = response;
        connection.responseReader = new ResponseReader();
        response.async();

        final ProxyConnection finalConnection = connection;
        BUILDER_POOL.provide(requestBuilder -> {
            requestBuilder.append(request.method().name())
                    .append(" ").append(sb -> rewritePath(sb, request.path(), request.getQueryString()))
                    .append(" HTTP/1.1\r\n")

                    .append("Host: ").append(host)
                    .append(sb -> {
                        if (port != 80)
                            sb.append(":").append(port);
                    }).append("\r\n")

                    .append("X-Real-IP: ").append(request.connection().getIp()).append("\r\n")
                    .append("X-Forwarded-for: ").append(request.connection().getServer().getHostname()).append("\r\n");

            Map<String, MultiValue<String>> headers = request.headers();
            for (Map.Entry<String, MultiValue<String>> header : headers.entrySet()) {
                if (header.getKey().equalsIgnoreCase("Host"))
                    continue;
                if (header.getKey().equalsIgnoreCase("Connection"))
                    continue;

                if (header.getValue().size() == 1)
                    requestBuilder.append(header.getKey()).append(": ").append(header.getValue().getValue()).append("\r\n");
                else for (String value : header.getValue().getValues())
                    requestBuilder.append(header.getKey()).append(": ").append(value).append("\r\n");
            }
            requestBuilder.append("\r\n");
            //todo: post request

//        System.out.println("send request: " + requestBuilder);
            finalConnection.write(AsciiReader.write(requestBuilder.toString()), (ByteBufferProvider) Thread.currentThread());

            if (request.getBody() != null)
                finalConnection.write(request.data(), (ByteBufferProvider) Thread.currentThread());

            return null;
        });

        return response;
    }

    public void rewritePath(ExceptionDrivenStringBuilder requestBuilder, Path path, String query) {
        requestBuilder.append(sb -> path.toString(sb)).append('?').append(query);
    }
}
