package com.wizzardo.http.framework.template.taglib;

import com.wizzardo.http.framework.WebApplicationTest;
import com.wizzardo.http.framework.template.Model;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by wizzardo on 27.06.15.
 */
public class ResourceTest extends WebApplicationTest implements TagTest {

    @Test
    public void test_static_js() {
        Assert.assertEquals("" +
                "<div>\n" +
                "    <script type=\"text/javascript\" src=\"/static/js/test.v207A.js\"></script>\n" +
                "</div>\n", prepare("<div><g:resource dir=\"js\" file=\"test.js\"/></div>")
                .get(new Model()).toString());
    }

    @Test
    public void test_static_css() {
        Assert.assertEquals("" +
                "<div>\n" +
                "    <link rel=\"stylesheet\" href=\"/static/css/test.vEA6A.css\">\n" +
                "</div>\n", prepare("<div><g:resource dir=\"css\" file=\"test.css\"/></div>")
                .get(new Model()).toString());
    }

    @Test
    public void test_static_css2() {
        Assert.assertEquals("" +
                "<div>\n" +
                "    <link rel=\"stylesheet\" href=\"http://localhost:" + port + "/static/css/test.vEA6A.css\">\n" +
                "</div>\n", prepare("<div><g:resource dir=\"css\" file=\"test.css\" absolute=\"true\"/></div>")
                .get(new Model()).toString());
    }

    @Test
    public void test_static_img() {
        Assert.assertEquals("" +
                "<div>\n" +
                "    <img src=\"/static/img/test.jpg\"/>\n" +
                "</div>\n", prepare("<div><img src=\"${resource(dir:'img', file:'test.jpg')}\"></div>")
                .get(new Model()).toString());
    }

    @Test
    public void test_static_img2() {
        Assert.assertEquals("" +
                "<div>\n" +
                "    <img src=\"http://localhost:" + port + "/static/img/test.jpg\"/>\n" +
                "</div>\n", prepare("<div><img src=\"${resource(dir:'/img/', file:'test.jpg', absolute: true)}\"></div>")
                .get(new Model()).toString());
    }

    @Test
    public void test_static_img3() {
        Assert.assertEquals("" +
                "<div>\n" +
                "    <img src=\"/static/\"/>\n" +
                "</div>\n", prepare("<div><img src=\"${resource(file:'')}\"></div>")
                .get(new Model()).toString());
    }

    @Test
    public void test_dynamic_js() {
        Assert.assertEquals("" +
                "<div>\n" +
                "    <script type=\"text/javascript\" src=\"/static/js/test.v207A.js\"></script>\n" +
                "</div>\n", prepare("<div><g:resource dir=\"js\" file=\"${js}\"/></div>")
                .get(new Model().append("js", "test.js")).toString());
    }

    @Test
    public void test_dynamic_css() {
        Assert.assertEquals("" +
                "<div>\n" +
                "    <link rel=\"stylesheet\" href=\"/static/css/test.vEA6A.css\">\n" +
                "</div>\n", prepare("<div><g:resource dir=\"/css/\" file=\"${css}\"/></div>")
                .get(new Model().append("css", "test.css")).toString());
    }

    @Test
    public void test_empty_file() {
        Assert.assertEquals("" +
                "<div>\n" +
                "    \n" +
                "</div>\n", prepare("<div><g:resource file=\"${''}\"/></div>")
                .get(new Model().append("css", "test.css")).toString());
    }
}
