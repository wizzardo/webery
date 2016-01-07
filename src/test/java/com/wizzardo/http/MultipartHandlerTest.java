package com.wizzardo.http;

import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.MultiPartEntry;
import com.wizzardo.tools.io.IOTools;
import com.wizzardo.tools.security.MD5;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

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
}