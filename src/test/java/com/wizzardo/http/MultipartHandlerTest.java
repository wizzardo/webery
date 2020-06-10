package com.wizzardo.http;

import com.wizzardo.http.request.BlockReader;
import com.wizzardo.http.request.MultiPartEntry;
import com.wizzardo.tools.security.MD5;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by wizzardo on 08.01.16.
 */
public class MultipartHandlerTest extends ServerTest {
    @Override
    public void setUp() throws NoSuchMethodException, ClassNotFoundException, NoSuchFieldException {
        workers = 0;
        super.setUp();
    }

    @Test
    public void testPostParams() throws IOException {
        byte[] data = new byte[10 * 1024 * 1024];
        new Random().nextBytes(data);
        final String md5 = MD5.create().update(data).asString();

        handler = new MultipartHandler((request, response) -> {
            Assert.assertEquals(true, request.isMultipart());
            byte[] bytes = request.entry("data").asBytes();
            return response.setBody(MD5.create().update(bytes).asString());
        });

        Assert.assertEquals(md5, makeRequest("/")
                .addByteArray("data", data, "just some data")
                .post().asString());


        handler = new MultipartHandler((request, response) -> {
            Assert.assertEquals(null, request.data());
            Assert.assertEquals(true, request.isMultipart());

            MultiPartEntry entry = request.entry("data");
            String value = entry.asString();
            Assert.assertEquals("some data", value);
            Assert.assertEquals("just some data", entry.fileName());

            MultiPartEntry empty = request.entry("empty");
            Assert.assertEquals(0, empty.asBytes().length);
            Assert.assertEquals("", empty.fileName());

            Assert.assertEquals("bar", request.param("foo"));
            Assert.assertEquals("barbar", request.param("foofoo"));

            return response.setBody("ok");
        });

        Assert.assertEquals("ok", makeRequest("/")
                .param("foo", "bar")
                .addByteArray("data", "some data".getBytes(), "just some data")
                .addByteArray("empty", new byte[0], "")
                .param("foofoo", "barbar")
                .post().asString());
    }

    @Test
    public void testEmptyValues() throws IOException {
        handler = new MultipartHandler((request, response) -> {
            Assert.assertEquals(null, request.data());
            Assert.assertEquals(true, request.isMultipart());

            MultiPartEntry entry = request.entry("data");
            String value = entry.asString();
            Assert.assertEquals("", value);
            Assert.assertEquals("", entry.fileName());

            MultiPartEntry empty = request.entry("empty");
            Assert.assertEquals(0, empty.asBytes().length);
            Assert.assertEquals("", empty.fileName());

            Assert.assertEquals("", request.param("foo"));

            return response.setBody("ok");
        });

        Assert.assertEquals("ok", makeRequest("/")
                .param("foo", "")
                .addByteArray("data", "".getBytes(), "")
                .addByteArray("empty", new byte[0], "")
                .param("foofoo", "")
                .post().asString());
    }

    @Test
    public void manual_test_1() throws IOException {
        String request = "------WebKitFormBoundaryrmfuIZ7BtpVXhNbQ\r\n" +
                "Content-Disposition: form-data; name=\"url\"\r\n" +
                "\r\n" +
                "\r\n" +
                "------WebKitFormBoundaryrmfuIZ7BtpVXhNbQ\r\n" +
                "Content-Disposition: form-data; name=\"fileName\"\r\n" +
                "\r\n" +
                "\r\n" +
                "------WebKitFormBoundaryrmfuIZ7BtpVXhNbQ\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"\"\r\n" +
                "Content-Type: application/octet-stream\r\n" +
                "\r\n" +
                "\r\n" +
                "------WebKitFormBoundaryrmfuIZ7BtpVXhNbQ\r\n" +
                "Content-Disposition: form-data; name=\"autostart\"\r\n" +
                "\r\n" +
                "on\r\n" +
                "------WebKitFormBoundaryrmfuIZ7BtpVXhNbQ--\r\n";

        String boundary = "------WebKitFormBoundaryrmfuIZ7BtpVXhNbQ";
        byte[] data = request.getBytes();

        AtomicInteger counter = new AtomicInteger();
        BlockReader br = new BlockReader(boundary.getBytes(), new MultipartHandler.MultipartConsumer(entry -> {
            switch (counter.get()) {
                case 0: {
                    Assert.assertEquals("url", entry.name());
                    Assert.assertEquals("", entry.asString());
                    break;
                }
                case 1: {
                    Assert.assertEquals("fileName", entry.name());
                    Assert.assertEquals("", entry.asString());
                    break;
                }
                case 2: {
                    Assert.assertEquals("file", entry.name());
                    Assert.assertEquals("", entry.fileName());
                    Assert.assertEquals("", entry.asString());
                    break;
                }
                case 3: {
                    Assert.assertEquals("autostart", entry.name());
                    Assert.assertEquals("on", entry.asString());
                    break;
                }
                default:
                    Assert.assertTrue(false);
            }
            counter.incrementAndGet();
        }));
        br.process(data);

        Assert.assertEquals(4, counter.get());
    }

    @Test
    public void manual_test_2() throws IOException {
        String request = "------WebKitFormBoundaryrmfuIZ7BtpVXhNbQ\r\n" +
                "Content-Disposition: form-data; name=\"url\"\r\n" +
                "\r\n" +
                "\r\n" +
                "------WebKitFormBoundaryrmfuIZ7BtpVXhNbQ\r\n" +
                "Content-Disposition: form-data; name=\"fileName\"\r\n" +
                "\r\n" +
                "\r\n" +
                "------WebKitFormBoundaryrmfuIZ7BtpVXhNbQ\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"\"\r\n" +
                "Content-Type: application/octet-stream\r\n" +
                "\r\n" +
                "\r\n" +
                "------WebKitFormBoundaryrmfuIZ7BtpVXhNbQ\r\n" +
                "Content-Disposition: form-data; name=\"autostart\"\r\n" +
                "\r\n" +
                "on\r\n" +
                "------WebKitFormBoundaryrmfuIZ7BtpVXhNbQ--\r\n";

        String boundary = "------WebKitFormBoundaryrmfuIZ7BtpVXhNbQ";
        byte[] data = request.getBytes();

        AtomicInteger counter = new AtomicInteger();
        BlockReader br = new BlockReader(boundary.getBytes(), new MultipartHandler.MultipartConsumer(entry -> {
            switch (counter.get()) {
                case 0: {
                    Assert.assertEquals("url", entry.name());
                    Assert.assertEquals("", entry.asString());
                    break;
                }
                case 1: {
                    Assert.assertEquals("fileName", entry.name());
                    Assert.assertEquals("", entry.asString());
                    break;
                }
                case 2: {
                    Assert.assertEquals("file", entry.name());
                    Assert.assertEquals("", entry.fileName());
                    Assert.assertEquals("", entry.asString());
                    break;
                }
                case 3: {
                    Assert.assertEquals("autostart", entry.name());
                    Assert.assertEquals("on", entry.asString());
                    break;
                }
                default:
                    Assert.assertTrue(false);
            }
            counter.incrementAndGet();
        }));
        for (int i = 0; i < data.length; i++) {
            br.process(data, i, 1);
        }

        Assert.assertEquals(4, counter.get());
    }

    @Test
    public void test_multipart_1() throws IOException {
        handler = new MultipartHandler((request, response) -> {
            String foo = request.entry("foo").asString();
            String bar = request.entry("bar").asString();
            String fileMD5 = MD5.create().update(request.entry("file").asBytes()).asString();
            return response.setBody(foo + "-" + bar + "-" + fileMD5);
        });

        byte[] data = new byte[2 * 1024 * 1024];
        new Random().nextBytes(data);

        for (int i = 0; i < 10; i++) {
            com.wizzardo.tools.http.Response response = makeRequest("/")
                    .param("foo", "foo")
                    .param("bar", "bar")
                    .addByteArray("file", data, "file")
                    .execute();

            Assert.assertEquals(200, response.getResponseCode());
            Assert.assertEquals("foo-bar-" + MD5.create().update(data).asString(), response.asString());
        }
    }

    @Override
    protected com.wizzardo.tools.http.Request fillRequest(com.wizzardo.tools.http.Request request) {
        return request.header("testMethod", name.getMethodName());
    }

    @Test
    public void test_multipart_2() throws IOException, InterruptedException, NoSuchMethodException, NoSuchFieldException, ClassNotFoundException {
        tearDown();
        workers = 4;
        super.setUp();

        test_multipart_1();
    }
}