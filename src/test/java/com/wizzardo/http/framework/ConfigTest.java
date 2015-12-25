package com.wizzardo.http.framework;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

public class ConfigTest extends WebApplicationTest {

    @Test
    public void test() throws IOException {
        Assert.assertEquals("value", server.getConfig().get("key"));
        Assert.assertEquals("c", ((Map) server.getConfig().get("a")).get("b"));
        Assert.assertEquals(1, ((Map) server.getConfig().get("a")).get("c"));
    }
}
