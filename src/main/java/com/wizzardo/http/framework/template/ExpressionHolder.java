package com.wizzardo.http.framework.template;

import com.wizzardo.tools.evaluation.EvalTools;
import com.wizzardo.tools.evaluation.Expression;
import com.wizzardo.tools.misc.Unchecked;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: moxa
 * Date: 2/11/13
 */
public class ExpressionHolder implements Renderable {
    private volatile boolean prepared = false;
    private String string;
    private Expression expression;

    public ExpressionHolder(String s) {
        string = s;
    }

    protected ExpressionHolder() {
    }

    public RenderResult get(Map<String, Object> model) {
        return new RenderResult(String.valueOf(getRaw(model)));
    }

    private static Pattern p = Pattern.compile("\\$\\{([^\\{\\}]+)\\}|\\$([^\\., -]+)|(\\[.+\\])");

    public Object getRaw(Map<String, Object> model) {
        if (!prepared) {
            synchronized (this) {
                if (!prepared) {
                    Unchecked.run(() -> {
                        Matcher m = p.matcher(string);
                        if (m.find()) {
                            String exp = m.group(1);
                            if (exp == null) {
                                exp = m.group(2);
                            }
                            if (exp == null) {
                                exp = m.group(3);
                            }
                            expression = EvalTools.prepare(exp, model);
                        } else
                            expression = EvalTools.prepare(string, model);
                    });
                    prepared = true;
                }
            }
        }
        return Unchecked.call(() -> expression.get(model));
    }

    @Override
    public String toString() {
        return string;
    }
}