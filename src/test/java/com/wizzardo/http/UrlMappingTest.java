package com.wizzardo.http;

import com.wizzardo.http.mapping.TemplatesHolder;
import com.wizzardo.http.request.Parameters;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

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
    public void testMappingVariables() throws IOException {
        Handler testHandler = (request, response) -> {
            Parameters params = request.params();
            return response.setBody(params.get("a", " ") + "-" + params.get("b", " ") + "-" + params.get("c", " ") + "-" + params.get("d", " ") + "-" + params.get("e", " "));
        };

        this.handler = new UrlHandler()
                .append("/$a/$b/$c", testHandler)
                .append("/$a/$c/$d/$e", testHandler)
        ;

        Assert.assertEquals("a-b-c- - ", makeRequest("/a/b/c").get().asString());
        Assert.assertEquals("a- -c-d-e", makeRequest("/a/c/d/e").get().asString());
    }

    @Test
    public void testMapping_regex() throws IOException {
        handler = new UrlHandler()
                .append("/action(\\d+)*", (request, response) -> response.setBody("ok"))
        ;

        Assert.assertEquals("ok", makeRequest("/action").get().asString());
        Assert.assertEquals("ok", makeRequest("/action1").get().asString());
        Assert.assertEquals(404, makeRequest("/404").get().getResponseCode());
    }

    @Test
    public void testEndsWith() throws IOException {
        handler = new UrlHandler()
                .append("*.html", (request, response) -> response.setBody("html"))
                .append("*.xhtml", (request, response) -> response.setBody("xhtml"))
                .append("/special/*.html", (request, response) -> response.setBody("special"));


        Assert.assertEquals("html", makeRequest("/foo.html").get().asString());
        Assert.assertEquals("html", makeRequest("/foo/bar.html").get().asString());
        Assert.assertEquals("html", makeRequest("/foo/bar/qwerty.html").get().asString());

        Assert.assertEquals("special", makeRequest("/special/foo.html").get().asString());
        Assert.assertEquals("special", makeRequest("/special/foo/bar.html").get().asString());
        Assert.assertEquals("special", makeRequest("/special/foo/bar/qwerty.html").get().asString());

        Assert.assertEquals("xhtml", makeRequest("/foo.xhtml").get().asString());
        Assert.assertEquals("xhtml", makeRequest("/foo/bar.xhtml").get().asString());
        Assert.assertEquals("xhtml", makeRequest("/foo/bar/qwerty.xhtml").get().asString());
    }

    @Test
    public void testUrlTemplates() throws IOException {
        TemplatesHolder<String> templates = new TemplatesHolder<>("localhost", 8080, "context");
        templates
                .append("action1", "/action1")
                .append("action2", "/action2")
                .append("action3", "/3/$action?/${id}?")
                .append("action4", "/pattern/${foo}-${bar}")
                .append("action5", "/$action/1")
                .append("action6", "/2/$action?")
                .append("any", "/any/*")
                .append("action7", "/any/${var}/*")
                .append("action8", "/any/${var}-*")
        ;

        Assert.assertEquals("/context/action1", templates.getTemplate("action1").getRelativeUrl());
        Assert.assertEquals("/context/action2", templates.getTemplate("action2").getRelativeUrl());
        Assert.assertEquals("http://localhost:8080/context/action2", templates.getTemplate("action2").getAbsoluteUrl());
        Assert.assertEquals("/context/3/foo/123", templates.getTemplate("action3").getRelativeUrl(new HashMap<String, Object>() {{
            put("action", "foo");
            put("id", 123);
        }}));
        Assert.assertEquals("/context/3/foo", templates.getTemplate("action3").getRelativeUrl(new HashMap<String, Object>() {{
            put("action", "foo");
        }}));
        Assert.assertEquals("/context/3", templates.getTemplate("action3").getRelativeUrl());
        Assert.assertEquals("/context/pattern/foo-bar", templates.getTemplate("action4").getRelativeUrl(new HashMap<String, Object>() {{
            put("foo", "foo");
            put("bar", "bar");
        }}));
        Assert.assertEquals("/context/foo/1", templates.getTemplate("action5").getRelativeUrl(new HashMap<String, Object>() {{
            put("action", "foo");
        }}));
        Assert.assertEquals("/context/2/foo", templates.getTemplate("action6").getRelativeUrl(new HashMap<String, Object>() {{
            put("action", "foo");
        }}));
        Assert.assertEquals("/context/2", templates.getTemplate("action6").getRelativeUrl());

        Assert.assertEquals("/context/any", templates.getTemplate("any").getRelativeUrl());
        Assert.assertEquals("/context/any/foo/bar", templates.getTemplate("any").getRelativeUrl("/foo/bar"));
        Assert.assertEquals("/context/any/foo", templates.getTemplate("action7").getRelativeUrl(new HashMap<String, Object>() {{
            put("var", "foo");
        }}));
        Assert.assertEquals("/context/any/foo/foo/bar", templates.getTemplate("action7").getRelativeUrl(new HashMap<String, Object>() {{
            put("var", "foo");
        }}, "/foo/bar"));
        Assert.assertEquals("/context/any/foo-", templates.getTemplate("action8").getRelativeUrl(new HashMap<String, Object>() {{
            put("var", "foo");
        }}));
        Assert.assertEquals("/context/any/foo-foo/bar", templates.getTemplate("action8").getRelativeUrl(new HashMap<String, Object>() {{
            put("var", "foo");
        }}, "foo/bar"));
    }

    @Test
    public void testUrlTemplatesVariables() throws IOException {
        TemplatesHolder<String> templates = new TemplatesHolder<>("localhost", 8080);
        templates
                .append("action1", "/${var}")
                .append("action2", "/prefix-${var}")
                .append("action3", "/prefix-${var}-suffix")
        ;

        Assert.assertEquals("/null", templates.getTemplate("action1").getRelativeUrl());
        Assert.assertEquals("/foo", templates.getTemplate("action1").getRelativeUrl(new HashMap<String, Object>() {{
            put("var", "foo");
        }}));
        Assert.assertEquals("/prefix-foo", templates.getTemplate("action2").getRelativeUrl(new HashMap<String, Object>() {{
            put("var", "foo");
        }}));
        Assert.assertEquals("/prefix-foo-suffix", templates.getTemplate("action3").getRelativeUrl(new HashMap<String, Object>() {{
            put("var", "foo");
        }}));
    }

    @Test
    public void testUrlTemplatesVariablesUrlEncode() throws IOException {
        TemplatesHolder<String> templates = new TemplatesHolder<>("localhost", 8080);
        templates
                .append("action1", "/${var}")
        ;

        Assert.assertEquals("/foo%26bar", templates.getTemplate("action1").getRelativeUrl(new HashMap<String, Object>() {{
            put("var", "foo&bar");
        }}));
        Assert.assertEquals("/foo%26bar?key=a%3Db", templates.getTemplate("action1").getRelativeUrl(new HashMap<String, Object>() {{
            put("var", "foo&bar");
            put("key", "a=b");
        }}));
    }
}
