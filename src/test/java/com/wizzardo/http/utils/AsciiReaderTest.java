package com.wizzardo.http.utils;

import org.junit.Assert;
import org.junit.Test;

public class AsciiReaderTest {
    @Test
    public void test_read() {
        byte[] data;
        data = "foo".getBytes();

        Assert.assertEquals("foo", new AsciiReader().read(data));
        Assert.assertEquals("foo".hashCode(), AsciiReader.read(data).hashCode());

        Assert.assertEquals("", AsciiReader.read(data, 0, -1));
    }

    @Test
    public void test_read_with_hash() {
        byte[] data;
        data = "foo".getBytes();

        Assert.assertEquals("foo", AsciiReader.read(data, 0, 3, -1));
        Assert.assertEquals(-1, AsciiReader.read(data, 0, 3, -1).hashCode());

        Assert.assertEquals("", AsciiReader.read(data, 0, -1, -1));
    }

    @Test
    public void test_read_with_buffer() {
        byte[] data;
        data = "foo".getBytes();

        Assert.assertEquals("fofoo", AsciiReader.read(data, 2, data, 0, 3));
        Assert.assertEquals("fofoo".hashCode(), AsciiReader.read(data, 2, data, 0, 3).hashCode());
    }

    @Test
    public void test_write() {
        Assert.assertArrayEquals("foo".getBytes(), AsciiReader.write("foo"));
    }
}