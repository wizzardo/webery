package com.wizzardo.http.framework.template;

import com.wizzardo.http.framework.Controller;
import com.wizzardo.http.framework.WebApplicationTest;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by wizzardo on 29.05.15.
 */
public class RenderTagTest extends WebApplicationTest {

    public static class RenderTagTestController extends Controller {
        public Renderer test_simple() {
            return renderView();
        }

        public Renderer test_shared() {
            return renderView();
        }

        public Renderer test_model() {
            return renderView();
        }
    }

    @Test
    public void test_simple() throws IOException {
        server.getUrlMapping()
                .append(path(), RenderTagTestController.class, name());

        Assert.assertEquals("" +
                "<html>\n" +
                "    <body>\n" +
                "        foo bar\n" +
                "    </body>\n" +
                "</html>\n", get());
    }

    @Test
    public void test_shared() throws IOException {
        server.getUrlMapping()
                .append(path(), RenderTagTestController.class, name());

        Assert.assertEquals("" +
                "<html>\n" +
                "    <body>\n" +
                "        bar foo\n" +
                "    </body>\n" +
                "</html>\n", get());
    }

    @Test
    public void test_model() throws IOException {
        server.getUrlMapping()
                .append(path(), RenderTagTestController.class, name());

        Assert.assertEquals("" +
                "<html>\n" +
                "    <body>\n" +
                "        <p>\n" +
                "            bar. foo\n" +
                "        </p>\n" +
                "    </body>\n" +
                "</html>\n", get());
    }

}
