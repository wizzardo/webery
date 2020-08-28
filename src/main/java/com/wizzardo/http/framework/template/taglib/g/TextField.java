package com.wizzardo.http.framework.template.taglib.g;

import com.wizzardo.http.framework.template.Body;
import com.wizzardo.http.framework.template.ExpressionHolder;
import com.wizzardo.http.framework.template.RenderResult;
import com.wizzardo.http.framework.template.Tag;

import java.util.Map;

/**
 * Created by wizzardo on 17.05.15.
 */
public class TextField extends Tag {
    public Tag init(Map<String, String> attrs, Body body, String offset) {
        ExpressionHolder name = asExpression(remove(attrs, "name"), true);

        String id = attrs.remove("id");
        String value = attrs.remove("value");

        append(offset);
        append("<input type=\"").append(getType()).append("\" name=\"");
        append((model, result) -> {
            String nameString = String.valueOf(name.getRaw(model));
            result.append(nameString);
            if (id == null)
                result.append("\" id=\"").append(nameString);

            return result;
        });

        if (id != null)
            append("\" id=\"").append(asExpression(id, true));

        append("\"");
        if (value != null)
            append(" value=\"").append(asExpression(value, true)).append("\"");

        prepareAttrs(attrs);
        append("/>\n");
        return this;
    }

    protected String getType() {
        return "text";
    }
}
