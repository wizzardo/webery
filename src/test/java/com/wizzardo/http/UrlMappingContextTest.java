package com.wizzardo.http;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * @author: wizzardo
 * Date: 25.09.14
 */
public class UrlMappingContextTest extends ServerTest {

    public void setUp() throws NoSuchMethodException, ClassNotFoundException, NoSuchFieldException {
        context = "context";
        super.setUp();
    }

    @Test
    public void testContext() throws IOException {
        handler = new UrlHandler(context)
                .append("/action1", (request, response) -> response.setBody("action1"))
                .append("/", (request, response) -> response.setBody("action2"))
        ;

        Assert.assertEquals("action1", makeRequest("/context/action1").get().asString());
        Assert.assertEquals("action2", makeRequest("/context/").get().asString());
        Assert.assertEquals("/ not found", makeRequest("/").get().asString());
    }
}
