package com.wizzardo.http.framework.template;

import com.wizzardo.http.framework.WebApplicationTest;
import com.wizzardo.tools.xml.Node;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Created by wizzardo on 29.05.15.
 */
public class LayoutTest extends WebApplicationTest {

    @Test
    public void test1() {
        Assert.assertEquals("" +
                "<html>\n" +
                "    <head>\n" +
                "        <title>\n" +
                "            Hello World!\n" +
                "        </title>\n" +
                "        <script src=\"global.js\"/>\n" +
                "        <script src=\"myscript.js\"/>\n" +
                "    </head>\n" +
                "    <body>\n" +
                "        Page to be decorated\n" +
                "    </body>\n" +
                "</html>\n", new ViewRenderer(new Model(), "layoutTest", "test1").render().toString());
    }
}
