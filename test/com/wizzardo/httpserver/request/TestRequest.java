package com.wizzardo.httpserver.request;

import com.wizzardo.httpserver.response.Response;
import com.wizzardo.tools.http.ConnectionMethod;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

/**
 * @author: wizzardo
 * Date: 7/26/14
 */
public class TestRequest extends ServerTest {

    @Test
    public void testOk() throws IOException {
        handler = new Handler() {
            @Override
            protected Response handleRequest(Request request) {
                return new Response().setBody("ok");
            }
        };

        Assert.assertEquals("ok", makeRequest("").get().asString());
    }

    @Test
    public void testHeaders() throws IOException {
        handler = new Handler() {
            @Override
            protected Response handleRequest(Request request) {
                Assert.assertEquals("value", request.header("key"));

                Assert.assertEquals("1", request.header("array"));
                List<String> headers = request.headers("array");
                Assert.assertEquals(2, headers.size());
                Assert.assertEquals("1", headers.get(0));
                Assert.assertEquals("2", headers.get(1));
                return new Response().setBody("ok");
            }
        };

        Assert.assertEquals("ok", curl("", "-H", "array: 1", "-H", "array: 2", "-H", "key: value"));
    }

    @Test
    public void testPath() throws IOException {
        handler = new Handler() {
            @Override
            protected Response handleRequest(Request request) {
                Assert.assertEquals("/", request.path());
                return new Response().setBody("ok");
            }
        };

        Assert.assertEquals("ok", makeRequest("").get().asString());

        handler = new Handler() {
            @Override
            protected Response handleRequest(Request request) {
                Assert.assertEquals("/path", request.path());
                return new Response().setBody("ok");
            }
        };

        Assert.assertEquals("ok", makeRequest("/path").get().asString());

        handler = new Handler() {
            @Override
            protected Response handleRequest(Request request) {
                Assert.assertEquals("/path", request.path());
                return new Response().setBody("ok");
            }
        };

        Assert.assertEquals("ok", makeRequest("/path").addParameter("key", "value").get().asString());
    }


    @Test
    public void testParams() throws IOException {
        handler = new Handler() {
            @Override
            protected Response handleRequest(Request request) {
                Assert.assertEquals("key=value&array=1&array=2&empty=&=empty&=", request.getQueryString());
                Assert.assertEquals("value", request.param("key"));

                Assert.assertEquals("1", request.param("array"));
                List<String> params = request.params("array");
                Assert.assertEquals(2, params.size());
                Assert.assertEquals("1", params.get(0));
                Assert.assertEquals("2", params.get(1));

                Assert.assertEquals("", request.param("empty"));
                Assert.assertEquals("empty", request.param(""));
                Assert.assertEquals("", request.params("").get(1));
                return new Response().setBody("ok");
            }
        };

        Assert.assertEquals("ok", makeRequest("/path")
                .addParameter("key", "value")
                .addParameter("array", "1")
                .addParameter("array", "2")
                .addParameter("empty", "")
                .addParameter("", "empty")
                .addParameter("", "")
                .get().asString());

        handler = new Handler() {
            @Override
            protected Response handleRequest(Request request) {
                Assert.assertEquals("", request.param("key"));
                return new Response().setBody("ok");
            }
        };

        Assert.assertEquals("ok", makeRequest("/path?key").get().asString());
    }

    @Test
    public void testMethod() throws IOException {
        handler = new Handler() {
            @Override
            protected Response handleRequest(Request request) {
                return new Response().setBody(request.method().name());
            }
        };

        Assert.assertEquals("GET", makeRequest("/").get().asString());
        Assert.assertEquals("POST", makeRequest("/").post().asString());
        Assert.assertEquals("PUT", makeRequest("/").method(ConnectionMethod.PUT).connect().asString());
        Assert.assertEquals("DELETE", makeRequest("/").method(ConnectionMethod.DELETE).connect().asString());
    }

    @Test
    public void testPostParams() throws IOException {
        handler = new Handler() {
            @Override
            protected Response handleRequest(Request request) {
                Assert.assertEquals("value", request.param("key"));
                return new Response().setBody("ok");
            }
        };

        Assert.assertEquals("ok", makeRequest("/").addParameter("key", "value").post().asString());
    }
}
