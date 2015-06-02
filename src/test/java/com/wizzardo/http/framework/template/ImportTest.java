package com.wizzardo.http.framework.template;

import com.wizzardo.http.framework.WebApplicationTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;

/**
 * Created by wizzardo on 02.06.15.
 */
public class ImportTest extends WebApplicationTest {

    public static class Foo {
        @Override
        public String toString() {
            return "foo";
        }
    }

    @Test
    public void test1() {
        Assert.assertEquals("" +
                "<html>\n" +
                "    <head>\n" +
                "        <title>\n" +
                "            Hello World!\n" +
                "        </title>\n" +
                "    </head>\n" +
                "    <body>\n" +
                "        foo " + new Date().getTime() / 1000 + "\n" +
                "    </body>\n" +
                "</html>\n", new ViewRenderer(new Model(), "importTest", "test1").render().toString());
    }
}
