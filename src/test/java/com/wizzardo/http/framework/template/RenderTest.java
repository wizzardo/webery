package com.wizzardo.http.framework.template;

import com.wizzardo.http.framework.template.Model;
import com.wizzardo.http.framework.template.RenderResult;
import com.wizzardo.http.framework.template.RenderableList;
import com.wizzardo.http.framework.template.ViewRenderer;
import com.wizzardo.tools.xml.GspParser;
import com.wizzardo.tools.xml.Node;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: moxa
 * Date: 2/11/13
 */
public class RenderTest {
    @Test
    public void render() {
        Node n = new GspParser().parse("<div style=\"width: 100px\"><a href=\"http://${host.toLowerCase()}\">yandex</a><br></div>");

        RenderableList l = new RenderableList();
        new ViewRenderingService().prepare(n.children(), l, "", "", true);


        Model model = new Model();
        model.put("host", "YA.ru");
        RenderResult result = l.get(model);

//        System.out.println(result.toString());
        Assert.assertEquals(3, l.size());
        Assert.assertEquals("<div style=\"width: 100px\">\n" +
                "    <a href=\"http://ya.ru\">\n" +
                "        yandex\n" +
                "    </a>\n" +
                "    <br/>\n" +
                "</div>\n", result.toString());
    }

    @Test
    public void prepare() {
        String s;
        RenderableList l = new RenderableList();
        Map<String, Object> model = new HashMap<String, Object>();

        reset(l, model);
        s = "abc";
        new ViewRenderingService().prepare(s, l);
        Assert.assertEquals(1, l.size());
        Assert.assertEquals("abc", l.get(0).get(null).toString());

        reset(l, model);
        s = "abc$qwerty";
        model.put("qwerty", "abc");
        new ViewRenderingService().prepare(s, l);
        Assert.assertEquals(2, l.size());
        Assert.assertEquals("abc", l.get(0).get(null).toString());
        Assert.assertEquals("abc", l.get(1).get(model).toString());


        reset(l, model);
        s = "foo${qwerty}bar";
        model.put("qwerty", "abc");
        new ViewRenderingService().prepare(s, l);
        Assert.assertEquals(3, l.size());
        Assert.assertEquals("foo", l.get(0).get(null).toString());
        Assert.assertEquals("abc", l.get(1).get(model).toString());
        Assert.assertEquals("bar", l.get(2).get(null).toString());


        reset(l, model);
        s = "abc$qwerty-$qwerty";
        model.put("qwerty", "abc");
        new ViewRenderingService().prepare(s, l);
        Assert.assertEquals(4, l.size());
        Assert.assertEquals("abc", l.get(0).get(null).toString());
        Assert.assertEquals("abc", l.get(1).get(model).toString());
        Assert.assertEquals("-", l.get(2).get(model).toString());
        Assert.assertEquals("abc", l.get(3).get(model).toString());


        reset(l, model);
        s = "abc$qwerty+$qwerty";
        model.put("qwerty", "abc");
        new ViewRenderingService().prepare(s, l);
        Assert.assertEquals(4, l.size());
        Assert.assertEquals("abc", l.get(0).get(null).toString());
        Assert.assertEquals("abc", l.get(1).get(model).toString());
        Assert.assertEquals("+", l.get(2).get(model).toString());
        Assert.assertEquals("abc", l.get(3).get(model).toString());


        reset(l, model);
        s = "abc${qwerty}";
        model.put("qwerty", "abc");
        new ViewRenderingService().prepare(s, l);
        Assert.assertEquals(2, l.size());
        Assert.assertEquals("abc", l.get(0).get(null).toString());
        Assert.assertEquals("abc", l.get(1).get(model).toString());


        reset(l, model);
        s = "abc${qwerty+qwerty}";
        model.put("qwerty", "abc");
        new ViewRenderingService().prepare(s, l);
        Assert.assertEquals(2, l.size());
        Assert.assertEquals("abc", l.get(0).get(null).toString());
        Assert.assertEquals("abcabc", l.get(1).get(model).toString());


        reset(l, model);
        s = "a${b+\"${c}d\"}e";
        model.put("b", "b");
        model.put("c", "c");
        new ViewRenderingService().prepare(s, l);
        Assert.assertEquals(3, l.size());
        Assert.assertEquals("abcde", l.get(model).toString());
        Assert.assertEquals("a", l.get(0).get(null).toString());
        Assert.assertEquals("bcd", l.get(1).get(model).toString());
        Assert.assertEquals("e", l.get(2).get(model).toString());
    }

    private void reset(List l, Map m) {
        l.clear();
        m.clear();
    }

    @Test()
    public void test_ignore_comments() {
        String gsp = "<div>\n" +
                "    before\n" +
                "    %{--<p>text</p>--}%\n" +
                "    after\n" +
                "</div>";

        RenderableList renderable = new RenderableList();
        ViewRenderingService service = new ViewRenderingService();
        service.offset = "";
        service.prepare(new GspParser().parse(gsp).children(), renderable, "", "", false);
        Assert.assertEquals("<div>\n" +
                "    before\n" +
                "    \n" +
                "    after\n" +
                "</div>", renderable.get(null).toString());
    }
}
