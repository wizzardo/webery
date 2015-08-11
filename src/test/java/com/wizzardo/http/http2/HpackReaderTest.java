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
}
