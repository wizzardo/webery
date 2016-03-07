package com.wizzardo.http.framework.message;

import org.junit.Assert;
import org.junit.Test;

import java.util.Locale;

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

    @Test
    public void test_messageBundle() {
        MessageBundle ms = new MessageBundle().load("messages");
        Assert.assertEquals("bar", ms.get("foo"));
        Assert.assertEquals("no localization", ms.get("no_localization"));

        Assert.assertEquals("bar ru", ms.get(new Locale("ru", "RU"), "foo"));
        ms.setDefaultLocale(new Locale("ru", "RU"));

        Assert.assertEquals("bar ru", ms.get("foo"));
        Assert.assertEquals("no localization", ms.get("no_localization"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_messageBundle_npe() {
        MessageBundle ms = new MessageBundle().load("messages");
        ms.appendDefault("npe", null);
    }
}
