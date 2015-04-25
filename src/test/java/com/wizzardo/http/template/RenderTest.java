package com.wizzardo.http.template;

import com.wizzardo.http.template.taglib.g.Each;
import com.wizzardo.tools.xml.Node;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
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
        Node n = Node.parse("<div style=\"width: 100px\"><a href=\"http://${host.toLowerCase()}\">yandex</a><br></div>", true);

        RenderableList l = new RenderableList();
        ViewRenderer.prepare(n.children(), l, "", "");


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
        ViewRenderer.prepare(s, l);
        Assert.assertEquals(1, l.size());
        Assert.assertEquals("abc", l.get(0).get(null).toString());

        reset(l, model);
        s = "abc$qwerty";
        model.put("qwerty", "abc");
        ViewRenderer.prepare(s, l);
        Assert.assertEquals(2, l.size());
        Assert.assertEquals("abc", l.get(0).get(null).toString());
        Assert.assertEquals("abc", l.get(1).get(model).toString());


        reset(l, model);
        s = "foo${qwerty}bar";
        model.put("qwerty", "abc");
        ViewRenderer.prepare(s, l);
        Assert.assertEquals(3, l.size());
        Assert.assertEquals("foo", l.get(0).get(null).toString());
        Assert.assertEquals("abc", l.get(1).get(model).toString());
        Assert.assertEquals("bar", l.get(2).get(null).toString());


        reset(l, model);
        s = "abc$qwerty-$qwerty";
        model.put("qwerty", "abc");
        ViewRenderer.prepare(s, l);
        Assert.assertEquals(4, l.size());
        Assert.assertEquals("abc", l.get(0).get(null).toString());
        Assert.assertEquals("abc", l.get(1).get(model).toString());
        Assert.assertEquals("-", l.get(2).get(model).toString());
        Assert.assertEquals("abc", l.get(3).get(model).toString());


        reset(l, model);
        s = "abc${qwerty}";
        model.put("qwerty", "abc");
        ViewRenderer.prepare(s, l);
        Assert.assertEquals(2, l.size());
        Assert.assertEquals("abc", l.get(0).get(null).toString());
        Assert.assertEquals("abc", l.get(1).get(model).toString());


        reset(l, model);
        s = "abc${qwerty+qwerty}";
        model.put("qwerty", "abc");
        ViewRenderer.prepare(s, l);
        Assert.assertEquals(2, l.size());
        Assert.assertEquals("abc", l.get(0).get(null).toString());
        Assert.assertEquals("abcabc", l.get(1).get(model).toString());
    }

    private void reset(List l, Map m) {
        l.clear();
        m.clear();
    }
}
