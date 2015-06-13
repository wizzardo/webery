package com.wizzardo.http.framework.template.taglib.g;


import com.wizzardo.http.Handler;
import com.wizzardo.http.framework.di.DependencyFactory;
import com.wizzardo.http.framework.template.Body;
import com.wizzardo.http.framework.template.ExpressionHolder;
import com.wizzardo.http.framework.template.RenderResult;
import com.wizzardo.http.framework.template.Tag;
import com.wizzardo.http.mapping.UrlMapping;
import com.wizzardo.http.mapping.UrlTemplate;

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
