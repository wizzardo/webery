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

    public Tag init(Map<String, String> attrs, Body body, String offset) {
        ExpressionHolder name = asExpression(remove(attrs, "name"), true);
        ExpressionHolder value = asExpression(remove(attrs, "value"), true);

        String id = attrs.remove("id");

        append(offset);
        append("<input type=\"radio\" name=\"");
        append(model -> {
            RenderResult result = new RenderResult();
            String nameString = String.valueOf(name.getRaw(model));
            result.append(nameString);
            if (id == null)
                result.append("\" id=\"").append(nameString);

            return result;
        });

        if (id != null)
            append("\" id=\"").append(asExpression(id, true));

        append("\" value=\"");
        append(value);
        append("\"");

        String ch;
        if ((ch = attrs.remove("checked")) != null) {
            if (ch.equalsIgnoreCase("true"))
                append(" checked=\"checked\"");
            else if (!ch.equalsIgnoreCase("false")) {
                ExpressionHolder checked = asExpression(ch, false);
                append((model) -> new RenderResult(AsBooleanExpression.toBoolean(checked.getRaw(model)) ? " checked=\"checked\"" : ""));
            }
        }

        prepareAttrs(attrs);
        append("/>\n");
        return this;
    }
}
