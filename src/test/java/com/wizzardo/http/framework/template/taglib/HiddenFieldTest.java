package com.wizzardo.http.framework.template.taglib;

import com.wizzardo.http.framework.template.TagLib;
import com.wizzardo.http.framework.template.taglib.g.HiddenField;
import com.wizzardo.http.framework.template.taglib.g.PasswordField;
import org.junit.Before;

import java.util.Collections;

/**
 * Created by wizzardo on 26.04.15.
 */
public class HiddenFieldTest extends TextFieldTest {

    @Before
    public void setup() {
        TagLib.findTags(Collections.singletonList(HiddenField.class));
    }

    protected String getType() {
        return "hidden";
    }
}
