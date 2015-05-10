package com.wizzardo.http.framework.template.taglib;

import com.wizzardo.http.framework.template.*;
import com.wizzardo.http.framework.template.taglib.g.Each;
import com.wizzardo.http.framework.template.taglib.g.Set;
import com.wizzardo.tools.xml.Node;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/**
 * Created by wizzardo on 26.04.15.
 */
public class SetTest {

    @Before
    public void setup() {
        TagLib.findTags(Collections.singletonList(Set.class));
    }

    @Test
    public void test_1() {
        Node n = Node.parse("<div><g:set value=\"${1}\" var=\"i\"/>$i</div>", true);

        RenderableList l = new RenderableList();
        ViewRenderer.prepare(n.children(), l, "", "");

        RenderResult result = l.get(new Model());

        Assert.assertEquals("" +
                "<div>\n" +
                "    1\n" +
                "</div>\n", result.toString());
    }

}
