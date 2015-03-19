package com.wizzardo.http.filter;

import com.wizzardo.http.ServerTest;
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
    public void testDigestAuth() throws IOException {
        handler = (request, response) -> response.setBody("ok");
        String user = "Mufasa";
        String password = "Circle Of Life";
        server.getFiltersMapping().addBefore("/*", new DigestAuthFilter("testrealm@host.com") {
            @Override
            protected String nonce() {
                return "dcd98b7102dd2f0e8b11d0f600bfb0c093";
            }
        }.allow(user, password));

        Assert.assertEquals(401, makeRequest("").get().getResponseCode());
        Assert.assertEquals("ok", makeRequest("/dir/index.html").header("Authorization", "Digest username=\"Mufasa\", realm=\"testrealm@host.com\", nonce=\"dcd98b7102dd2f0e8b11d0f600bfb0c093\", uri=\"/dir/index.html\", qop=auth, nc=00000001, cnonce=\"0a4f113b\", response=\"6629fae49393a05397450978507c4ef1\"").get().asString());
    }

    @Test
    public void testBasicAuthToken() throws IOException {
        TokenFilter tokenFilter = new TokenFilter(new BasicAuthFilter());

        handler = (request, response) -> {
            if (request.param("token") != null)
                return response.setBody("ok");
            else
                return response.setBody(tokenFilter.generateToken(request));
        };

        String user = "user";
        String password = "password";
        server.getFiltersMapping().addBefore("/*", tokenFilter.allow(user, password));

        Assert.assertEquals(401, makeRequest("").get().getResponseCode());
        String token = makeRequest("").setBasicAuthentication(user, password).get().asString();
        Assert.assertNotNull(token);
        Assert.assertEquals("ok", makeRequest("").param("token", token).get().asString());
    }

    @Test
    public void testChain() throws IOException {
        handler = (request, response) -> response.setBody(request.param("all") + " " + request.param("foo") + " " + request.param("bar"));
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

    @Test
    public void testChainReverse() throws IOException {
        handler = (request, response) -> response.setBody(request.param("all") + " " + request.param("foo") + " " + request.param("bar"));
        server.getFiltersMapping().addBefore("/foo/bar/*", (request, response) -> {
            request.param("bar", "true");
            return true;
        });
        server.getFiltersMapping().addBefore("/foo/*", (request, response) -> {
            request.param("foo", "true");
            return true;
        });
        server.getFiltersMapping().addBefore("/*", (request, response) -> {
            request.param("all", "true");
            return true;
        });

        Assert.assertEquals("true true true", makeRequest("/foo/bar/123").get().asString());
        Assert.assertEquals("true true true", makeRequest("/foo/bar").get().asString());
        Assert.assertEquals("true true null", makeRequest("/foo/").get().asString());
        Assert.assertEquals("true true null", makeRequest("/foo").get().asString());
        Assert.assertEquals("true null null", makeRequest("/").get().asString());
    }

    @Test
    public void testChainShuffle() throws IOException {
        handler = (request, response) -> response.setBody(request.param("all") + " " + request.param("foo") + " " + request.param("bar"));
        server.getFiltersMapping().addBefore("/foo/bar/*", (request, response) -> {
            request.param("bar", "true");
            return true;
        });
        server.getFiltersMapping().addBefore("/*", (request, response) -> {
            request.param("all", "true");
            return true;
        });
        server.getFiltersMapping().addBefore("/foo/*", (request, response) -> {
            request.param("foo", "true");
            return true;
        });

        Assert.assertEquals("true true true", makeRequest("/foo/bar/123").get().asString());
        Assert.assertEquals("true true true", makeRequest("/foo/bar").get().asString());
        Assert.assertEquals("true true null", makeRequest("/foo/").get().asString());
        Assert.assertEquals("true true null", makeRequest("/foo").get().asString());
        Assert.assertEquals("true null null", makeRequest("/").get().asString());
    }


    @Test
    public void testEndsWithMapping() throws IOException {
        handler = (request, response) -> response.setBody(request.param("foo"));
        server.getFiltersMapping().addBefore("*.foo", (request, response) -> {
            request.param("foo", "true");
            return true;
        });
        Assert.assertEquals("true", makeRequest("/bar.foo").get().asString());
        Assert.assertEquals("true", makeRequest("/bar/qwerty.foo").get().asString());


        handler = (request, response) -> response.setBody(request.param("foo") + " " + request.param("bar"));
        server.getFiltersMapping().addBefore("/bar/*", (request, response) -> {
            request.param("bar", "true");
            return true;
        });
        Assert.assertEquals("true true", makeRequest("/bar/qwerty.foo").get().asString());
        Assert.assertEquals("true null", makeRequest("/1.foo").get().asString());
        Assert.assertEquals("null true", makeRequest("/bar/1").get().asString());


        handler = (request, response) -> response.setBody(request.param("foo") + " " + request.param("bar") + " " + request.param("qwe"));
        server.getFiltersMapping().addBefore("/*", (request, response) -> {
            request.param("qwe", "true");
            return true;
        });
        Assert.assertEquals("true true true", makeRequest("/bar/qwerty.foo").get().asString());
        Assert.assertEquals("true null true", makeRequest("/1.foo").get().asString());
        Assert.assertEquals("null true true", makeRequest("/bar/1").get().asString());


        handler = (request, response) -> response.setBody(request.param("foo") + " " + request.param("bar") + " " + request.param("qwe") + " " + request.param("foobar"));
        server.getFiltersMapping().addBefore("*.bar.foo", (request, response) -> {
            request.param("foobar", "true");
            return true;
        });
        Assert.assertEquals("true true true true", makeRequest("/bar/qwerty.bar.foo").get().asString());
        Assert.assertEquals("true null true null", makeRequest("/1.foo").get().asString());
        Assert.assertEquals("true null true true", makeRequest("/1.bar.foo").get().asString());
    }
}
