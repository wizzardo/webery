package com.wizzardo.http.framework.template.taglib.g;

import com.wizzardo.http.Handler;
import com.wizzardo.http.framework.di.DependencyFactory;
import com.wizzardo.http.framework.template.*;
import com.wizzardo.http.mapping.UrlMapping;
import com.wizzardo.http.mapping.UrlTemplate;

import java.util.Collections;
import java.util.Map;

/**
 * Created by wizzardo on 13.06.15.
 */
public class CreateLink extends Tag implements RenderableString {

    protected UrlMapping<Handler> urlMapping = DependencyFactory.getDependency(UrlMapping.class);

    @Override
    public Tag init(Map<String, String> attrs, Body body, String offset) {
        String controller = attrs.remove("controller");
        String action = attrs.remove("action");
        String base = attrs.remove("base");
        String fragment = attrs.remove("fragment");
        boolean absolute = Boolean.valueOf(attrs.remove("absolute"));

        append(offset);
        beforeAppendUrl();

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

        afterAppendUrl();
        append("\n");

        return this;
    }

    protected void beforeAppendUrl() {
    }

    protected void afterAppendUrl() {
    }

    @Override
    public String render(Map<String, Object> attrs) {
        String controller = (String) attrs.remove("controller");
        String action = (String) attrs.remove("action");
        String base = (String) attrs.remove("base");
        String fragment = (String) attrs.remove("fragment");
        boolean absolute = Boolean.valueOf((String) attrs.remove("absolute"));

        Map<String, Object> params = (Map) attrs.remove("params");
        if (params == null)
            params = Collections.emptyMap();

        UrlTemplate template = getUrlTemplate(controller, action);
        if (template == null)
            throw new IllegalStateException("can not find mapping for controller '" + controller + "' and action:'" + action + "'");

        String url;

        if (base != null) {
            url = template.getUrl(base, params);
        } else if (absolute)
            url = template.getAbsoluteUrl(params);
        else
            url = template.getRelativeUrl(params);

        if (fragment != null && fragment.length() > 0)
            return url + "#" + fragment;


        return url;
    }

    protected UrlTemplate getUrlTemplate(String name) {
        return urlMapping.getUrlTemplate(name);
    }

    protected UrlTemplate getUrlTemplate(String controller, String action) {
        return getUrlTemplate(controller + "." + action);
    }
}
