package com.wizzardo.http.request;

import com.wizzardo.http.response.RangeResponse;
import com.wizzardo.tools.http.ConnectionMethod;
import com.wizzardo.tools.io.FileTools;
import com.wizzardo.tools.io.IOTools;
import com.wizzardo.tools.security.MD5;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
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
        handler = (request, response) -> response.setBody("ok");

        Assert.assertEquals("ok", makeRequest("").get().asString());
    }

    @Test
    public void testHeaders() throws IOException {
        handler = (request, response) -> {
            Assert.assertEquals("value", request.header("key"));

            Assert.assertEquals("1", request.header("array"));
            List<String> headers = request.headers("array");
            Assert.assertEquals(2, headers.size());
            Assert.assertEquals("1", headers.get(0));
            Assert.assertEquals("2", headers.get(1));
            return response.setBody("ok");
        };

        Assert.assertEquals("ok", curl("", "-H", "array: 1", "-H", "array: 2", "-H", "key: value"));
    }

    @Test
    public void testPath() throws IOException {
        handler = (request, response) -> {
            Assert.assertEquals("/", request.path());
            return response.setBody("ok");
        };

        Assert.assertEquals("ok", makeRequest("").get().asString());

        handler = (request, response) -> {
            Assert.assertEquals("/path", request.path());
            return response.setBody("ok");
        };

        Assert.assertEquals("ok", makeRequest("/path").get().asString());

        handler = (request, response) -> {
            Assert.assertEquals("/path", request.path());
            return response.setBody("ok");
        };

        Assert.assertEquals("ok", makeRequest("/path").addParameter("key", "value").get().asString());
    }


    @Test
    public void testParams() throws IOException {
        handler = (request, response) -> {
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
            return response.setBody("ok");
        };

        Assert.assertEquals("ok", makeRequest("/path")
                .addParameter("key", "value")
                .addParameter("array", "1")
                .addParameter("array", "2")
                .addParameter("empty", "")
                .addParameter("", "empty")
                .addParameter("", "")
                .get().asString());

        handler = (request, response) -> {
            Assert.assertEquals("", request.param("key"));
            return response.setBody("ok");
        };

        Assert.assertEquals("ok", makeRequest("/path?key").get().asString());
    }

    @Test
    public void testMethod() throws IOException {
        handler = (request, response) -> response.setBody(request.method().name());

        Assert.assertEquals("GET", makeRequest("/").get().asString());
        Assert.assertEquals("POST", makeRequest("/").post().asString());
        Assert.assertEquals("PUT", makeRequest("/").method(ConnectionMethod.PUT).execute().asString());
        Assert.assertEquals("DELETE", makeRequest("/").method(ConnectionMethod.DELETE).execute().asString());
    }

    @Test
    public void testOutputStream() throws IOException {
        handler = (request, response) -> {
            response.setHeader(Header.KEY_CONTENT_LENGTH, 2);
            try {
                response.getOutputStream(request.getConnection()).write("ok".getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
            request.getConnection().close();
            return response;
        };

        Assert.assertEquals("ok", makeRequest("/").get().asString());
        Assert.assertEquals("ok", makeRequest("/").get().asString());
        Assert.assertEquals("ok", makeRequest("/").get().asString());

        byte[] big = new byte[10 * 1024 * 1024];
        new Random().nextBytes(big);
        String md5 = MD5.getMD5AsString(big);

        handler = (request, response) -> {
            response.setHeader(Header.KEY_CONTENT_LENGTH, big.length);
            try {
                response.getOutputStream(request.getConnection()).write(big);
            } catch (IOException e) {
                e.printStackTrace();
            }
            request.getConnection().close();
            return response;
        };

        Assert.assertEquals(md5, MD5.getMD5AsString(makeRequest("/").get().asStream()));


        handler = (request, response) -> {
            response.setHeader(Header.KEY_CONTENT_LENGTH, big.length);
            try {
                OutputStream out = response.getOutputStream(request.getConnection());
                for (int i = 0; i < big.length; i++) {
                    out.write(big[i] & 0xff);
                }
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
                request.getConnection().close();
            }
            return response;
        };

        Assert.assertEquals(md5, MD5.getMD5AsString(makeRequest("/").get().asStream()));
    }

    @Test
    public void testPostParams() throws IOException {
        handler = (request, response) -> {
            Assert.assertEquals("value", request.param("key"));
            return response.setBody("ok");
        };

        Assert.assertEquals("ok", makeRequest("/").addParameter("key", "value").post().asString());


        handler = (request, response) -> {
            Assert.assertEquals("some data", new String(request.data()));
            return response.setBody("ok");
        };

        Assert.assertEquals("ok", makeRequest("/")
                .data("some data".getBytes(), "just some data")
                .post().asString());


        byte[] data = new byte[10 * 1024 * 1024];
        new Random().nextBytes(data);
        final String md5 = MD5.getMD5AsString(data);

        handler = (request, response) -> {
            Assert.assertEquals(null, request.data());
            Assert.assertEquals(false, request.isMultipart());
            try {
                return response.setBody(MD5.getMD5AsString(request.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
                return response.setBody(e.getMessage());
            }
        };

        Assert.assertEquals(md5, makeRequest("/")
                .data(data, "just some data")
                .post().asString());

        handler = (request, response) -> {
            Assert.assertEquals(null, request.data());
            Assert.assertEquals(false, request.isMultipart());
            try {
                int l = (int) request.headerLong(Header.KEY_CONTENT_LENGTH);
                ByteArrayOutputStream out = new ByteArrayOutputStream(l);
                InputStream in = request.getInputStream();
                while (l-- > 0)
                    out.write(in.read());

                return response.setBody(MD5.getMD5AsString(out.toByteArray()));
            } catch (IOException e) {
                e.printStackTrace();
                return response.setBody(e.getMessage());
            }
        };

        Assert.assertEquals(md5, makeRequest("/")
                .data(data, "just some data")
                .post().asString());


        final AtomicInteger counter = new AtomicInteger();
        handler = (request, response) -> {
            Assert.assertEquals(null, request.data());
            Assert.assertEquals(true, request.isMultipart());
            Assert.assertEquals(0, counter.getAndIncrement());
            try {
                String value = new String(IOTools.bytes(request.getInputStream()));
                Assert.assertEquals("------WebKitFormBoundaryZzaC4MkAfrAMfJCJ\r\n" +
                        "Content-Disposition: form-data; name=\"data\"; filename=\"just some data\"\r\n" +
                        "Content-Type: application/octet-stream\r\n" +
                        "\r\n" +
                        "some data\r\n" +
                        "------WebKitFormBoundaryZzaC4MkAfrAMfJCJ--", value);
            } catch (IOException e) {
                e.printStackTrace();
                return response.setBody(e.getMessage());
            }

            return response.setBody("ok");
        };

        Assert.assertEquals("ok", makeRequest("/")
                .addByteArray("data", "some data".getBytes(), "just some data")
                .post().asString());


        handler = (request, response) -> {
            Assert.assertEquals(null, request.data());
            Assert.assertEquals(true, request.isMultipart());

            Request.MultiPartEntry entry = request.getMultiPartEntry("data");
            String value = FileTools.text(entry.getFile());
            Assert.assertEquals("some data", value);
            Assert.assertEquals("just some data", entry.getFilename());

            Assert.assertEquals("bar", request.param("foo"));
            Assert.assertEquals("barbar", request.param("foofoo"));

            return response.setBody("ok");
        };

        Assert.assertEquals("ok", makeRequest("/")
                .param("foo", "bar")
                .addByteArray("data", "some data".getBytes(), "just some data")
                .param("foofoo", "barbar")
                .post().asString());
    }

    @Test
    public void testRange() throws IOException {
        byte[] data = new byte[1000];
        new Random().nextBytes(data);
        final File file = File.createTempFile("test_range", null);
        file.deleteOnExit();
        FileTools.bytes(file, data);

        handler = (request, response) -> new RangeResponse(request, file);

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
