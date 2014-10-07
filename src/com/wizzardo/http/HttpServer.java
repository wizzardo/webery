package com.wizzardo.http;

import com.wizzardo.epoll.EpollServer;
import com.wizzardo.epoll.IOThread;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;
import com.wizzardo.http.websocket.Frame;
import com.wizzardo.http.websocket.Message;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author: moxa
 * Date: 11/5/13
 */
public class HttpServer extends EpollServer<HttpConnection> {

    private Response staticResponse = new Response()
            .appendHeader(Header.KEY_CONNECTION, Header.VALUE_CONNECTION_KEEP_ALIVE)
            .appendHeader(Header.KEY_CONTENT_TYPE, Header.VALUE_CONTENT_TYPE_HTML_UTF8)
            .setBody("It's alive!".getBytes())
            .makeStatic();

    private BlockingQueue<HttpConnection> queue = new LinkedBlockingQueue<>();
    private int workersCount;
    private int sessionTimeoutSec = 30 * 60;
    private FiltersMapping filtersMapping = new FiltersMapping();
    private volatile Handler handler = (request, response) -> staticResponse;

    public HttpServer(int port) {
        this(null, port);
    }

    public HttpServer(String host, int port) {
        this(host, port, 0);
    }

    public HttpServer(String host, int port, int workersCount) {
        super(host, port);
        this.workersCount = workersCount;

        System.out.println("worker count: " + workersCount);
        for (int i = 0; i < workersCount; i++) {
            new Worker(queue, "worker_" + i) {
                @Override
                protected void process(HttpConnection connection) {
                    handle(connection);
                }
            };
        }
    }

    @Override
    public void run() {
        Session.createSessionsHolder(sessionTimeoutSec);
        super.run();
    }

    @Override
    protected HttpConnection createConnection(int fd, int ip, int port) {
        return new HttpConnection(fd, ip, port);
    }

    @Override
    protected IOThread<HttpConnection> createIOThread() {
        return new HttpIOThread();
    }

    private class HttpIOThread extends IOThread<HttpConnection> {

        @Override
        public void onRead(final HttpConnection connection) {
            if (connection.getState() == HttpConnection.State.READING_INPUT_STREAM) {
                connection.getInputStream().wakeUp();
                return;
            }

            if (connection.processListener())
                return;

            ByteBuffer b;
            try {
                while ((b = read(connection, connection.getBufferSize())).limit() > 0) {
                    if (connection.check(b))
                        break;
                }
                if (!connection.isRequestReady())
                    return;

            } catch (IOException e) {
                e.printStackTrace();
                connection.close();
                return;
            }

            if (workersCount > 0)
                queue.add(connection);
            else
                handle(connection);
        }

        @Override
        public void onWrite(HttpConnection connection) {
            if (connection.hasDataToWrite())
                connection.write();
            else if (connection.getState() == HttpConnection.State.WRITING_OUTPUT_STREAM)
                connection.getOutputStream().wakeUp();
        }
    }

    protected void handle(HttpConnection connection) {
        try {
            Request request = connection.getRequest();
            Response response = new Response();
            request.response(response);

            if (!filtersMapping.before(request, response)) {
                finishHandling(connection, response);
                return;
            }

            response = handler.handle(request, response);
            request.response(response);

            filtersMapping.after(connection.getRequest(), response);

            finishHandling(connection, response);
        } catch (Exception t) {
            t.printStackTrace();
            //TODO render error page
            connection.close();
        }
    }

    private void finishHandling(HttpConnection connection, Response response) throws IOException {
        if (connection.getState() == HttpConnection.State.WRITING_OUTPUT_STREAM)
            connection.getOutputStream().flush();

        if (response.isProcessed())
            return;

        connection.write(response.toReadableBytes());
        connection.onFinishingHandling();
    }

    public FiltersMapping getFiltersMapping() {
        return filtersMapping;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public void setSessionTimeout(int sec) {
        this.sessionTimeoutSec = sec;
    }

    public static void main(String[] args) {
        HttpServer server = new HttpServer(null, 8084, args.length > 0 ? Integer.parseInt(args[0]) : 0);
        server.setIoThreadsCount(args.length > 1 ? Integer.parseInt(args[1]) : 4);
        server.setHandler(new UrlHandler()
//                        .append("/static", new FileTreeHandler("/home/wizzardo/")) //todo ignore prefix
                        .append("/echo", new WebSocketHandler() {
                            @Override
                            public void onMessage(WebSocketListener listener, Message message) {
                                System.out.println(message.asString());
                                listener.sendMessage(message);
                            }
                        }).append("/time", new WebSocketHandler() {
                            {
                                final Thread thread = new Thread(() -> {
                                    while (true) {
                                        try {
                                            Thread.sleep(1000);
                                        } catch (InterruptedException ignored) {
                                        }

                                        broadcast(new Date().toString());
                                    }
                                });
                                thread.setDaemon(true);
                                thread.start();
                            }

                            ConcurrentLinkedQueue<WebSocketListener> listeners = new ConcurrentLinkedQueue<>();

                            void broadcast(String message) {
                                Message m = new Message();
                                Frame frame = new Frame();
                                frame.setData(message.getBytes());
                                m.add(frame);

                                Iterator<WebSocketListener> iter = listeners.iterator();
                                while (iter.hasNext()) {
                                    WebSocketListener listener = iter.next();
                                    listener.sendMessage(m);
                                }
                            }

                            @Override
                            public void onConnect(WebSocketListener listener) {
                                listeners.add(listener);
                            }
                        })
        );
        server.start();
    }
}
