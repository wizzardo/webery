package com.wizzardo.http.framework.template.taglib.g;

import com.wizzardo.http.Handler;
import com.wizzardo.http.framework.WebWorker;
import com.wizzardo.http.framework.di.DependencyFactory;
import com.wizzardo.http.framework.template.*;
import com.wizzardo.http.mapping.UrlMapping;
import com.wizzardo.http.mapping.UrlTemplate;
import com.wizzardo.tools.evaluation.AsBooleanExpression;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by wizzardo on 13.06.15.
 */
public class CreateLink extends Tag implements RenderableString {

    protected UrlMapping<Handler> urlMapping = DependencyFactory.getDependency(UrlMapping.class);

    @Override
    public Tag init(Map<String, String> attrs, Body body, String offset) {
        String controller = attrs.remove("controller");
        if (controller == null)
            controller = ((WebWorker) Thread.currentThread()).controller();

        String action = attrs.remove("action");
        if (action == null)
            action = ((WebWorker) Thread.currentThread()).action();

        ExpressionHolder base = asExpression(attrs, "base", true, false);
        ExpressionHolder fragment = asExpression(attrs, "fragment", true, false);
        ExpressionHolder absolute = asExpression(attrs, "absolute", false, false);

        append(offset);
        beforeAppendUrl(attrs, body, offset);

        ExpressionHolder<Map<String, Object>> params = asExpression(remove(attrs, "params", "[:]"), false);

        UrlTemplate template = urlMapping.getUrlTemplate(controller + "." + action);

        if (template == null)
            throw new IllegalStateException("can not find mapping for controller '" + controller + "' and action:'" + action + "'");

        if (base != null) {
            add(model -> new RenderResult(template.getUrl(String.valueOf(base.getRaw(model)), params.getRaw(model))));
        } else if (absolute != null)
            add(model -> {
                if (AsBooleanExpression.toBoolean(absolute.getRaw(model)))
                    return new RenderResult(template.getAbsoluteUrl(params.getRaw(model)));
                else
                    return new RenderResult(template.getRelativeUrl(params.getRaw(model)));
            });
        else
            add(model -> new RenderResult(template.getRelativeUrl(params.getRaw(model))));

        if (fragment != null) {
            append("#").append(fragment);
        }

        afterAppendUrl(attrs, body, offset);
        append("\n");

        return this;
    }

    protected void beforeAppendUrl(Map<String, String> attrs, Body body, String offset) {
    }

    protected void afterAppendUrl(Map<String, String> attrs, Body body, String offset) {
    }

    @Override
    public String render(Map<String, Object> attrs) {
        String controller = (String) attrs.remove("controller");
        if (controller == null)
            controller = ((WebWorker) Thread.currentThread()).controller();

        String action = (String) attrs.remove("action");
        if (action == null)
            action = ((WebWorker) Thread.currentThread()).action();

        String base = (String) attrs.remove("base");
        String fragment = (String) attrs.remove("fragment");
        Boolean absolute = (Boolean) attrs.remove("absolute");
        Object id = attrs.remove("id");

        Map<String, Object> params = (Map) attrs.remove("params");
        if (params == null) {
            if (id == null) {
                params = Collections.emptyMap();
            } else {
                params = new HashMap<>();
                params.put("id", id);
            }
        } else if (id != null) {
            params.put("id", id);
        }

        UrlTemplate template = getUrlTemplate(controller, action);
        if (template == null)
            throw new IllegalStateException("can not find mapping for controller '" + controller + "' and action:'" + action + "'");

        String url;

        if (base != null) {
            url = template.getUrl(base, params);
        } else if (absolute != null && absolute)
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
