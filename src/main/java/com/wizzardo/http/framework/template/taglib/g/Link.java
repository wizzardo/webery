package com.wizzardo.http.framework.template.taglib.g;

import com.wizzardo.http.framework.template.Body;

import java.util.Map;

/**
 * @author: moxa
 * Date: 7/3/13
 */
public class Link extends CreateLink {

    @Override
    protected void beforeAppendUrl(Map<String, String> attrs, Body body, String offset) {
        append("<a href=\"");
    }

    @Override
    protected void afterAppendUrl(Map<String, String> attrs, Body body, String offset) {
        append("\"");
        prepareAttrs(attrs);
        if (body != null && !body.isEmpty()) {
            append(">\n").append(body).append(offset).append("</a>");
        } else {
            append("/>");
        }
    }
}
