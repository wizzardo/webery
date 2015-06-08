package com.wizzardo.http.framework.template.taglib;

import com.wizzardo.http.framework.WebApplicationTest;
import com.wizzardo.http.framework.message.MessageBundle;
import com.wizzardo.http.framework.template.*;
import com.wizzardo.http.framework.template.taglib.g.FormatBoolean;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

/**
 * Created by wizzardo on 24.05.15.
 */
public class FormatBooleanTest extends WebApplicationTest implements TagTest {

    @Before
    public void setup() {
        TagLib.findTags(Collections.singletonList(FormatBoolean.class));
    }

    @Override
    protected MessageBundle initMessageSource(MessageBundle bundle) {
        return bundle.load("messages");
    }

    @Test
    public void test_1() {
        RenderResult result = prepare("<div><g:formatBoolean boolean=\"${myBoolean}\"/></div>")
                .get(new Model().append("myBoolean", false));

        Assert.assertEquals("" +
                "<div>\n" +
                "    false\n" +
                "</div>\n", result.toString());
    }

    @Test
    public void test_2() {
        RenderableList l = prepare("<g:formatBoolean boolean=\"${i > 0}\" true=\"$i > 0\" false=\"$i <= 0\"/>");
        Model model = new Model();

        model.put("i", 1);
        Assert.assertEquals("1 > 0\n", l.get(model).toString());

        model.put("i", -1);
        Assert.assertEquals("-1 <= 0\n", l.get(model).toString());
    }

    @Test
    public void test_3() {
        RenderResult result = prepare("<div><g:formatBoolean boolean=\"${myBoolean}\"/></div>")
                .get(new Model().append("myBoolean", true));

        Assert.assertEquals("" +
                "<div>\n" +
                "    ok\n" +
                "</div>\n", result.toString());
    }
}
