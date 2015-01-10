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

    private static Pattern VARIABLES = Pattern.compile("\\$\\{?([a-zA-Z_]+[\\w]*)\\}?");

    protected Map<String, UrlMapping<T>> mapping = new HashMap<>();
    protected Map<Pattern, UrlMapping<T>> regexpMapping = new LinkedHashMap<>();
    protected T value;
    protected UrlMapping<T> parent;

    protected UrlMapping(UrlMapping parent) {
        this.parent = parent;
    }

    private static class UrlMappingWithVariables extends UrlMapping {
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
    }

    public UrlMapping() {
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
        }
        while (tree != null && tree.value == null)
            tree = tree.find("");

        return tree;
    }

    private UrlMapping find(String part) {
        UrlMapping handler = mapping.get(part);
        if (handler != null)
            return handler;

        for (Map.Entry<Pattern, UrlMapping<T>> entry : regexpMapping.entrySet()) {
            if (entry.getKey().matcher(part).matches())
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
                Pattern pattern = Pattern.compile(part.replace("*", ".*"));
                next = tree.regexpMapping.get(pattern);
                if (next == null) {
                    next = new UrlMapping(tree);
                    tree.regexpMapping.put(pattern, next);
                }
            } else if (part.contains("$")) {
                Pattern pattern = Pattern.compile(convertRegexpVariables(part));
                next = tree.regexpMapping.get(pattern);
                if (next == null) {
                    next = new UrlMappingWithVariables(tree, part, counter);
                    tree.regexpMapping.put(pattern, next);
                }
            } else {
                next = tree.mapping.get(part);
                if (next == null) {
                    next = new UrlMapping(tree);
                    tree.mapping.put(part, next);
                }
            }

            tree = next;
            counter++;
        }
        tree.value = handler;

        return this;
    }

    private String convertRegexpVariables(String s) {
        return s.replaceAll(VARIABLES.pattern(), "([^/]+)").replace("/([^/]+)?", "/?([^/]+)?");
    }
}
