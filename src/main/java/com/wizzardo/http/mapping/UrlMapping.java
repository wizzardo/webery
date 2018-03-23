package com.wizzardo.http.mapping;

import com.wizzardo.http.Named;
import com.wizzardo.http.request.ByteTree;
import com.wizzardo.http.request.Request;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * @author: wizzardo
 * Date: 25.09.14
 */
public class UrlMapping<T extends Named> {
    public static final ByteTree SEGMENT_CACHE = new ByteTree();

    static final Pattern VARIABLES = Pattern.compile("\\$\\{?([a-zA-Z_]+[\\w]*)\\}?");
    static final String OPTIONAL = "(.+)?";
    protected static final Pattern END = Pattern.compile("\\*([a-zA-Z_0-9\\.\\-]+)");

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

    protected void prepare(BiConsumer<String, String> parameterConsumer, Path path) {
        if (parent != null)
            parent.prepare(parameterConsumer, path);
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
        UrlMapping<T> last = findRecursive(Arrays.asList(path.split("/")), 0);
        return last == null ? null : last.value;
    }

    public T get(Request request, Path path) {
        if (request != null)
            return get((BiConsumer<String, String>) request::param, path);
        else
            return get((BiConsumer<String, String>) null, path);
    }

    public T get(BiConsumer<String, String> parameterConsumer, Path path) {
        path = adjustPath(path);
        if (path == null)
            return null;

        UrlMapping<T> last = find(path);
        T result = last == null ? null : last.value;
        if (result != null && parameterConsumer != null)
            last.prepare(parameterConsumer, path);

        return result;
    }

    public void each(BiConsumer<String, String> parameterConsumer, Path path, Consumer<T> consumer) {
        path = adjustPath(path);
        if (path == null)
            return;

        Path finalPath = path;
        findRecursive(path.parts(), 0, mapping -> {
            mapping.prepare(parameterConsumer, finalPath);
            consumer.accept(mapping.value);
            return true;
        });
    }

    public T get(Function<T, BiConsumer<String, String>> resultToParameterConsumer, Path path) {
        path = adjustPath(path);
        if (path == null)
            return null;

        UrlMapping<T> last = find(path);
        T result = last == null ? null : last.value;
        if (result != null && resultToParameterConsumer != null)
            last.prepare(resultToParameterConsumer.apply(result), path);

        return result;
    }

    public void each(Function<T, BiConsumer<String, String>> resultToParameterConsumer, Path path, Consumer<T> consumer) {
        path = adjustPath(path);
        if (path == null)
            return;

        Path finalPath = path;
        findRecursive(path.parts(), 0, mapping -> {
            mapping.prepare(resultToParameterConsumer.apply(mapping.value), finalPath);
            consumer.accept(mapping.value);
            return true;
        });
    }

    protected Path adjustPath(Path path) {
        if (context != null) {
            if (path.length() == 0 || !path.getPart(0).equals(context))
                return null;

            return path.subPath(1);
        }
        return path;
    }

    protected UrlMapping<T> find(Path path) {
        return findRecursive(path.parts(), 0);
    }

    protected UrlMapping<T> findRecursive(List<String> parts, int index) {
        if (index == parts.size())
            return this.value != null ? this : null;

        String part = parts.get(index);
        if (part.isEmpty())
            return findRecursive(parts, index + 1);

        UrlMapping<T> tree = findStatic(part);
        if (tree != null) {
            if (!tree.checkNextPart()) {
                return tree;
            } else {
                tree = tree.findRecursive(parts, index + 1);
                if (tree != null)
                    return tree;
            }
        }

        for (Map.Entry<String, UrlMappingMatcher<T>> entry : regexpMapping.entrySet()) {
            if (entry.getValue().matches(part)) {
                tree = entry.getValue();
                if (!tree.checkNextPart()) {
                    return tree;
                } else {
                    tree = tree.findRecursive(parts, index + 1);
                    if (tree != null)
                        return tree;
                }
            }
        }

        tree = findEndsWith(parts);
        if (tree != null) {
            if (!tree.checkNextPart()) {
                return tree;
            } else {
                tree = tree.findRecursive(parts, index + 1);
                if (tree != null)
                    return tree;
            }
        }

        return null;
    }

    public int size() {
        return mapping.size() + regexpMapping.size() + (endsWithMapping != null ? endsWithMapping.size() : 0);
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    protected boolean findRecursive(List<String> parts, int index, Predicate<UrlMapping<T>> consumer) {
        if (index == parts.size() && value != null) {
            return consumer.test(this);
        }

        String part = parts.get(index);
        if (part.isEmpty())
            findRecursive(parts, index + 1, consumer);

        UrlMapping<T> tree = findStatic(part);
        if (tree != null) {
            if (!tree.checkNextPart()) {
                if (!consumer.test(tree))
                    return false;
            } else {
                tree = tree.findRecursive(parts, index + 1);
                if (tree != null && !consumer.test(tree))
                    return false;
            }
        }

        for (Map.Entry<String, UrlMappingMatcher<T>> entry : regexpMapping.entrySet()) {
            if (entry.getValue().matches(part)) {
                tree = entry.getValue();
                if (!tree.checkNextPart()) {
                    if (!consumer.test(tree))
                        return false;
                } else {
                    tree = tree.findRecursive(parts, index + 1);
                    if (tree != null && !consumer.test(tree))
                        return false;
                }
            }
        }

        tree = findEndsWith(parts);
        if (tree != null) {
            if (!tree.checkNextPart()) {
                if (!consumer.test(tree))
                    return false;
            } else {
                tree = tree.findRecursive(parts, index + 1);
                if (tree != null && !consumer.test(tree))
                    return false;
            }
        }
        return true;
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
                    if (part.equals("*")) {
                        next = tree.regexpMapping.get(".*");
                        if (next == null) {
                            UrlMappingMatcher<T> t = new UrlMappingMatcherAny<>(tree);
                            tree.regexpMapping.put(".*", t);
                            next = t;
                        }
                    } else {
                        next = tree.regexpMapping.get(part);
                        if (next == null) {
                            UrlMappingMatcher<T> t = new UrlMappingMatcherPattern<>(tree, part);
                            tree.regexpMapping.put(part, t);
                            next = t;
                        }

                    }
                }
            } else if (part.contains("$")) {
                String pattern = convertRegexpVariables(part);
                next = tree.regexpMapping.get(part);
                if (next == null) {
                    UrlMappingWithVariables<T> t;
                    if (pattern.equals("(.+)") || pattern.equals("(.+)?"))
                        t = new UrlMappingMatcherAnyVariable<>(tree, part, counter);
                    else
                        t = new UrlMappingWithVariables<>(tree, part, counter);
                    tree.regexpMapping.put(part, t);
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
