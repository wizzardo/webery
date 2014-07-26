package com.wizzardo.httpserver.request;

import com.wizzardo.httpserver.response.Response;
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
}
