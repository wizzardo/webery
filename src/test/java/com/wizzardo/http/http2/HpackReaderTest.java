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

    @Test
    public void test_int_decoding_1() {
        byte[] bytes;

        bytes = new byte[1];
        bytes[0] = (byte) 0b00001010;

        HpackReader.IntDecodingResult result = HpackReader.decode(bytes, 3);

        Assert.assertEquals(8, result.offset);
        Assert.assertEquals(10, result.value);
    }

    @Test
    public void test_int_decoding_2() {
        byte[] bytes;

        bytes = new byte[3];
        bytes[0] = (byte) 0b00011111;
        bytes[1] = (byte) 0b10011010;
        bytes[2] = (byte) 0b00001010;

        HpackReader.IntDecodingResult result = HpackReader.decode(bytes, 3);

        Assert.assertEquals(24, result.offset);
        Assert.assertEquals(1337, result.value);
    }

    @Test
    public void test_int_decoding_3() {
        byte[] bytes;

        bytes = new byte[1];
        bytes[0] = (byte) 0b00101010;

        HpackReader.IntDecodingResult result = HpackReader.decode(bytes, 0);

        Assert.assertEquals(8, result.offset);
        Assert.assertEquals(42, result.value);
    }


    @Test
    public void test_string_encoding_1() {
        String s = "/sample/path";
        byte[] bytes = new byte[14];

        Assert.assertEquals(8 * bytes.length, HpackReader.encode(s, false, bytes, 8));
        Assert.assertArrayEquals(new byte[]{0x0c, 0x2f, 0x73, 0x61, 0x6d, 0x70, 0x6c, 0x65, 0x2f, 0x70, 0x61, 0x74, 0x68}, bytes);
    }
}
