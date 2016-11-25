package com.wizzardo.http.framework.template;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class RenderResultTest {

    @Test
    public void test_write() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new RenderResult()
                .append("foo")
                .append("bar".getBytes(StandardCharsets.UTF_8))
                .write(out);

        Assert.assertEquals("foobar", out.toString());
    }

    @Test
    public void test_compact() throws IOException {
        RenderResult result = new RenderResult()
                .append("foo")
                .append("bar".getBytes(StandardCharsets.UTF_8));

        Assert.assertEquals(2, result.renders.size());

        Assert.assertEquals("foobar", new String(result.bytes()));
        Assert.assertEquals(null, result.renders);

        result.compact();
        Assert.assertEquals("foobar", new String(result.bytes()));
        Assert.assertEquals(null, result.renders);

        result.append("qwe");
        Assert.assertEquals(null, result.bytes);
        Assert.assertEquals("foobarqwe", result.toString());
    }
}