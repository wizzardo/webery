package com.wizzardo.http;

import com.wizzardo.epoll.readable.ReadableByteBuffer;
import com.wizzardo.http.mapping.UrlMapping;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;
import com.wizzardo.http.response.Status;

import java.io.IOException;

/**
 * Created by wizzardo on 18.02.15.
 */
public class HttpServer<T extends HttpConnection> extends AbstractHttpServer<T> {
    private ReadableByteBuffer staticResponse = new Response()
            .appendHeader(Header.KEY_CONNECTION, Header.VALUE_CONNECTION_KEEP_ALIVE)
            .appendHeader(Header.KEY_CONTENT_TYPE, Header.VALUE_CONTENT_TYPE_HTML_UTF8)
            .setBody("It's alive!".getBytes())
            .buildStaticResponse();

    private byte[] serverName = "wizzardo-http/0.1".getBytes();
    protected FiltersMapping filtersMapping = new FiltersMapping();
    protected UrlMapping<Handler> urlMapping = new UrlMapping<>();
    protected ServerDate serverDate = new ServerDate();

    public HttpServer(int port) {
        this(null, port);
    }

    public HttpServer(String host, int port) {
        this(host, port, 0);
    }

    public HttpServer(String host, int port, int workersCount) {
        super(host, port, workersCount);

        urlMapping.append("/", (request, response) -> response.setStaticResponse(staticResponse.copy()));
    }

    public FiltersMapping getFiltersMapping() {
        return filtersMapping;
    }

    public UrlMapping<Handler> getUrlMapping() {
        return urlMapping;
    }

    @Override
    protected void handle(T connection) throws Exception {
        Request request = connection.getRequest();
        Response response = connection.getResponse();

        response.appendHeader(Header.KEY_DATE.bytes, serverDate.getDateAsBytes());
        response.appendHeader(Header.KEY_SERVER.bytes, serverName);

//            System.out.println(request.method() + " " + request.path() + " " + request.protocol());
//            System.out.println(request.headers());

        if (!filtersMapping.before(request, response))
            return;

        response = handle(request, response);

        filtersMapping.after(request, response);
    }

    protected Response handle(Request request, Response response) throws IOException {
        Handler handler = urlMapping.get(request);
        if (handler != null)
            response = handler.handle(request, response);
        else
            response.setStatus(Status._404).setBody(request.path() + " not found");
        return response;
    }

    public static void main(String[] args) {
        HttpServer server = new HttpServer(null, 8084, args.length > 0 ? Integer.parseInt(args[0]) : 0);
        server.setIoThreadsCount(args.length > 1 ? Integer.parseInt(args[1]) : 1);
        ReadableByteBuffer staticResponse = new Response()
                .appendHeader(Header.KEY_CONNECTION, Header.VALUE_CONNECTION_KEEP_ALIVE)
                .appendHeader(Header.KEY_CONTENT_TYPE, Header.VALUE_CONTENT_TYPE_HTML_UTF8)
                .setBody("ololo".getBytes())
                .buildStaticResponse();

        server.getUrlMapping()
//                        .append("/ololo", (request, response) -> response.setStaticResponse(staticResponse.copy()))
//                        .append("/*", new FileTreeHandler("/usr/share/nginx/html/", ""))
                .append("/*", new FileTreeHandler("/media/wizzardo/DATA/", ""))
//                        .append("/*", new FileTreeHandler("/home/wizzardo/", ""))
        ;
//        server.setHandler(new UrlHandler()
//                        .append("/static/*", new FileTreeHandler("/home/wizzardo/", "/static"))
//                        .append("/echo", new WebSocketHandler() {
//                            @Override
//                            public void onMessage(WebSocketListener listener, Message message) {
//                                System.out.println(message.asString());
//                                listener.sendMessage(message);
//                            }
//                        }).append("/time", new WebSocketHandler() {
//                            {
//                                final Thread thread = new Thread(() -> {
//                                    while (true) {
//                                        try {
//                                            Thread.sleep(1000);
//                                        } catch (InterruptedException ignored) {
//                                        }
//
//                                        broadcast(new Date().toString());
//                                    }
//                                });
//                                thread.setDaemon(true);
//                                thread.start();
//                            }
//
//                            ConcurrentLinkedQueue<WebSocketListener> listeners = new ConcurrentLinkedQueue<>();
//
//                            void broadcast(String message) {
//                                Message m = new Message().append(message);
//
//                                Iterator<WebSocketListener> iter = listeners.iterator();
//                                while (iter.hasNext()) {
//                                    WebSocketListener listener = iter.next();
//                                    listener.sendMessage(m);
//                                }
//                            }
//
//                            @Override
//                            public void onConnect(WebSocketListener listener) {
//                                listeners.add(listener);
//                            }
//                        })
//        );
        server.start();
    }
}
