package com.wizzardo.http.filter;

import com.wizzardo.http.Filter;
import com.wizzardo.http.ServerTest;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * @author: wizzardo
 * Date: 29.11.14
 */
public class FilterTest extends ServerTest {

    @Test
    public void testBasicAuth() throws IOException {
        handler = (request, response) -> response.setBody("ok");
        String user = "user";
        String password = "password";
        server.getFiltersMapping().addBefore("/*", new BasicAuthFilter().allow(user, password));

        Assert.assertEquals(401, makeRequest("").get().getResponseCode());
        Assert.assertEquals("ok", makeRequest("").setBasicAuthentication(user, password).get().asString());
    }

    @Test
    public void testChain() throws IOException {
        handler = (request, response) -> response.setBody(request.param("all")+" "+request.param("foo")+" "+request.param("bar"));
        server.getFiltersMapping().addBefore("/*", (request, response) -> {
            request.param("all", "true");
            return true;
        });
        server.getFiltersMapping().addBefore("/foo/*", (request, response) -> {
            request.param("foo", "true");
            return true;
        });
        server.getFiltersMapping().addBefore("/foo/bar/*", (request, response) -> {
            request.param("bar", "true");
            return true;
        });

        Assert.assertEquals("true true true", makeRequest("/foo/bar/123").get().asString());
        Assert.assertEquals("true true true", makeRequest("/foo/bar").get().asString());
        Assert.assertEquals("true true null", makeRequest("/foo/").get().asString());
        Assert.assertEquals("true true null", makeRequest("/foo").get().asString());
        Assert.assertEquals("true null null", makeRequest("/").get().asString());
    }
}
