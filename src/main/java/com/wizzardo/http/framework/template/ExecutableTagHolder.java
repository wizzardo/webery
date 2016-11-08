package com.wizzardo.http.framework.template;

import com.wizzardo.tools.collections.CollectionTools;
import com.wizzardo.tools.evaluation.EvalTools;
import com.wizzardo.tools.evaluation.Expression;
import com.wizzardo.tools.misc.Mapper;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: moxa
 * Date: 2/11/13
 */
public class ExecutableTagHolder implements Renderable {
    private CollectionTools.Closure2<RenderResult, Map<String, Object>, Body> closure;
    private Map<String, Mapper<Map<String, Object>, Object>> attrsRaw;
    private Body body;

    public ExecutableTagHolder(Body body, CollectionTools.Closure2<RenderResult, Map<String, Object>, Body> closure) {
        this.closure = closure;
        attrsRaw = new LinkedHashMap<>(body.attributes().size());
        this.body = body;
        for (Map.Entry<String, String> attr : body.attributes().entrySet()) {
            attrsRaw.put(attr.getKey(), new AttributeVariableMapper(attr.getValue()));
        }
    }

    public RenderResult get(Map<String, Object> model) {
        Map<String, Object> attrs = new HashMap<String, Object>(attrsRaw.size() + model.size());
        attrs.putAll(model);
        for (Map.Entry<String, Mapper<Map<String, Object>, Object>> attr : attrsRaw.entrySet()) {
            attrs.put(attr.getKey(), attr.getValue().map(model));
        }
        return closure.execute(attrs, body);
    }

    static class AttributeVariableMapper implements Mapper<Map<String, Object>, Object> {
        private String string;
        private volatile boolean prepared = false;
        private Expression expression;

        public AttributeVariableMapper(String string) {
            this.string = string;
        }

        @Override
        public Object map(Map<String, Object> model) {
            if (!prepared) {
                synchronized (this) {
                    if (!prepared) {
                        expression = EvalTools.prepare(prepare(string), model);
                        prepared = true;
                    }
                }
            }
            return expression.get(model);
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
}
