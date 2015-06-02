package com.wizzardo.http.framework.template.taglib.g;

import com.wizzardo.http.framework.di.DependencyFactory;
import com.wizzardo.http.framework.message.MessageSource;
import com.wizzardo.http.framework.template.*;
import com.wizzardo.tools.evaluation.AsBooleanExpression;

import java.util.Map;

/**
 * Created by wizzardo on 24.05.15.
 */
public class FormatBoolean extends Tag {

    protected MessageSource messageSource = DependencyFactory.getDependency(MessageSource.class);

    @Override
    public Tag init(Map<String, String> attrs, Body body, String offset) {
        ExpressionHolder raw = asExpression(attrs, "boolean", false, true);

        Renderable trueString = getValueString("true", attrs);
        Renderable falseString = getValueString("false", attrs);

        append(offset);
        append(model -> {
            Object value = raw.getRaw(model);
            if (AsBooleanExpression.toBoolean(value))
                return trueString.get(model);
            else
                return falseString.get(model);
        });
        append("\n");
        return this;
    }

    protected Renderable getValueString(String name, Map<String, String> attrs) {
        String value = attrs.remove(name);
        if (value != null)
            return asExpression(value, true);

        value = messageSource.get("boolean." + name);
        if (value != null)
            return Renderable.create(value);

        value = messageSource.get("default.boolean." + name);
        return Renderable.create(value);
    }
}
