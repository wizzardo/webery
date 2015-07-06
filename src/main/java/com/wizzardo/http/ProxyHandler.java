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
            .resetter(sb -> sb.setLength(0))
            .build();

    protected Queue<ProxyConnection> connections = new LinkedBlockingQueue<>();
    protected EpollCore<ProxyConnection> epoll = new EpollCore<ProxyConnection>() {
        @Override
        protected ProxyConnection createConnection(int fd, int ip, int port) {
            return new ProxyConnection(fd, ip, port);
        }

        @Override
        protected IOThread<ProxyConnection> createIOThread(int number, int divider) {
            return new IOThread<ProxyConnection>(number, divider) {
                @Override
                public void onRead(ProxyConnection connection) {
                    connection.read(this);
                }
            };
        }
    };

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
        volatile long limit;

        public ProxyConnection(int fd, int ip, int port) {
            super(fd, ip, port);
        }

        protected void read(ByteBufferProvider byteBufferProvider) {
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
                    int offset = 0;
                    if (!responseReader.isComplete()) {
                        int k = responseReader.read(buffer, 0, r);
                        if (k < 0)
                            continue;

                        srcResponse.headersReset();
                        for (Map.Entry<String, MultiValue> entry : responseReader.headers.entrySet()) {
                            String name = entry.getKey();
                            if (entry.getValue().size() > 1)
                                for (String value : entry.getValue().getValues()) {
                                    srcResponse.appendHeader(name, value);
                                }
                            else
                                srcResponse.appendHeader(name, entry.getValue().getValue());
                        }

                        offset = k;
                        MultiValue length = responseReader.getHeaders().get(Header.KEY_CONTENT_LENGTH.value);
                        if (!responseReader.status.equals("200"))
                            srcResponse.setStatus(Status.valueOf(Integer.parseInt(responseReader.getStatus())));

                        srcResponse.commit(srcRequest.connection());
                        if (length == null) {
                            processingBy.set(null);
                            end();
                            return;
                        }
                        limit = Long.valueOf(length.getValue());
                    }

                    limit -= r - offset;
                    if (limit == 0)
                        proxyWrite(new CustomReadableByteArray(buffer, offset, r - offset), byteBufferProvider);
                    else
                        proxyWrite(new CustomReadableByteArray(Arrays.copyOfRange(buffer, offset, r)), byteBufferProvider);


//                System.out.println("response: " + new String(buffer, offset, r - offset));
//                System.out.println("need to read: " + limit + "; r:" + r + " ,offset:" + offset);
                    if (limit == 0) {
                        processingBy.set(null);
                        end();
                        return;
                    }

                    break;
                }
            } catch (Exception e) {
                try {
                    close();
                } catch (IOException ignored) {
                }
                try {
                    srcRequest.connection().close();
                } catch (IOException ignored) {
                }
                e.printStackTrace();
            }
//            System.out.println("wait "+srcRequest.connection().hasDataToWrite());
            processingBy.set(null);
        }

        protected void proxyWrite(CustomReadableByteArray readable, ByteBufferProvider bufferProvider) {
            srcRequest.connection().write(readable, bufferProvider);
        }

        protected void end() {
            if (srcRequest.header(Header.KEY_CONNECTION).equalsIgnoreCase(Header.VALUE_CLOSE.value))
                srcRequest.connection().setCloseOnFinishWriting(true);

            srcRequest.connection().onFinishingHandling();
            limit = -1;
            connections.add(this);
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
                Unchecked.run(() -> ProxyConnection.this.read((ByteBufferProvider) Thread.currentThread()));
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
                    .append("X-Forwarded-for: ").append(request.connection().getServer().getHost()).append("\r\n");

            Map<String, MultiValue> headers = request.headers();
            for (Map.Entry<String, MultiValue> header : headers.entrySet()) {
                if (header.getKey().equalsIgnoreCase("Host"))
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
        response.async();

        return response;
    }

    public void rewritePath(ExceptionDrivenStringBuilder requestBuilder, Path path, String query) {
        requestBuilder.append(sb -> path.toString(sb)).append('?').append(query);
    }
}
