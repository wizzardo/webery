package com.wizzardo.http.framework.template.taglib.g;

import com.wizzardo.http.framework.template.Body;
import com.wizzardo.http.framework.template.ExpressionHolder;
import com.wizzardo.http.framework.template.RenderResult;
import com.wizzardo.http.framework.template.Tag;
import com.wizzardo.tools.evaluation.AsBooleanExpression;

import java.util.Map;

/**
 * Created by wizzardo on 12.05.15.
 */
public class Radio extends Tag {
    public Radio(Map<String, String> attrs, Body body, String offset) {
        super(attrs, body, offset);

        ExpressionHolder name = new ExpressionHolder<>(check(attrs, "name"), true);
        ExpressionHolder value = new ExpressionHolder<>(check(attrs, "value"), true);

        append(offset);
        append("<input type=\"radio\" name=\"");
        append(name);
        append("\" value=\"");
        append(value);
        append("\"");

        String ch;
        if ((ch = attrs.get("checked")) != null) {
            if (ch.equalsIgnoreCase("true"))
                append(" checked=\"checked\"");
            else if (!ch.equalsIgnoreCase("false")) {
                ExpressionHolder checked = new ExpressionHolder<>(ch, true);
                append((model) -> new RenderResult(AsBooleanExpression.toBoolean(checked.getRaw(model)) ? " checked=\"checked\"" : ""));
            }
        }

        append("/>");
    }
}
