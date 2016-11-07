package com.wizzardo.http;

import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;
import com.wizzardo.tools.http.HttpClient;
import com.wizzardo.tools.http.HttpSession;
import com.wizzardo.tools.io.IOTools;
import com.wizzardo.tools.misc.Unchecked;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import java.io.IOException;

/**
 * @author: moxa
 * Date: 5/7/13
 */
public class ServerTest<S extends HttpServer> {

    protected S server;
    protected int workers = 4;
    protected int port = 9999;
    protected volatile Handler handler;
    protected String context;

    @Rule
    public TestName name = new TestName();

    @Before
    public void setUp() throws NoSuchMethodException, ClassNotFoundException, NoSuchFieldException {
        System.out.println("setUp " + this.getClass().getSimpleName() + "." + name.getMethodName());
        server = (S) new HttpServer<HttpConnection>(null, port, context, workers) {
            @Override
            protected Response handle(Request request, Response response) throws IOException {
                response.setHeader(Header.KEY_CONNECTION, Header.VALUE_CLOSE);
                return handler.handle(request, response);
            }

            {
                filtersMapping = new FiltersMapping() {
                    @Override
                    public FiltersMapping addBefore(String url, Filter handler) {
                        return super.addBefore(url, new FilterWrapper(url, handler));
                    }

                    @Override
                    public FiltersMapping addAfter(String url, Filter handler) {
                        return super.addAfter(url, new FilterWrapper(url, handler));
                    }
                };

            }

            @Override
            public MimeProvider getMimeProvider() {
                return new MimeProvider() {
                    @Override
                    protected void init() throws IOException {
                    }
                };
            }

            @Override
            protected void onError(HttpConnection connection, Exception e) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            }
        };
        server.setIoThreadsCount(1);
        server.start();
        try {
            Thread.sleep(25); // wait for server startup
        } catch (InterruptedException ignored) {
        }
    }

    @After
    public void tearDown() throws InterruptedException {
        System.out.println("tearDown " + this.getClass().getSimpleName() + "." + name.getMethodName());
        server.close();
        handler = null;
        context = null;
    }

    public String name() {
        return name.getMethodName();
    }

    public String path() {
        return "/" + name();
    }

    public String get() {
        return Unchecked.call(() -> makeRequest(path()).get().asString());
    }

    static class FilterWrapper implements Filter {
        String mapping;
        Filter filter;

        public FilterWrapper(String mapping, Filter filter) {
            this.mapping = mapping;
            this.filter = filter;
        }

        @Override
        public boolean filter(Request request, Response response) {
            return filter.filter(request, response);
        }

        @Override
        public String toString() {
            return "filter: " + mapping;
        }
    }

    protected com.wizzardo.tools.http.Request makeRequest(String path) {
        return makeRequest(path, port);
    }

    protected com.wizzardo.tools.http.Request makeRequest(String path, int port) {
        return fillRequest(HttpClient.createRequest(makeUrl(path, port)));
    }

    protected com.wizzardo.tools.http.Request makeRequest(String path, HttpSession session) {
        return fillRequest(session.createRequest(makeUrl(path)));
    }

    protected String makeUrl(String path) {
        return makeUrl(path, port);
    }

    protected String makeUrl(String path, int port) {
        return "http://localhost:" + port + path;
    }

    protected com.wizzardo.tools.http.Request fillRequest(com.wizzardo.tools.http.Request request) {
        return request.header("testMethod", name.getMethodName())
                .header("Connection", "Close");
    }

    public int getPort() {
        return port;
    }

    protected String curl(String path, String... params) {
        Runtime runtime = Runtime.getRuntime();

        String[] args = new String[params.length + 2];
        args[0] = "curl";
        System.arraycopy(params, 0, args, 1, params.length);
        args[args.length - 1] = "http://localhost:" + port + path;

        try {
            Process p = runtime.exec(args);
            return new String(IOTools.bytes(p.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected void checkException(Runnable runnable, Class<? extends Exception> exceptionClass, String message) {
        boolean b = false;
        try {
            runnable.run();
            b = true;
        } catch (Exception e) {
            Assert.assertEquals(exceptionClass, e.getClass());
            Assert.assertEquals(message, e.getMessage());
        }
        Assert.assertFalse(b);
    }

    protected void checkResponse(int status, String message, com.wizzardo.tools.http.Response response) {
        Unchecked.run(() -> {
            Assert.assertEquals(status, response.getResponseCode());
            Assert.assertEquals(message, response.asString());
        });
    }
}
