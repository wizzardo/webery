package com.wizzardo.http;

import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;
import com.wizzardo.tools.http.HttpClient;
import com.wizzardo.tools.io.IOTools;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import java.io.IOException;

/**
 * @author: moxa
 * Date: 5/7/13
 */
public class ServerTest {

    protected HttpServer server;
    protected int workers = 4;
    protected int port = 9999;
    protected volatile Handler handler;

    @Rule
    public TestName name = new TestName();

    @Before
    public void setUp() throws NoSuchMethodException, ClassNotFoundException, NoSuchFieldException {
        System.out.println("setUp " + name.getMethodName());
        server = new HttpServer(null, port, workers) {
            @Override
            protected Response handle(Request request, Response response) throws IOException {
                response.setHeader(Header.KEY_CONNECTION, Header.VALUE_CONNECTION_CLOSE);
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
        };
        server.setIoThreadsCount(1);
        server.start();
    }

    @After
    public void tearDown() throws InterruptedException {
        System.out.println("tearDown " + name.getMethodName());
        server.stopEpoll();
        handler = null;
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
        return HttpClient.createRequest("http://localhost:" + port + path)
                .header("testMethod", name.getMethodName())
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
}
