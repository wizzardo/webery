package com.wizzardo.http.framework.template.taglib.g;

import com.wizzardo.http.framework.di.DependencyFactory;
import com.wizzardo.http.framework.message.MessageBundle;
import com.wizzardo.http.framework.message.MessageSource;
import com.wizzardo.http.framework.template.Body;
import com.wizzardo.http.framework.template.ExpressionHolder;
import com.wizzardo.http.framework.template.RenderResult;
import com.wizzardo.http.framework.template.Tag;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by wizzardo on 27.05.15.
 */
public class Message extends Tag {

    protected MessageBundle messageSource = DependencyFactory.getDependency(MessageBundle.class);

    @Override
    public Tag init(Map<String, String> attrs, Body body, String offset) {
        ExpressionHolder code = asExpression(attrs, "code", true, true);

        ExpressionHolder<List> args = asExpression(attrs, "args", false, false);
        ExpressionHolder<Locale> locale = asExpression(attrs, "locale", false, false);
        ExpressionHolder defaultMessage = asExpression(attrs, "default", true, false);

        append(offset);
        append(model -> {
            String stringCode = String.valueOf(code.getRaw(model));
            List l = args != null ? args.getRaw(model) : Collections.EMPTY_LIST;

            String result;
            if (locale == null)
                result = messageSource.get(stringCode, l);
            else
                result = messageSource.get(locale.getRaw(model), stringCode, l);

            if (result == null) {
                if (defaultMessage != null)
                    return defaultMessage.get(model);
                else
                    return new RenderResult("null");
            }

            return new RenderResult(result);
        });
        append("\n");
        return this;
    }
}
