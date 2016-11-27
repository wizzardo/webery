package com.wizzardo.http.framework.template;

import com.wizzardo.tools.evaluation.EvalTools;
import com.wizzardo.tools.evaluation.Expression;
import com.wizzardo.tools.misc.Mapper;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wizzardo on 08/11/16.
 */
class AttributeVariableMapper<T> implements Mapper<Map<String, Object>, T> {
    private Expression expression;

    public AttributeVariableMapper(String string) {
        expression = EvalTools.prepare(prepare(string));
        ExpressionHolder.setVariables(expression);
    }

    @Override
    public T map(Map<String, Object> model) {
        return (T) expression.get(model);
    }

    private static Pattern p = Pattern.compile("\\$\\{([^\\{\\}]+)\\}|\\$([^\\., -]+)|(\\[.+\\])");

    private String prepare(String model) {
        Matcher m = p.matcher(model);
        if (m.matches()) {
            String exp = m.group(1);
            if (exp == null)
                exp = m.group(2);
            if (exp == null)
                exp = m.group(3);
            return exp;
        }
        throw new IllegalArgumentException("Cannot parse '" + model + "' as model attribute");
    }
}
