package com.wizzardo.http;

import com.wizzardo.http.request.Request;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: wizzardo
 * Date: 25.09.14
 */
public class UrlMapping<T> {

    private static final Pattern VARIABLES = Pattern.compile("\\$\\{?([a-zA-Z_]+[\\w]*)\\}?");
    private static final String OPTIONAL = "([^/]+)?";

    protected Map<String, UrlMapping<T>> mapping = new HashMap<>();
    protected Map<String, UrlMappingMatcher<T>> regexpMapping = new LinkedHashMap<>();
    protected T value;
    protected UrlMapping<T> parent;

    public UrlMapping() {
    }

    protected UrlMapping(UrlMapping parent) {
        this.parent = parent;
    }

    protected boolean checkNextPart() {
        return true;
    }

    private static abstract class UrlMappingMatcher<T> extends UrlMapping<T> {
        protected UrlMappingMatcher(UrlMapping parent) {
            super(parent);
        }

        protected abstract boolean matches(String urlPart);
    }

    private static class UrlMappingMatcherAny<T> extends UrlMappingMatcher<T> {
        protected UrlMappingMatcherAny(UrlMapping parent) {
            super(parent);
        }

        @Override
        protected boolean matches(String urlPart) {
            return true;
        }

        @Override
        protected boolean checkNextPart() {
            return false;
        }

        @Override
        protected void setValue(T t) {
            super.setValue(t);
            if (parent != null)
                parent.setValue(t);
        }
    }

    private static class UrlMappingMatcherPattern<T> extends UrlMappingMatcher<T> {
        Pattern pattern;

        protected UrlMappingMatcherPattern(UrlMapping parent, String pattern) {
            super(parent);
            this.pattern = Pattern.compile(pattern);
        }

        @Override
        protected boolean matches(String urlPart) {
            return pattern.matcher(urlPart).matches();
        }

        @Override
        protected boolean checkNextPart() {
            return false;
        }
    }

    private static class UrlMappingWithVariables<T> extends UrlMappingMatcher<T> {
        String[] variables;
        Pattern pattern;
        int partNumber;

        UrlMappingWithVariables(UrlMapping parent, String part, int partNumber) {
            super(parent);
            this.partNumber = partNumber;
            Matcher m = VARIABLES.matcher(part);
            final List<String> vars = new ArrayList<>();
            while (m.find()) {
                vars.add(m.group(1));
            }
            part = part.replaceAll(VARIABLES.pattern(), "([^/]+)").replace("/([^/]+)?", "/?([^/]+)?");

            pattern = Pattern.compile(part);
            variables = vars.toArray(new String[vars.size()]);
        }

        @Override
        protected void prepare(Request request) {
            super.prepare(request);
            String part = request.path().getPart(partNumber);
            if (part == null)
                return;
            Matcher matcher = pattern.matcher(part);
            if (matcher.find()) {
                for (int i = 1; i <= variables.length; i++) {
                    request.param(variables[i - 1], matcher.group(i));
                }
            }
        }

        @Override
        protected boolean matches(String urlPart) {
            return pattern.matcher(urlPart).matches();
        }

        @Override
        protected void setValue(T t) {
            super.setValue(t);
            if (parent != null && pattern.pattern().equals(OPTIONAL))
                parent.setValue(t);
        }
    }

    protected void prepare(Request request) {
        if (parent != null)
            parent.prepare(request);
    }

    public T get(Request request) {
        UrlMapping<T> last = find(request.path());
        if (last != null && last.value != null)
            last.prepare(request);

        return last == null ? null : last.value;
    }

    private UrlMapping<T> find(Path path) {
        return find(path.parts());
    }

    public UrlMapping find(List<String> parts) {
        UrlMapping tree = this;
        for (int i = 0; i < parts.size() && tree != null; i++) {
            String part = parts.get(i);
            if (part.isEmpty())
                continue;

            tree = tree.find(part);
            if (tree != null && !tree.checkNextPart())
                break;
        }

        return tree;
    }

    private UrlMapping find(String part) {
        UrlMapping handler = mapping.get(part);
        if (handler != null)
            return handler;

        for (Map.Entry<String, UrlMappingMatcher<T>> entry : regexpMapping.entrySet()) {
            if (entry.getValue().matches(part))
                return entry.getValue();
        }
        return null;
    }

    public UrlMapping append(String url, T handler) {
        String[] parts = url.split("/");
        UrlMapping<T> tree = this;
        int counter = 0;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty())
                continue;

            UrlMapping<T> next;
            if (part.contains("*")) {
                String pattern = part.replace("*", ".*");
                next = tree.regexpMapping.get(pattern);
                if (next == null) {
                    UrlMappingMatcher<T> t;
                    if (pattern.equals(".*"))
                        t = new UrlMappingMatcherAny<>(tree);
                    else
                        t = new UrlMappingMatcherPattern<>(tree, pattern);
                    tree.regexpMapping.put(pattern, t);
                    next = t;
                }
            } else if (part.contains("$")) {
                String pattern = convertRegexpVariables(part);
                next = tree.regexpMapping.get(pattern);
                if (next == null) {
                    UrlMappingWithVariables<T> t = new UrlMappingWithVariables<>(tree, part, counter);
                    tree.regexpMapping.put(pattern, t);
                    next = t;
                }
            } else {
                next = tree.mapping.get(part);
                if (next == null) {
                    next = new UrlMapping<>(tree);
                    tree.mapping.put(part, next);
                }
            }

            tree = next;
            counter++;
        }
        tree.setValue(handler);

        return this;
    }

    protected void setValue(T t) {
        value = t;
    }

    private String convertRegexpVariables(String s) {
        return s.replaceAll(VARIABLES.pattern(), "([^/]+)").replace("/([^/]+)?", "/?([^/]+)?");
    }
}
