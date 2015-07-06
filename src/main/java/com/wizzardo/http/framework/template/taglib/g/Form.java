package com.wizzardo.http.framework.template.taglib.g;

import com.wizzardo.http.framework.template.Body;
import com.wizzardo.http.framework.template.Tag;

import java.util.Map;

/**
 * Created by wizzardo on 02.07.15.
 */
public class Form extends CreateLink {

    @Override
    protected void beforeAppendUrl(Map<String, String> attrs, Body body, String offset) {
        append("<form action=\"");
    }

    @Override
    protected void afterAppendUrl(Map<String, String> attrs, Body body, String offset) {
        append("\"");
        prepareAttrs(attrs);

        if (!attrs.containsKey("method"))
            append(" method=\"POST\"");

        if (body != null && !body.isEmpty()) {
            append(">\n").append(body).append(offset).append("</form>");
        } else {
            append("></form>");
        }
    }

    @Override
    public String render(Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }
}
