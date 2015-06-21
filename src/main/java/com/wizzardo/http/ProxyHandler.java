package com.wizzardo.http;

import com.wizzardo.epoll.ByteBufferProvider;
import com.wizzardo.epoll.Connection;
import com.wizzardo.epoll.EpollCore;
import com.wizzardo.epoll.IOThread;
import com.wizzardo.http.mapping.Path;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;
import com.wizzardo.http.response.Status;
import com.wizzardo.http.utils.AsciiReader;
import com.wizzardo.tools.misc.ExceptionDrivenStringBuilder;
import com.wizzardo.tools.misc.pool.Holder;
import com.wizzardo.tools.misc.pool.Pool;
import com.wizzardo.tools.misc.pool.SoftHolder;
import com.wizzardo.tools.misc.pool.ThreadLocalPool;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by wizzardo on 28.05.15.
 */
public class ProxyHandler implements Handler {

    protected static final Pool<ExceptionDrivenStringBuilder> BUILDER_POOL = new ThreadLocalPool<ExceptionDrivenStringBuilder>() {
        @Override
        public ExceptionDrivenStringBuilder create() {
            return new ExceptionDrivenStringBuilder();
        }

        @Override
        protected Holder<ExceptionDrivenStringBuilder> createHolder(ExceptionDrivenStringBuilder builder) {
            return new SoftHolder<ExceptionDrivenStringBuilder>(this, builder) {
                @Override
                public ExceptionDrivenStringBuilder get() {
                    ExceptionDrivenStringBuilder builder = super.get();
                    builder.setLength(0);
                    return builder;
                }
            };
        }
    };

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
                    try {
                        connection.read(this);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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

        final byte[] buffer = new byte[16 * 1024];
        long limit;

        public ProxyConnection(int fd, int ip, int port) {
            super(fd, ip, port);
        }

        protected void read(ByteBufferProvider byteBufferProvider) throws IOException {
            int r;
            while ((r = read(buffer, byteBufferProvider)) > 0) {
                int offset = 0;
                if (!responseReader.isComplete()) {
                    int k = responseReader.read(buffer, 0, r);
                    if (k < 0)
                        return;

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
                        end();
                        break;
                    }
                    limit = Long.valueOf(length.getValue());
                }

                limit -= r - offset;
                if (limit == 0)
                    srcRequest.connection().write(buffer, offset, r - offset, byteBufferProvider);
                else
                    srcRequest.connection().write(Arrays.copyOfRange(buffer, offset, r), byteBufferProvider);


//                System.out.println("response: " + new String(buffer, offset, r - offset));
//                System.out.println("need to read: " + limit + "; r:" + r + " ,offset:" + offset);
                if (limit == 0) {
                    end();
                    break;
                }
            }
        }

        protected void end() {
            srcRequest.connection().onFinishingHandling();
            connections.add(this);
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
        try (Holder<ExceptionDrivenStringBuilder> holder = BUILDER_POOL.holder()) {
            ExceptionDrivenStringBuilder requestBuilder = holder.get();

            //todo: append params
            requestBuilder.append(request.method().name()).append(" ").append(rewritePath(request.path())).append(" HTTP/1.1\r\n");
            requestBuilder.append("Host: " + host);
            if (port != 80)
                requestBuilder.append(":").append(port);
            requestBuilder.append("\r\n");
            requestBuilder.append("X-Real-IP: ").append(request.connection().getIp()).append("\r\n");
            requestBuilder.append("X-Forwarded-for: ").append(request.connection().getServer().getHost()).append("\r\n");
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
            connection.write(AsciiReader.write(requestBuilder.toString()), (ByteBufferProvider) Thread.currentThread());
        }
        response.async();


        return response;
    }

    public String rewritePath(Path path) {
        return path.toString();
    }
}
