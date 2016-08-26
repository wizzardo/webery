package com.wizzardo.http.framework.template.taglib.g;

import com.wizzardo.http.Handler;
import com.wizzardo.http.framework.ControllerUrlMapping;
import com.wizzardo.http.framework.RequestContext;
import com.wizzardo.http.framework.di.DependencyFactory;
import com.wizzardo.http.framework.template.*;
import com.wizzardo.http.mapping.UrlTemplate;
import com.wizzardo.tools.evaluation.AsBooleanExpression;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by wizzardo on 13.06.15.
 */
public class CreateLink extends Tag implements RenderableString {

    protected ControllerUrlMapping urlMapping = DependencyFactory.get(ControllerUrlMapping.class);

    @Override
    public Tag init(Map<String, String> attrs, Body body, String offset) {
        String mapping = remove(attrs, "mapping", false);
        if (mapping == null) {
            String controller = attrs.remove("controller");
            if (controller == null)
                controller = ((RequestContext) Thread.currentThread()).controller();

            String action = attrs.remove("action");
            if (action == null)
                action = ((RequestContext) Thread.currentThread()).action();

            mapping = urlMapping.toMapping(controller, action);
        }

        ExpressionHolder base = asExpression(attrs, "base", true, false);
        ExpressionHolder fragment = asExpression(attrs, "fragment", true, false);
        ExpressionHolder absolute = asExpression(attrs, "absolute", false, false);
        ExpressionHolder suffix = asExpression(attrs, "path", true, false);

        append(offset);
        beforeAppendUrl(attrs, body, offset);

        ExpressionHolder<Map<String, Object>> params = asExpression(remove(attrs, "params", "[:]"), false);

        UrlTemplate template = urlMapping.getUrlTemplate(mapping);

        if (template == null)
            throw new IllegalStateException("can not find mapping for '" + mapping + "'");

        if (base != null) {
            add(model -> new RenderResult(template.getUrl(asString(base, model), params.getRaw(model), asString(suffix, model))));
        } else if (absolute != null)
            add(model -> {
                if (AsBooleanExpression.toBoolean(absolute.getRaw(model)))
                    return new RenderResult(template.getAbsoluteUrl(params.getRaw(model), asString(suffix, model)));
                else
                    return new RenderResult(template.getRelativeUrl(params.getRaw(model), asString(suffix, model)));
            });
        else
            add(model -> new RenderResult(template.getRelativeUrl(params.getRaw(model), asString(suffix, model))));

        if (fragment != null) {
            append("#").append(fragment);
        }

        afterAppendUrl(attrs, body, offset);
        append("\n");

        return this;
    }

    protected String asString(ExpressionHolder exp, Map<String, Object> model) {
        if (exp == null)
            return null;

        return String.valueOf(exp.getRaw(model));
    }

    protected void beforeAppendUrl(Map<String, String> attrs, Body body, String offset) {
    }

    protected void afterAppendUrl(Map<String, String> attrs, Body body, String offset) {
    }

    @Override
    public String render(Map<String, Object> attrs) {
        String mapping = (String) attrs.remove("mapping");
        if (mapping == null) {
            String controller = (String) attrs.remove("controller");
            if (controller == null)
                controller = ((RequestContext) Thread.currentThread()).controller();

            String action = (String) attrs.remove("action");
            if (action == null)
                action = ((RequestContext) Thread.currentThread()).action();

            mapping = urlMapping.toMapping(controller, action);
        }
        String base = (String) attrs.remove("base");
        String fragment = (String) attrs.remove("fragment");
        Boolean absolute = (Boolean) attrs.remove("absolute");
        String suffix = (String) attrs.remove("path");
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

        UrlTemplate template = getUrlTemplate(mapping);
        if (template == null)
            throw new IllegalStateException("can not find mapping for '" + mapping + "'");

        String url;

        if (base != null) {
            url = template.getUrl(base, params, suffix);
        } else if (absolute != null && absolute)
            url = template.getAbsoluteUrl(params, suffix);
        else
            url = template.getRelativeUrl(params, suffix);

        if (fragment != null && fragment.length() > 0)
            return url + "#" + fragment;


        return url;
    }

    protected UrlTemplate getUrlTemplate(String name) {
        return urlMapping.getUrlTemplate(name);
    }
}
