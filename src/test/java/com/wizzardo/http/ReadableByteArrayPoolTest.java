package com.wizzardo.http;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by wizzardo on 11/11/16.
 */
public class ReadableByteArrayPoolTest {
    @Test
    public void test() throws IOException {
        ReadableByteArrayPool.PooledReadableByteArray byteArray = ReadableByteArrayPool.get();

        Assert.assertEquals(10240, byteArray.bytes().length);
        Assert.assertEquals(10240, byteArray.length());

        byteArray.length(10);
        Assert.assertEquals(10, byteArray.length());

        byte[] bytes = byteArray.bytes();
        byteArray.close();

        byteArray = new ReadableByteArrayPool().get();
        Assert.assertSame(bytes, byteArray.bytes());
        Assert.assertEquals(10240, byteArray.length());
        byteArray.close();
    }
}
