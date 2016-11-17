package com.wizzardo.http.response;

import com.wizzardo.http.request.Header;
import org.junit.Assert;
import org.junit.Test;

public class ResponseTest {

    @Test
    public void test_toString() {
        Assert.assertEquals("status: 200\n", new Response().toString());

        Assert.assertEquals("status: 200\nContent-Type: application/json\r\n", new Response().appendHeader(Header.KV_CONTENT_TYPE_APPLICATION_JSON).toString());

        Assert.assertEquals("status: 200\nConnection: Close\r\n", new Response().header(Header.KEY_CONNECTION, Header.VALUE_CLOSE).toString());
    }
}