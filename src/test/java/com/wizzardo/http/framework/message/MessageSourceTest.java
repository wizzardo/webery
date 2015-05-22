package com.wizzardo.http.framework.message;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by wizzardo on 21.05.15.
 */
public class MessageSourceTest {

    @Test
    public void test_template() {
        Assert.assertEquals("foo bar", new Template("foo bar").get());
        Assert.assertEquals("foo bar", new Template("foo {0}").get("bar"));
        Assert.assertEquals("foo bar", new Template("{0} bar").get("foo"));
        Assert.assertEquals("foo bar", new Template("{0} {1}").get("foo", "bar"));
        Assert.assertEquals("foo bar", new Template("{1} {0}").get("bar", "foo"));
        Assert.assertEquals("bar bar", new Template("{0} {0}").get("bar"));
        Assert.assertEquals("foo null", new Template("foo {0}").get());
    }

    @Test
    public void test_properties() {
        MessageSource ms = new PropertiesMessageSource("messages");
        Assert.assertEquals("bar", ms.get("foo"));
    }
}
