package com.wizzardo.http.framework.template;

import com.wizzardo.http.framework.RequestContext;
import com.wizzardo.tools.evaluation.EvalTools;
import com.wizzardo.tools.evaluation.Expression;
import com.wizzardo.tools.evaluation.Variable;
import com.wizzardo.tools.interfaces.Supplier;
import com.wizzardo.tools.misc.Unchecked;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: moxa
 * Date: 2/11/13
 */
public class ExpressionHolder<T> implements Renderable {
    private volatile boolean prepared = false;
    private String string;
    private Expression expression;
    protected boolean stringTemplate;
    protected List<String> imports;

    public ExpressionHolder(String s) {
        this(s, null, false);
    }

    public ExpressionHolder(String s, List<String> imports, boolean stringTemplate) {
        string = s;
        this.imports = imports;
        this.stringTemplate = stringTemplate;
    }

    protected ExpressionHolder() {
    }

    public RenderResult get(Map<String, Object> model) {
        return new RenderResult(String.valueOf(getRaw(model)));
    }

    private static Pattern p = Pattern.compile("\\$\\{([^\\{\\}]+)\\}|\\$([^\\., -]+)|(\\[.+\\])");

    public T raw(Map<String, Object> model) {
        return getRaw(model);
    }

    public T getRaw(Map<String, Object> model) {
        if (!prepared) {
            synchronized (this) {
                if (!prepared) {
                    Unchecked.run(() -> {
                        if (stringTemplate)
                            expression = EvalTools.prepare(string, model, TagLib.getTagFunctions(), imports, true);
                        else {
                            Matcher m = p.matcher(string);
                            if (m.matches()) {
                                String exp = m.group(1);
                                if (exp == null) {
                                    exp = m.group(2);
                                }
                                if (exp == null) {
                                    exp = m.group(3);
                                }
                                expression = EvalTools.prepare(exp, model, TagLib.getTagFunctions(), imports);
                            } else
                                expression = EvalTools.prepare(string, model, TagLib.getTagFunctions(), imports);
                        }
                        setVariables(expression);
                    });
                    prepared = true;
                }
            }
        }
        return Unchecked.call(() -> (T) expression.get(model));
    }

    @Override
    public String toString() {
        return string;
    }

    static void setVariables(Expression expression) {
        expression.setVariable(new ReadOnlyVariable<>("request", () -> RequestContext.get().getRequestHolder().request));
        expression.setVariable(new ReadOnlyVariable<>("response", () -> RequestContext.get().getRequestHolder().response));
        expression.setVariable(new ReadOnlyVariable<>("controller", () -> RequestContext.get().controller()));
        expression.setVariable(new ReadOnlyVariable<>("action", () -> RequestContext.get().action()));
        expression.setVariable(new ReadOnlyVariable<>("handler", () -> RequestContext.get().handler()));
    }

    static class ReadOnlyVariable<T> extends Variable {
        final Supplier<T> supplier;

        public ReadOnlyVariable(String name, Supplier<T> supplier) {
            super(name, null);
            this.supplier = supplier;
        }

        @Override
        public Object get() {
            return supplier.supply();
        }

        @Override
        public void set(Object o) {
            throw new UnsupportedOperationException("set");
        }

        @Override
        public String toString() {
            return getName() + ": " + get();
        }
    }
}