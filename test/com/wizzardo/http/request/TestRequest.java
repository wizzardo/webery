package com.wizzardo.http.request;

import com.wizzardo.http.response.RangeResponse;
import com.wizzardo.http.response.Response;
import com.wizzardo.tools.http.ConnectionMethod;
import com.wizzardo.tools.io.FileTools;
import com.wizzardo.tools.io.IOTools;
import com.wizzardo.tools.security.MD5;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

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
        Assert.assertEquals("PUT", makeRequest("/").method(ConnectionMethod.PUT).execute().asString());
        Assert.assertEquals("DELETE", makeRequest("/").method(ConnectionMethod.DELETE).execute().asString());
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


        handler = new Handler() {
            @Override
            protected Response handleRequest(Request request) {
                Assert.assertEquals("some data", new String(request.data()));
                return new Response().setBody("ok");
            }
        };

        Assert.assertEquals("ok", makeRequest("/")
                .data("some data".getBytes(), "just some data")
                .post().asString());


        byte[] data = new byte[3 * 1024 * 1024];
        new Random().nextBytes(data);
        final String md5 = MD5.getMD5AsString(data);

        handler = new Handler() {
            @Override
            protected Response handleRequest(Request request) {
                Assert.assertEquals(null, request.data());
                Assert.assertEquals(false, request.isMultipart());
                try {
                    return new Response().setBody(MD5.getMD5AsString(request.connection().getInputStream()));
                } catch (IOException e) {
                    e.printStackTrace();
                    return new Response().setBody(e.getMessage());
                }
            }
        };

        Assert.assertEquals(md5, makeRequest("/")
                .data(data, "just some data")
                .post().asString());


        final AtomicInteger counter = new AtomicInteger();
        handler = new Handler() {
            @Override
            protected Response handleRequest(Request request) {
                Assert.assertEquals(null, request.data());
                Assert.assertEquals(true, request.isMultipart());
                Assert.assertEquals(0, counter.getAndIncrement());
                try {
                    String data = new String(IOTools.bytes(request.connection().getInputStream()));
//                    System.out.println(data);
                    Assert.assertEquals("------WebKitFormBoundaryZzaC4MkAfrAMfJCJ\r\n" +
                            "Content-Disposition: form-data; name=\"data\"; filename=\"just some data\"\r\n" +
                            "Content-Type: application/octet-stream\r\n" +
                            "\r\n" +
                            "some data\r\n" +
                            "------WebKitFormBoundaryZzaC4MkAfrAMfJCJ--", data);
                } catch (IOException e) {
                    e.printStackTrace();
                    return new Response().setBody(e.getMessage());
                }

                return new Response().setBody("ok");
            }
        };

        Assert.assertEquals("ok", makeRequest("/")
                .addByteArray("data", "some data".getBytes(), "just some data")
                .post().asString());


        handler = new Handler() {
            @Override
            public Response handleRequest(Request request) {
                Assert.assertEquals(null, request.data());
                Assert.assertEquals(true, request.isMultipart());

                Request.MultiPartEntry entry = request.getMultiPartEntry("data");
                String data = FileTools.text(entry.getFile());
                Assert.assertEquals("some data", data);
                Assert.assertEquals("just some data", entry.getFilename());

                Assert.assertEquals("bar", request.param("foo"));
                Assert.assertEquals("barbar", request.param("foofoo"));

                return new Response().setBody("ok");
            }
        };

        Assert.assertEquals("ok", makeRequest("/")
                .param("foo","bar")
                .addByteArray("data", "some data".getBytes(), "just some data")
                .param("foofoo","barbar")
                .post().asString());
    }

    @Test
    public void testRange() throws IOException {
        byte[] data = new byte[1000];
        new Random().nextBytes(data);
        final File file = File.createTempFile("test_range", null);
        file.deleteOnExit();
        FileTools.bytes(file, data);

        handler = new Handler() {
            @Override
            protected Response handleRequest(Request request) {
                return new RangeResponse(request, file);
            }
        };

        byte[] test;
        test = new byte[100];
        System.arraycopy(data, 0, test, 0, test.length);
        Assert.assertArrayEquals(test, makeRequest("/")
                .header("Range", "bytes=0-99")
                .get().asBytes());

        System.arraycopy(data, 100, test, 0, test.length);
        Assert.assertArrayEquals(test, makeRequest("/")
                .header("Range", "bytes=100-199")
                .get().asBytes());

        test = new byte[900];
        System.arraycopy(data, 100, test, 0, test.length);
        Assert.assertArrayEquals(test, makeRequest("/")
                .header("Range", "bytes=100-")
                .get().asBytes());

        test = new byte[200];
        System.arraycopy(data, 800, test, 0, test.length);
        Assert.assertArrayEquals(test, makeRequest("/")
                .header("Range", "bytes=-200")
                .get().asBytes());

        test = new byte[200];
        System.arraycopy(data, 800, test, 0, test.length);
        Assert.assertArrayEquals(test, makeRequest("/")
                .header("Range", "bytes=800-2000")
                .get().asBytes());

        Assert.assertEquals(416, makeRequest("/")
                .header("Range", "bytes=400-200")
                .get().getResponseCode());

        Assert.assertEquals(416, makeRequest("/")
                .header("Range", "bytes=1000-1001")
                .get().getResponseCode());
    }
}
