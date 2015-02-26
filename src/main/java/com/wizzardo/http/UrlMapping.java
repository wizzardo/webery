package com.wizzardo.http;

import com.wizzardo.http.request.ByteTree;
import com.wizzardo.http.request.Request;
import com.wizzardo.tools.misc.CharTree;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: wizzardo
 * Date: 25.09.14
 */
public class UrlMapping<T> {
    public static final ByteTree SEGMENT_CACHE = new ByteTree();

    private static final Pattern VARIABLES = Pattern.compile("\\$\\{?([a-zA-Z_]+[\\w]*)\\}?");
    private static final String OPTIONAL = "([^/]+)?";
    protected static final Pattern END = Pattern.compile("\\*([a-zA-Z_0-9\\.]+)");

    protected Map<String, UrlMapping<T>> mapping = new HashMap<>();
    protected Map<String, UrlMappingMatcher<T>> regexpMapping = new LinkedHashMap<>();
    protected T value;
    protected UrlMapping<T> parent;
    protected UrlMappingEndsWith<T> endsWithMapping;

    public UrlMapping() {
    }

    protected UrlMapping(UrlMapping parent) {
        this.parent = parent;
    }

    protected boolean checkNextPart() {
        return true;
    }

    protected static abstract class UrlMappingMatcher<T> extends UrlMapping<T> {
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
            if (request == null)
                return;

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

    protected static class UrlMappingEndsWith<T> extends UrlMapping<T> {
        protected CharTree<UrlMapping<T>> endsWith = new CharTree<>();

        protected UrlMappingEndsWith(UrlMapping parent) {
            super(parent);
        }

        UrlMapping<T> append(String pattern) {
            UrlMappingHolder<T> mapping = new UrlMappingHolder<>(this);
            pattern = pattern.substring(1);
            endsWith.appendReverse(pattern, mapping);
            return mapping;
        }

        @Override
        protected UrlMapping<T> find(String part, List<String> parts) {
            return endsWith.findEnds(parts.get(parts.size() - 1));
        }

        @Override
        protected UrlMapping<T> find(String part, String[] parts) {
            return endsWith.findEnds(parts[parts.length - 1]);
        }
    }

    protected static class UrlMappingHolder<T> extends UrlMapping<T> {
        protected UrlMappingHolder(UrlMapping parent) {
            super(parent);
        }

        @Override
        protected boolean checkNextPart() {
            return false;
        }
    }

    protected void prepare(Request request) {
        if (parent != null && request != null)
            parent.prepare(request);
    }

    public T get(Request request) {
        return get(request, request.path());
    }

    public T get(String path) {
        UrlMapping<T> last = find(Arrays.asList(path.split("/")));
        return last == null ? null : last.value;
    }

    public T get(Request request, Path path) {
        UrlMapping<T> last = find(path);
        if (last != null && last.value != null)
            last.prepare(request);

        return last == null ? null : last.value;
    }

    private UrlMapping<T> find(Path path) {
        return find(path.parts());
    }

    private UrlMapping<T> find(List<String> parts) {
        UrlMapping<T> tree = this;
        for (int i = 0; i < parts.size() && tree != null; i++) {
            String part = parts.get(i);
            if (part.isEmpty())
                continue;

            tree = tree.find(part, parts);
            if (tree != null && !tree.checkNextPart())
                break;
        }

        return tree;
    }

    protected UrlMapping<T> find(String part, List<String> parts) {
        UrlMapping<T> handler = findStatic(part);
        if (handler != null)
            return handler;

        handler = findDynamic(part);
        if (handler != null)
            return handler;

        return findEndsWith(parts);
    }

    protected UrlMapping<T> findStatic(String part) {
        return mapping.get(part);
    }

    protected UrlMapping<T> findDynamic(String part) {
        for (Map.Entry<String, UrlMappingMatcher<T>> entry : regexpMapping.entrySet()) {
            if (entry.getValue().matches(part))
                return entry.getValue();
        }
        return null;
    }

    protected UrlMapping<T> findEndsWith(List<String> parts) {
        return endsWithMapping != null ? endsWithMapping.find(null, parts) : null;
    }

    protected UrlMapping<T> findEndsWith(String[] parts) {
        return endsWithMapping != null ? endsWithMapping.find(null, parts) : null;
    }

    protected UrlMapping<T> find(String part, String[] parts) {
        UrlMapping<T> handler = findStatic(part);
        if (handler != null)
            return handler;

        handler = findDynamic(part);
        if (handler != null)
            return handler;

        return findEndsWith(parts);
    }

    public UrlMapping<T> append(String url, T handler) {
        String[] parts = url.split("/");
        UrlMapping<T> tree = this;
        int counter = 0;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty())
                continue;

            UrlMapping<T> next;
            if (part.contains("*")) {
                if (i == parts.length - 1 && END.matcher(part).matches()) {
                    part = part.substring(1);
                    if (tree.endsWithMapping == null)
                        tree.endsWithMapping = new UrlMappingEndsWith<>(tree);

                    next = tree.endsWithMapping.append(part);
                } else {
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
                SEGMENT_CACHE.append(part);
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

    protected String convertRegexpVariables(String s) {
        return s.replaceAll(VARIABLES.pattern(), "([^/]+)").replace("/([^/]+)?", "/?([^/]+)?");
    }
}
