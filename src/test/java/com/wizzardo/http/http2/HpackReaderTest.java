package com.wizzardo.http.http2;

import com.wizzardo.http.http2.hpack.HpackReader;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by wizzardo on 10.08.15.
 */
public class HpackReaderTest {

    @Test
    public void test_int_encoding() {
        byte[] bytes;

        bytes = new byte[1];
        HpackReader.encode(10, bytes, 3);

        Assert.assertEquals(0b00001010, bytes[0]);
    }
}
