package com.wizzardo.http.html;

import com.wizzardo.http.ServerTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TagTest {

    @Test
    public void test_each() {
        AtomicInteger counter = new AtomicInteger();
        new Tag().each(Arrays.asList(1, 2, 3), (integer, tag) -> counter.addAndGet(integer));

        Assert.assertEquals(6, counter.get());
    }

    @Test
    public void test_each2() {
        AtomicInteger counter = new AtomicInteger();
        new Tag().each(new Integer[]{1, 2, 3}, (integer, tag) -> counter.addAndGet(integer));

        Assert.assertEquals(6, counter.get());
    }

    @Test
    public void test_each_null_collection() {
        AtomicInteger counter = new AtomicInteger();
        new Tag().each((List<Integer>) null, (integer, tag) -> counter.addAndGet(integer));

        Assert.assertEquals(0, counter.get());
    }

    @Test
    public void test_each_null_collection2() {
        AtomicInteger counter = new AtomicInteger();
        new Tag().each((Integer[]) null, (integer, tag) -> counter.addAndGet(integer));

        Assert.assertEquals(0, counter.get());
    }

    @Test
    public void test_addIf() {
        Tag tag = new Tag();

        Assert.assertSame(tag, new Tag().addIf(true, tag).body.get(0));
        Assert.assertNull(new Tag().addIf(false, tag).body);
    }

    @Test
    public void test_addIf2() {
        Tag tag = new Tag();

        Assert.assertSame(tag, new Tag().addIf(true, () -> tag).body.get(0));
        Assert.assertNull(new Tag().addIf(false, () -> tag).body);
    }

    @Test
    public void test_basic_attrs() {
        Tag tag = new Tag();

        Assert.assertEquals("id", tag.id("id").attrs.get("id"));
        Assert.assertEquals("class", tag.clazz("class").attrs.get("class"));
        Assert.assertEquals("style", tag.style("style").attrs.get("style"));
    }


    @Test
    public void test_text_exceptions() {
        ServerTest.checkException(() -> new Tag.Text("foo").attr("foo", "bar"), IllegalStateException.class, "Text tag can not have any attributes");
        ServerTest.checkException(() -> new Tag.Text("foo").add(new Tag()), IllegalStateException.class, "Text tag can not have any inner tags");
    }

    @Test
    public void test_script() {
        Tag.Script script = new Tag.Script();

        Assert.assertEquals("script", script.name);
        Assert.assertEquals("text/javascript", script.attrs.get("type"));
        Assert.assertEquals("src", script.src("src").attrs.get("src"));
    }

    @Test
    public void test_link() {
        Tag.Link link = new Tag.Link();

        Assert.assertEquals("link", link.name);
        Assert.assertEquals("stylesheet", link.attrs.get("rel"));
        Assert.assertEquals("href", link.href("href").attrs.get("href"));
    }

    @Test
    public void test_form() {
        Tag.Form form = new Tag.Form();

        Assert.assertEquals("form", form.name);
        Assert.assertEquals("method", form.method("method").attrs.get("method"));
        Assert.assertEquals("action", form.action("action").attrs.get("action"));
        Assert.assertEquals("enctype", form.enctype("enctype").attrs.get("enctype"));
    }


    @Test
    public void test_input() {
        Tag.Input input = new Tag.Input();

        Assert.assertEquals("input", input.name);
        Assert.assertEquals("type", input.type("type").attrs.get("type"));
        Assert.assertEquals("name", input.name("name").attrs.get("name"));
        Assert.assertEquals("value", input.value("value").attrs.get("value"));
        Assert.assertEquals("placeholder", input.placeholder("placeholder").attrs.get("placeholder"));
    }


    @Test
    public void test_select() {
        Tag.Select select = new Tag.Select(Arrays.asList(1, 2, 3));

        Assert.assertEquals("select", select.name);
        Assert.assertEquals(3, select.body.size());

        Assert.assertEquals("option", select.body.get(0).name);
        Assert.assertEquals("1", select.body.get(0).attrs.get("value"));
        Assert.assertEquals("1", ((Tag.Text) select.body.get(0).body.get(0)).name);

        Assert.assertEquals("option", select.body.get(1).name);
        Assert.assertEquals("2", select.body.get(1).attrs.get("value"));
        Assert.assertEquals("2", ((Tag.Text) select.body.get(1).body.get(0)).name);

        Assert.assertEquals("option", select.body.get(2).name);
        Assert.assertEquals("3", select.body.get(2).attrs.get("value"));
        Assert.assertEquals("3", ((Tag.Text) select.body.get(2).body.get(0)).name);
    }

    @Test
    public void test_select2() {
        Map<String, Integer> values = new LinkedHashMap<>();
        values.put("option_1", 1);
        values.put("option_2", 2);
        values.put("option_3", 3);

        Tag.Select select = new Tag.Select(values);

        Assert.assertEquals("select", select.name);
        Assert.assertEquals(3, select.body.size());

        Assert.assertEquals("option", select.body.get(0).name);
        Assert.assertEquals("option_1", select.body.get(0).attrs.get("value"));
        Assert.assertEquals("1", ((Tag.Text) select.body.get(0).body.get(0)).name);

        Assert.assertEquals("option", select.body.get(1).name);
        Assert.assertEquals("option_2", select.body.get(1).attrs.get("value"));
        Assert.assertEquals("2", ((Tag.Text) select.body.get(1).body.get(0)).name);

        Assert.assertEquals("option", select.body.get(2).name);
        Assert.assertEquals("option_3", select.body.get(2).attrs.get("value"));
        Assert.assertEquals("3", ((Tag.Text) select.body.get(2).body.get(0)).name);
    }

}