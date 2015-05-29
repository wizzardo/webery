package com.wizzardo.http.framework.template.taglib.g;

import com.wizzardo.http.framework.template.Body;
import com.wizzardo.http.framework.template.ExpressionHolder;
import com.wizzardo.http.framework.template.RenderResult;
import com.wizzardo.http.framework.template.Tag;

import java.util.Map;

/**
 * Created by wizzardo on 29.05.15.
 */
public class PasswordField extends TextField {

    @Override
    protected String getType() {
        return "password";
    }
}
