package com.wizzardo.http.framework.template.taglib.g;

import com.wizzardo.http.framework.template.Body;
import com.wizzardo.http.framework.template.ExpressionHolder;
import com.wizzardo.http.framework.template.RenderResult;
import com.wizzardo.http.framework.template.Tag;
import com.wizzardo.tools.xml.Node;

import java.util.Map;

/**
 * Created by wizzardo on 18.05.15.
 */
public class TextArea extends Tag {

    @Override
    public Tag init(Node node, String offset, String dir) {
        return init(node.attributes(), new Body(node, "", dir, false), offset);
    }

    public Tag init(Map<String, String> attrs, Body body, String offset) {
        ExpressionHolder name = new ExpressionHolder<>(remove(attrs, "name"), true);

        String id = attrs.remove("id");
        String value = attrs.remove("value");

        append(offset);
        append("<textarea name=\"");
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
        prepareAttrs(attrs);
        append(">");

        if (value != null)
            append(new ExpressionHolder<>(value, true));
        else
            append(body::get);

        append("</textarea>\n");
        return this;
    }
}
