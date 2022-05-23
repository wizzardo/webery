package com.wizzardo.http;

import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;
import com.wizzardo.tools.evaluation.CallStack;
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
import java.io.OutputStream;
import java.net.Socket;

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
                debug = false;
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

    protected String rawRequest(String path) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("GET ").append(path).append(" HTTP/1.1").append("\r\n");
        sb.append("Host: localhost:").append(port).append("\r\n");
        sb.append("testMethod: ").append(name.getMethodName()).append("\r\n");
        sb.append("Connection: Close\r\n");
        sb.append("\r\n");
        Socket socket = new Socket("localhost", port);
        OutputStream out = socket.getOutputStream();
        out.write(sb.toString().getBytes());
        out.flush();
        byte[] bytes = IOTools.bytes(socket.getInputStream());
        socket.close();
        return new String(bytes);
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

    public static void checkException(Runnable runnable, Class<? extends Exception> exceptionClass, String message) {
        boolean b = false;
        try {
            runnable.run();
            b = true;
        } catch (Exception e) {
            if (e.getCause() != null) {
                Throwable cause = e.getCause();
                Assert.assertEquals(exceptionClass, cause.getClass());
                Assert.assertEquals(message, cause.getMessage());
                return;
            }
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

    public static int indexOf(byte[] src, byte[] search) {
        int result = -1;
        outer:
        for (int i = 0; i < src.length - search.length; i++) {
            for (int j = 0; j < search.length; j++) {
                if (src[i + j] != search[j])
                    continue outer;
            }
            return i;
        }
        return result;
    }
}
