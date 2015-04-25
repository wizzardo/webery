package com.wizzardo.http.template;

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
