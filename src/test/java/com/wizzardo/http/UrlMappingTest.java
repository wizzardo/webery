package com.wizzardo.http;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * @author: wizzardo
 * Date: 25.09.14
 */
public class UrlMappingTest extends ServerTest {

    @Test
    public void testMapping() throws IOException {
        handler = new UrlHandler()
                .append("/action1", (request, response) -> response.setBody("action1"))
                .append("/action2", (request, response) -> response.setBody("action2"))
                .append("/$action/1", (request, response) -> response.setBody(request.param("action")))
                .append("/2/$action?", (request, response) -> response.setBody(request.paramWithDefault("action", "default")))
                .append("/3/$action?/${id}?", (request, response) ->
                        response.setBody(request.paramWithDefault("action", "action2") + "+" + request.paramWithDefault("id", "action3")))
                .append("/any/*", (request, response) -> response.setBody("any"))
                .append("/pattern/${foo}-${bar}", (request, response) ->
                        response.setBody(request.param("foo") + "-" + request.param("bar")))
        ;

        Assert.assertEquals("action1", makeRequest("/action1").get().asString());
        Assert.assertEquals("action2", makeRequest("/action2").get().asString());
        Assert.assertEquals("action5", makeRequest("/action5/1").get().asString());
        Assert.assertEquals("action6", makeRequest("/2/action6").get().asString());
        Assert.assertEquals("default", makeRequest("/2").get().asString());
        Assert.assertEquals("ololo+123", makeRequest("/3/ololo/123").get().asString());
        Assert.assertEquals("action2+action3", makeRequest("/3").get().asString());

        Assert.assertEquals("any", makeRequest("/any").get().asString());
        Assert.assertEquals("any", makeRequest("/any/foo").get().asString());
        Assert.assertEquals("any", makeRequest("/any/foo/bar").get().asString());

        Assert.assertEquals("foo-bar", makeRequest("/pattern/foo-bar").get().asString());
    }

    @Test
    public void testEndsWith() throws IOException {
        handler = new UrlHandler()
                .append("*.html", (request, response) -> response.setBody("html"))
                .append("/special/*.html", (request, response) -> response.setBody("special"));


        Assert.assertEquals("html", makeRequest("/foo.html").get().asString());
        Assert.assertEquals("html", makeRequest("/foo/bar.html").get().asString());
        Assert.assertEquals("html", makeRequest("/foo/bar/qwerty.html").get().asString());

        Assert.assertEquals("special", makeRequest("/special/foo.html").get().asString());
        Assert.assertEquals("special", makeRequest("/special/foo/bar.html").get().asString());
        Assert.assertEquals("special", makeRequest("/special/foo/bar/qwerty.html").get().asString());
    }
}
