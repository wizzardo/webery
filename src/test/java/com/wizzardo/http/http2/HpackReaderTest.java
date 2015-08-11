package com.wizzardo.http.http2;

import com.wizzardo.http.http2.hpack.HpackReader;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by wizzardo on 10.08.15.
 */
public class HpackReaderTest {

    @Test
    public void test_int_encoding_1() {
        byte[] bytes;

        bytes = new byte[1];

        Assert.assertEquals(8, HpackReader.encode(10, bytes, 3));
        Assert.assertEquals(0b00001010, bytes[0] & 0xff);

        bytes[0] = (byte) 255;
        Assert.assertEquals(8, HpackReader.encode(10, bytes, 3));
        Assert.assertEquals(0b11101010, bytes[0] & 0xff);
    }

    @Test
    public void test_int_encoding_2() {
        byte[] bytes;

        bytes = new byte[3];

        Assert.assertEquals(24, HpackReader.encode(1337, bytes, 3));
        Assert.assertEquals(0b00011111, bytes[0] & 0xff);
        Assert.assertEquals(0b10011010, bytes[1] & 0xff);
        Assert.assertEquals(0b00001010, bytes[2] & 0xff);
    }

    @Test
    public void test_int_encoding_3() {
        byte[] bytes;

        bytes = new byte[1];

        Assert.assertEquals(8, HpackReader.encode(42, bytes, 0));
        Assert.assertEquals(0b00101010, bytes[0] & 0xff);
    }
}
