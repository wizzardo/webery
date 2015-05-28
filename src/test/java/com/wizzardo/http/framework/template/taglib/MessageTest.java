package com.wizzardo.http.framework.template.taglib;

import com.wizzardo.http.framework.WebApplicationTest;
import com.wizzardo.http.framework.message.MessageBundle;
import com.wizzardo.http.framework.template.Model;
import com.wizzardo.http.framework.template.RenderResult;
import com.wizzardo.http.framework.template.RenderableList;
import com.wizzardo.http.framework.template.TagLib;
import com.wizzardo.http.framework.template.taglib.g.FormatBoolean;
import com.wizzardo.http.framework.template.taglib.g.Message;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

/**
 * Created by wizzardo on 24.05.15.
 */
public class MessageTest extends WebApplicationTest implements TagTest {

    @Before
    public void setup() {
        TagLib.findTags(Collections.singletonList(Message.class));
    }

    @Override
    protected MessageBundle initMessageSource(MessageBundle bundle) {
        return bundle.load("messages");
    }

    @Test
    public void test_1() {
        RenderResult result = prepare("<div><g:message code=\"test.message.${0}.args\"/></div>")
                .get(new Model());

        Assert.assertEquals("" +
                "<div>\n" +
                "    test message zero args\n" +
                "</div>\n", result.toString());
    }

    @Test
    public void test_2() {
        RenderResult result = prepare("<div><g:message code=\"test.message.${1}.args\" args=\"['one']\"/></div>")
                .get(new Model());

        Assert.assertEquals("" +
                "<div>\n" +
                "    test message: one\n" +
                "</div>\n", result.toString());
    }

    @Test
    public void test_3() {
        RenderResult result = prepare("<div><g:message code=\"test.message.${'default'}.args\" default=\"${'default message'}\"/></div>")
                .get(new Model());

        Assert.assertEquals("" +
                "<div>\n" +
                "    default message\n" +
                "</div>\n", result.toString());
    }
}
