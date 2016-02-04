package com.wizzardo.http.mapping;

import com.wizzardo.http.Named;
import com.wizzardo.http.request.ByteTree;
import com.wizzardo.http.request.Request;

import java.util.*;
import java.util.regex.Pattern;

/**
 * @author: wizzardo
 * Date: 25.09.14
 */
public class UrlMapping<T extends Named> {
    public static final ByteTree SEGMENT_CACHE = new ByteTree();

    static final Pattern VARIABLES = Pattern.compile("\\$\\{?([a-zA-Z_]+[\\w]*)\\}?");
    static final String OPTIONAL = "(.+)?";
    protected static final Pattern END = Pattern.compile("\\*([a-zA-Z_0-9\\.]+)");

    protected Map<String, UrlMapping<T>> mapping = new HashMap<>();
    protected Map<String, UrlMappingMatcher<T>> regexpMapping = new LinkedHashMap<>();
    protected T value;
    protected UrlMapping<T> parent;
    protected UrlMappingEndsWith<T> endsWithMapping;
    protected TemplatesHolder<String> urlTemplates = new TemplatesHolder<>();
    protected String context;

    public UrlMapping() {
    }

    public UrlMapping(String context) {
        this.context = context;
    }

    public UrlMapping(String host, int port, String context) {
        this.context = context;
        urlTemplates = new TemplatesHolder<>(host, port, context);
    }

    protected UrlMapping(UrlMapping<T> parent) {
        this.parent = parent;
    }

    protected boolean checkNextPart() {
        return true;
    }

    protected void prepare(Request request) {
        if (parent != null && request != null)
            parent.prepare(request);
    }

    public TemplatesHolder<String> getTemplatesHolder() {
        return urlTemplates;
    }

    public UrlTemplate getUrlTemplate(String name) {
        if (urlTemplates == null)
            return null;

        return urlTemplates.getTemplate(name);
    }

    public T get(Request request) {
        return get(request, request.path());
    }

    public T get(String path) {
        UrlMapping<T> last = find(Arrays.asList(path.split("/")));
        return last == null ? null : last.value;
    }

    public T get(Request request, Path path) {
        if (context != null) {
            if (path.length() == 0 || !path.getPart(0).equals(context))
                return null;

            path = path.subPath(1);
        }

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
        return append(url, null, handler);
    }

    public UrlMapping<T> append(String url, String name, T handler) {
        if (name == null)
            name = handler.name();

        if (name != null)
            urlTemplates.append(name, url);

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
                    UrlMappingWithVariables<T> t;
                    if (pattern.equals("(.+)") || pattern.equals("(.+)?"))
                        t = new UrlMappingMatcherAnyVariable<>(tree, part, counter);
                    else
                        t = new UrlMappingWithVariables<>(tree, part, counter);
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
        return s.replaceAll(VARIABLES.pattern(), "(.+)");
    }

    public void setContext(String context) {
        this.context = context;
        urlTemplates.setContext(context);
    }
}
