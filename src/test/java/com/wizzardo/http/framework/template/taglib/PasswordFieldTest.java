package com.wizzardo.http.framework.template.taglib;

import com.wizzardo.http.framework.template.Model;
import com.wizzardo.http.framework.template.RenderResult;
import com.wizzardo.http.framework.template.TagLib;
import com.wizzardo.http.framework.template.taglib.g.PasswordField;
import com.wizzardo.http.framework.template.taglib.g.TextField;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

/**
 * Created by wizzardo on 26.04.15.
 */
public class PasswordFieldTest extends TextFieldTest {

    @Before
    public void setup() {
        TagLib.findTags(Collections.singletonList(PasswordField.class));
    }

    protected String getType() {
        return "password";
    }
}
