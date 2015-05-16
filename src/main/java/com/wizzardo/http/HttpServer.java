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
            .appendHeader(Header.KEY_CONNECTION, Header.VALUE_KEEP_ALIVE)
            .appendHeader(Header.KEY_CONTENT_TYPE, Header.VALUE_HTML_UTF8)
            .setBody("It's alive!".getBytes())
            .buildStaticResponse();

    private byte[] serverName = "Server: wizzardo-http/0.1\r\n".getBytes();
    protected FiltersMapping filtersMapping;
    protected UrlMapping<Handler> urlMapping;
    protected ServerDate serverDate = new ServerDate();
    protected String context;

    public HttpServer(int port) {
        this(null, port);
    }

    public HttpServer(String host, int port) {
        this(host, port, null, 0);
    }

    public HttpServer(String host, int port, String context) {
        this(host, port, context, 0);
    }

    public HttpServer(String host, int port, int workersCount) {
        this(host, port, null, workersCount);
    }

    public HttpServer(String host, int port, String context, int workersCount) {
        super(host, port, workersCount);
        this.context = context;
    }

    protected UrlMapping<Handler> createUrlMapping(String host, int port, String context) {
        return new UrlMapping<>(host, port, context);
    }

    @Override
    public void run() {
        init();
        super.run();
    }

    protected void init() {
        String base = getHost().equals("0.0.0.0") ? "localhost" : getHost();
        if (isSecured())
            base = "https://" + base;
        else
            base = "http://" + base;

        urlMapping = createUrlMapping(base, getPort(), context);
        filtersMapping = new FiltersMapping(context);
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

        response.appendHeader(serverDate.getDateAsBytes());
        response.appendHeader(serverName);

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
}
