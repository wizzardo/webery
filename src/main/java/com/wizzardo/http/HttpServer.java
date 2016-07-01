package com.wizzardo.http;

import com.wizzardo.epoll.SslConfig;
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
    protected boolean debug = false;

    public HttpServer() {
        init();
    }

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
        this(host, port, context, workersCount, null);
    }

    public HttpServer(String host, int port, String context, int workersCount, SslConfig sslConfig) {
        super(host, port, workersCount);
        this.context = context;
        if (sslConfig != null)
            loadCertificates(sslConfig);

        init();
    }

    protected UrlMapping<Handler> createUrlMapping() {
        return new UrlMapping<>();
    }

    public HttpServer<T> setDebugOutput(boolean enabled) {
        debug = enabled;
        return this;
    }

    @Override
    public synchronized void start() {
        onStart();
        started = true;
        super.start();
    }

    protected void onStart() {
        urlMapping.getTemplatesHolder()
                .setHost(getHost())
                .setPort(getPort())
                .setContext(context)
                .setIsHttps(isSecured());

        urlMapping.setContext(context);
        filtersMapping.setContext(context);
    }

    protected String getHost() {
        return getNetworkInterface();
    }

    protected void init() {
        urlMapping = createUrlMapping();
        filtersMapping = new FiltersMapping();
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

        if (debug) {
            System.out.println("request: ");
            System.out.println(request.method() + " " + request.path() + " " + request.protocol());
            System.out.println(request.params());
            System.out.println(request.headers());
        }

        if (!filtersMapping.before(request, response))
            return;

        response = handle(request, response);

        filtersMapping.after(request, response);
        if (debug) {
            System.out.println("response: ");
            System.out.println(response);
        }
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
