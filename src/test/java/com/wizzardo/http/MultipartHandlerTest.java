package com.wizzardo.http;

import com.wizzardo.http.request.BlockReader;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.MultiPartEntry;
import com.wizzardo.tools.security.MD5;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
            return response.setBody(MD5.create().update(bytes).asString()).appendHeader(Header.KV_CONNECTION_CLOSE);
        });

        String responseString = makeRequest("/")
                .addByteArray("data", data, "just some data")
                .post().asString();

        Assert.assertEquals(md5, responseString);


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
                "url-value\r\n" +
                "------WebKitFormBoundaryrmfuIZ7BtpVXhNbQ\r\n" +
                "Content-Disposition: form-data; name=\"fileName\"\r\n" +
                "\r\n" +
                "fileName-value\r\n" +
                "------WebKitFormBoundaryrmfuIZ7BtpVXhNbQ\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"\"\r\n" +
                "Content-Type: application/octet-stream\r\n" +
                "\r\n" +
                "data\r\n" +
                "------WebKitFormBoundaryrmfuIZ7BtpVXhNbQ\r\n" +
                "Content-Disposition: form-data; name=\"autostart\"\r\n" +
                "\r\n" +
                "on\r\n" +
                "------WebKitFormBoundaryrmfuIZ7BtpVXhNbQ--\r\n";

        String boundary = "------WebKitFormBoundaryrmfuIZ7BtpVXhNbQ";
        byte[] data = request.getBytes();
        byte[] buffer = new byte[data.length];
        for (int step = 1; step < data.length; step++) {
            String errorMessage = "failed at step: " + step;

            AtomicInteger counter = new AtomicInteger();
            BlockReader br = new BlockReader(boundary.getBytes(), new MultipartHandler.MultipartConsumer(entry -> {
                switch (counter.get()) {
                    case 0: {
                        Assert.assertEquals(errorMessage, "url", entry.name());
                        Assert.assertEquals(errorMessage, "url-value", entry.asString());
                        break;
                    }
                    case 1: {
                        Assert.assertEquals(errorMessage, "fileName", entry.name());
                        Assert.assertEquals(errorMessage, "fileName-value", entry.asString());
                        break;
                    }
                    case 2: {
                        Assert.assertEquals(errorMessage, "file", entry.name());
                        Assert.assertEquals(errorMessage, "", entry.fileName());
                        Assert.assertEquals(errorMessage, "data", entry.asString());
                        break;
                    }
                    case 3: {
                        Assert.assertEquals(errorMessage, "autostart", entry.name());
                        Assert.assertEquals(errorMessage, "on", entry.asString());
                        break;
                    }
                    default:
                        Assert.assertTrue(false);
                }
                counter.incrementAndGet();
            }));
            for (int i = 0; i < data.length; i += step) {
//                br.process(data, i, Math.min(data.length - i, step));
                int l = Math.min(data.length - i, step);
                System.arraycopy(data, i, buffer, 0, l);
                br.process(buffer, 0, l);
            }

            Assert.assertEquals(4, counter.get());
        }

    }

    @Test
    public void manual_test_4() throws IOException, ExecutionException, InterruptedException {
        String request = "------WebKitFormBoundaryrmfuIZ7BtpVXhNbQ\r\n" +
                "Content-Disposition: form-data; name=\"url\"\r\n" +
                "\r\n" +
                "url-value\r\n" +
                "------WebKitFormBoundaryrmfuIZ7BtpVXhNbQ\r\n" +
                "Content-Disposition: form-data; name=\"fileName\"\r\n" +
                "\r\n" +
                "fileName-value\r\n" +
                "------WebKitFormBoundaryrmfuIZ7BtpVXhNbQ\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"\"\r\n" +
                "Content-Type: application/octet-stream\r\n" +
                "\r\n" +
                "data\r\n" +
                "------WebKitFormBoundaryrmfuIZ7BtpVXhNbQ\r\n" +
                "Content-Disposition: form-data; name=\"autostart\"\r\n" +
                "\r\n" +
                "on\r\n" +
                "------WebKitFormBoundaryrmfuIZ7BtpVXhNbQ--\r\n";

        String boundary = "------WebKitFormBoundaryrmfuIZ7BtpVXhNbQ";
        byte[] data = request.getBytes();
        byte[] buffer = new byte[data.length];
        ExecutorService executorService = Executors.newFixedThreadPool(1);

        for (int separator = 1; separator < data.length; separator++) {
            String errorMessage = "failed at step: " + separator;

            AtomicInteger counter = new AtomicInteger();
            BlockReader br = new BlockReader(boundary.getBytes(), new MultipartHandler.MultipartConsumer(entry -> {
                switch (counter.get()) {
                    case 0: {
                        Assert.assertEquals(errorMessage, "url", entry.name());
                        Assert.assertEquals(errorMessage, "url-value", entry.asString());
                        break;
                    }
                    case 1: {
                        Assert.assertEquals(errorMessage, "fileName", entry.name());
                        Assert.assertEquals(errorMessage, "fileName-value", entry.asString());
                        break;
                    }
                    case 2: {
                        Assert.assertEquals(errorMessage, "file", entry.name());
                        Assert.assertEquals(errorMessage, "", entry.fileName());
                        Assert.assertEquals(errorMessage, "data", entry.asString());
                        break;
                    }
                    case 3: {
                        Assert.assertEquals(errorMessage, "autostart", entry.name());
                        Assert.assertEquals(errorMessage, "on", entry.asString());
                        break;
                    }
                    default:
                        Assert.assertTrue(false);
                }
                counter.incrementAndGet();
            }));

            br.process(data, 0, separator);
            int length = data.length - separator;
            System.arraycopy(data, separator, buffer, 0, length);
            Future<?> future = executorService.submit(() -> {
                br.process(buffer, 0, length);
            });
            future.get();

//            for (int i = 0; i < data.length; i += separator) {
////                br.process(data, i, Math.min(data.length - i, step));
//                int l = Math.min(data.length - i, separator);
//                System.arraycopy(data, i, buffer, 0, l);
//                br.process(buffer, 0, l);
//            }

            Assert.assertEquals(4, counter.get());
        }

    }

    @Test
    public void manual_test_3() throws IOException {
        String request = "------WebKitFormBoundaryZzaC4MkAfrAMfJCJ\r\n" +
                "Content-Disposition: form-data; name=\"foo\"\r\n" +
                "\r\n";
        String part2 =
                "bar\r\n" +
                        "------WebKitFormBoundaryZzaC4MkAfrAMfJCJ\r\n" +
                        "Content-Disposition: form-data; name=\"data\"; filename=\"just some data\"\r\n" +
                        "Content-Type: application/octet-stream\r\n\r\n" +
                        "some data\r\n" +
                        "------WebKitFormBoundaryZzaC4MkAfrAMfJCJ\r\n" +
                        "Content-Disposition: form-data; name=\"empty\"; filename=\"\"\r\n" +
                        "Content-Type: application/octet-stream\r\n\r\n" +
                        "\r\n" +
                        "------WebKitFormBoundaryZzaC4MkAfrAMfJCJ\r\n" +
                        "Content-Disposition: form-data; name=\"foofoo\"\r\n\r\n" +
                        "barbar\r\n" +
                        "------WebKitFormBoundaryZzaC4MkAfrAMfJCJ--";

        String boundary = "------WebKitFormBoundaryZzaC4MkAfrAMfJCJ";

        AtomicInteger counter = new AtomicInteger();
        BlockReader br = new BlockReader(boundary.getBytes(), new MultipartHandler.MultipartConsumer(entry -> {
            String errorMessage = "";
            switch (counter.get()) {
                case 0: {
                    Assert.assertEquals(errorMessage, "foo", entry.name());
                    Assert.assertEquals(errorMessage, "bar", entry.asString());
                    break;
                }
                case 1: {
                    Assert.assertEquals(errorMessage, "data", entry.name());
                    Assert.assertEquals(errorMessage, "some data", entry.asString());
                    Assert.assertEquals(errorMessage, "just some data", entry.fileName());
                    break;
                }
                case 2: {
                    Assert.assertEquals(errorMessage, "empty", entry.name());
                    Assert.assertEquals(errorMessage, "", entry.asString());
                    break;
                }
                case 3: {
                    Assert.assertEquals(errorMessage, "foofoo", entry.name());
                    Assert.assertEquals(errorMessage, "barbar", entry.asString());
                    break;
                }
                default:
                    Assert.assertTrue(false);
            }
            counter.incrementAndGet();
        }));
        br.process(request.getBytes());
        br.process(part2.getBytes());

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
//            System.out.println("request.foo: |" + foo + "| " + Arrays.toString(foo.getBytes(StandardCharsets.UTF_8)));
//            System.out.println("request.bar: |" + bar + "| " + Arrays.toString(bar.getBytes(StandardCharsets.UTF_8)));
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
            String expected = "foo-bar-" + MD5.create().update(data).asString();
            String actual = response.asString();
//            System.out.println("expected: |" + expected + "|" + Arrays.toString(expected.getBytes(StandardCharsets.UTF_8)));
//            System.out.println("actual:   |" + actual + "|" + Arrays.toString(actual.getBytes(StandardCharsets.UTF_8)));
            Assert.assertEquals(expected, actual);
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