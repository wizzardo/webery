package com.wizzardo.http.framework.template;

import com.wizzardo.http.framework.Controller;
import com.wizzardo.http.framework.WebApplicationTest;
import com.wizzardo.tools.misc.With;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

}
