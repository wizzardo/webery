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
public class Link extends Tag {

    protected UrlMapping<Handler> urlMapping = DependencyFactory.getDependency(UrlMapping.class);

    public Tag init(Map<String, String> attrs, Body body, String offset) {
        String controller = attrs.remove("controller");
        String action = attrs.remove("action");
        String base = attrs.remove("base");
        String fragment = attrs.remove("fragment");
        boolean absolute = Boolean.valueOf(attrs.remove("absolute"));

        append(offset);
        append("<a href=\"");

        ExpressionHolder<Map<String, Object>> params = asExpression(remove(attrs, "params", "[:]"), false);

        UrlTemplate template = urlMapping.getUrlTemplate(controller + "." + action);

        if (template == null)
            throw new IllegalStateException("can not find mapping for controller '" + controller + "' and action:'" + action + "'");

        if (base != null) {
            add(model -> new RenderResult(template.getUrl(base, params.getRaw(model))));
        } else if (absolute)
            add(model -> new RenderResult(template.getAbsoluteUrl(params.getRaw(model))));
        else
            add(model -> new RenderResult(template.getRelativeUrl(params.getRaw(model))));

        if (fragment != null && fragment.length() > 0) {
            append("#").append(fragment);
        }

        append("\"");
        prepareAttrs(attrs);
        if (body != null && !body.isEmpty()) {
            append(">\n").append(body).append(offset).append("</a>\n");
        } else {
            append("/>\n");
        }

        return this;
    }
}
