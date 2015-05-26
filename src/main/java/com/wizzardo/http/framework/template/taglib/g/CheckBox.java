package com.wizzardo.http.framework.template.taglib.g;

import com.wizzardo.http.framework.template.Body;
import com.wizzardo.http.framework.template.ExpressionHolder;
import com.wizzardo.http.framework.template.RenderResult;
import com.wizzardo.http.framework.template.Tag;
import com.wizzardo.tools.evaluation.AsBooleanExpression;

import java.util.Map;

/**
 * Created by wizzardo on 26.05.15.
 */
public class CheckBox extends Tag {

    @Override
    public Tag init(Map<String, String> attrs, Body body, String offset) {
        ExpressionHolder name = new ExpressionHolder<>(remove(attrs, "name"), true);

        String id = attrs.remove("id");
        String value = attrs.remove("value");

        append(offset);
        append("<input type=\"checkbox\" name=\"");
        append(model -> {
            RenderResult result = new RenderResult();
            String nameString = String.valueOf(name.getRaw(model));
            result.append(nameString);
            if (id == null)
                result.append("\" id=\"").append(nameString);

            return result;
        });

        if (id != null)
            append("\" id=\"").append(new ExpressionHolder<>(id, true));

        append("\"");
        if (value != null)
            append(" value=\"").append(new ExpressionHolder<>(value, true)).append("\"");

        String ch;
        if ((ch = attrs.remove("checked")) != null) {
            if (ch.equalsIgnoreCase("true"))
                append(" checked=\"checked\"");
            else if (!ch.equalsIgnoreCase("false")) {
                ExpressionHolder checked = new ExpressionHolder<>(ch, true);
                append((model) -> new RenderResult(AsBooleanExpression.toBoolean(checked.getRaw(model)) ? " checked=\"checked\"" : ""));
            }
        }

        prepareAttrs(attrs);
        append("/>\n");
        return this;
    }
}
