package com.wizzardo.http.framework.template.taglib;

import com.wizzardo.http.framework.Controller;
import com.wizzardo.http.framework.WebApplicationTest;
import com.wizzardo.http.framework.template.Model;
import com.wizzardo.http.framework.template.RenderResult;
import com.wizzardo.http.framework.template.Renderer;
import com.wizzardo.http.framework.template.TagLib;
import com.wizzardo.http.framework.template.taglib.g.CreateLink;
import com.wizzardo.http.framework.template.taglib.g.Link;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by wizzardo on 26.04.15.
 */
public class CreateLinkTest extends WebApplicationTest implements TagTest {

    public static class BookController extends Controller {

        public Renderer list() {
            return renderString("list of books");
        }

        public Renderer show() {
            return renderString("some book");
        }
    }

    @Override
    public void setUp() throws NoSuchMethodException, ClassNotFoundException, NoSuchFieldException {
        super.setUp();
        TagLib.findTags(Collections.singletonList(CreateLink.class));
        server.getUrlMapping()
                .append("/book/list", BookController.class, "list")
                .append("/book/$id", BookController.class, "show")
        ;
    }

    @Test
    public void testLinkTag() {
        Map<String, String> attrs = new LinkedHashMap<String, String>();
        Map<String, Object> model = new LinkedHashMap<String, Object>();

        attrs.put("controller", "book");
        attrs.put("action", "list");
        Assert.assertEquals("/book/list\n", new CreateLink().init(new LinkedHashMap<>(attrs)).get(model).toString());

        attrs.put("absolute", "true");
        Assert.assertEquals("http://localhost:9999/book/list\n", new CreateLink().init(new LinkedHashMap<>(attrs)).get(model).toString());

        attrs.put("base", "http://ya.ru");
        Assert.assertEquals("http://ya.ru/book/list\n", new CreateLink().init(new LinkedHashMap<>(attrs)).get(model).toString());

        attrs.put("fragment", "some_fragment");
        Assert.assertEquals("http://ya.ru/book/list#some_fragment\n", new CreateLink().init(new LinkedHashMap<>(attrs)).get(model).toString());

        attrs.put("params", "[key:'value']");
        Assert.assertEquals("http://ya.ru/book/list?key=value#some_fragment\n", new CreateLink().init(new LinkedHashMap<>(attrs)).get(model).toString());

        attrs.put("params", "[key:'value', key2: 'value2']");
        Assert.assertEquals("http://ya.ru/book/list?key=value&key2=value2#some_fragment\n", new CreateLink().init(new LinkedHashMap<>(attrs)).get(model).toString());


        attrs.clear();
        attrs.put("controller", "book");
        attrs.put("action", "show");
        attrs.put("params", "[id:1]");
        Assert.assertEquals("/book/1\n", new CreateLink().init(new LinkedHashMap<>(attrs)).get(model).toString());


        attrs.clear();
        attrs.put("controller", "book");
        attrs.put("action", "show");
        attrs.put("params", "[id:id]");
        model.put("id", 1);
        Assert.assertEquals("/book/1\n", new CreateLink().init(new LinkedHashMap<>(attrs)).get(model).toString());

    }

    @Test
    public void testRenderableString() {
        RenderResult result = prepare("<a href=\"${createLink([controller:'book', action:'show', id: 1])}\">link</a>")
                .get(new Model());
        Assert.assertEquals("<a href=\"/book/1\">\n" +
                "    link\n" +
                "</a>\n", result.toString());

        result = prepare("${createLink([controller:'book', action:'show', id: 1, params: [foo: 'bar', boo: 'far']])}")
                .get(new Model());
        Assert.assertEquals("/book/1?foo=bar&boo=far\n", result.toString());

        result = prepare("${createLink([controller:'book', action:'list', params: [foo: 'bar']])}")
                .get(new Model());
        Assert.assertEquals("/book/list?foo=bar\n", result.toString());

        result = prepare("${createLink([controller:'book', action:'list', absolute: true])}")
                .get(new Model());
        Assert.assertEquals("http://localhost:9999/book/list\n", result.toString());

        result = prepare("${createLink([controller:'book', action:'list', base: 'http://ya.ru', params: [foo: 'bar']])}")
                .get(new Model());
        Assert.assertEquals("http://ya.ru/book/list?foo=bar\n", result.toString());

        result = prepare("${createLink([controller:'book', action:'list', fragment: 'foo'])}")
                .get(new Model());
        Assert.assertEquals("/book/list#foo\n", result.toString());

        checkException(() -> prepare("${createLink([controller:'none', action:'list'])}").get(new Model()),
                IllegalStateException.class, "can not find mapping for controller 'none' and action:'list'");
    }

    @Test
    public void testInTemplate() {
        Assert.assertEquals("" +
                "<div>\n" +
                "    /book/1\n" +
                "</div>\n", prepare("<div><g:createLink controller=\"book\" action=\"show\" params=\"[id: 1]\"/></div>")
                .get(new Model()).toString());
    }

    @Test
    public void test_exception() {
        checkException(() -> prepare("<div><g:createLink controller=\"none\" action=\"index\" params=\"[id: 1]\"/></div>"),
                IllegalStateException.class, "can not find mapping for controller 'none' and action:'index'");
    }
}
