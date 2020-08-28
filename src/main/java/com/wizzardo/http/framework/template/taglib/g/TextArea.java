package com.wizzardo.http.framework.template.taglib.g;

import com.wizzardo.http.framework.template.*;
import com.wizzardo.tools.xml.Node;

import java.util.Map;

/**
 * Created by wizzardo on 18.05.15.
 */
public class TextArea extends Tag {

    @Override
    public Tag init(Node node, String offset, String dir, boolean addNewLine, ViewRenderingService viewRenderingService) {
        return init(node.attributes(), new Body(node, "", dir, false, imports), offset);
    }

    public Tag init(Map<String, String> attrs, Body body, String offset) {
        ExpressionHolder name = asExpression(remove(attrs, "name"), true);

        String id = attrs.remove("id");
        String value = attrs.remove("value");

        append(offset);
        append("<textarea name=\"");
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
        prepareAttrs(attrs);
        append(">");

        if (value != null)
            append(asExpression(value, true));
        else
            append(body::get);

        append("</textarea>\n");
        return this;
    }
}
