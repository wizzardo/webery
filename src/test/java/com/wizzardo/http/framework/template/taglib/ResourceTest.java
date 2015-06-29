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
    public void test_1() {
        Assert.assertEquals("" +
                "<div>\n" +
                "    <script type=\"text/javascript\" src=\"/static/js/test.js\"></script>\n" +
                "</div>\n", prepare("<div><g:resource dir=\"js\" file=\"test.js\"/></div>")
                .get(new Model()).toString());
    }

    @Test
    public void test_2() {
        Assert.assertEquals("" +
                "<div>\n" +
                "    <link rel=\"stylesheet\" href=\"/static/css/test.css\">\n" +
                "</div>\n", prepare("<div><g:resource dir=\"css\" file=\"test.css\"/></div>")
                .get(new Model()).toString());
    }

    @Test
    public void test_3() {
        Assert.assertEquals("" +
                "<div>\n" +
                "    <img src=\"/static/img/test.jpg\"/>\n" +
                "</div>\n", prepare("<div><img src=\"${resource(dir:'img', file:'test.jpg')}\"></div>")
                .get(new Model()).toString());
    }
}
