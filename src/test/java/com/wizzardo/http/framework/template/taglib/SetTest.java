package com.wizzardo.http.framework.template.taglib;

import com.wizzardo.http.framework.template.*;
import com.wizzardo.http.framework.template.taglib.g.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

/**
 * Created by wizzardo on 26.04.15.
 */
public class SetTest implements TagTest {

    @Before
    public void setup() {
        TagLib.findTags(Collections.singletonList(Set.class));
    }

    @Test
    public void test_1() {
        RenderResult result = prepare("<div><g:set value=\"${1}\" var=\"i\"/>$i</div>").get(new Model());

        Assert.assertEquals("" +
                "<div>\n" +
                "    1\n" +
                "</div>\n", result.toString());
    }

    @Test
    public void test_2() {
        RenderResult result = prepare("<div><g:set value=\"${1}\" var=\"i\"/>${i.class.simpleName}</div>").get(new Model());

        Assert.assertEquals("" +
                "<div>\n" +
                "    Integer\n" +
                "</div>\n", result.toString());
    }

}
