package com.wizzardo.http;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Created by wizzardo on 19.01.15.
 */
public class ChainUrlMapping<T> extends UrlMapping<Set<T>> {

    public ChainUrlMapping<T> add(String url, T t) {
        UrlMapping<Set<T>> tree = this;
        String[] parts = url.split("/");

        int i;
        for (i = 0; i < parts.length && tree != null; i++) {
            String part = parts[i];
            if (part.isEmpty())
                continue;

            if (part.contains("*"))
                part = part.replace("*", ".*");
            else if (part.contains("$"))
                part = convertRegexpVariables(part);

            UrlMapping<Set<T>> next = tree.find(part);
            if (next != null && next.value != null && i == parts.length - 1)
                next.value.add(t);

            if (next == null && i == parts.length - 1)
                addToAll(part, tree, t);
            tree = next;
        }


        Set<T> set = new LinkedHashSet<>();
        tree = this;
        for (i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty())
                continue;

            if (part.contains("*"))
                part = part.replace("*", ".*");
            else if (part.contains("$"))
                part = convertRegexpVariables(part);

            addAll(part, tree, set);
            tree = tree.find(part);
            if (tree == null)
                break;
        }
        set.add(t);
        append(url, set);

        return this;
    }

    private void addAll(String part, UrlMapping<Set<T>> tree, Set<T> set) {
        UrlMapping<Set<T>> values = tree.mapping.get(part);
        if (values != null && values.value != null)
            set.addAll(values.value);

        for (Map.Entry<String, UrlMappingMatcher<Set<T>>> entry : regexpMapping.entrySet()) {
            if (entry.getValue().matches(part) && entry.getValue().value != null)
                set.addAll(entry.getValue().value);
        }
    }

    private void addToAll(UrlMapping<Set<T>> tree, T t) {
        if (tree.value != null)
            tree.value.add(t);

        for (Map.Entry<String, UrlMapping<Set<T>>> entry : tree.mapping.entrySet())
            addToAll(entry.getValue(), t);

        for (Map.Entry<String, UrlMappingMatcher<Set<T>>> entry : tree.regexpMapping.entrySet())
            addToAll(entry.getValue(), t);
    }

    private void addToAll(String pattern, UrlMapping<Set<T>> tree, T t) {
        Pattern p = Pattern.compile(pattern);

        for (Map.Entry<String, UrlMapping<Set<T>>> entry : tree.mapping.entrySet())
            if (p.matcher(entry.getKey()).matches())
                addToAll(entry.getValue(), t);

        for (Map.Entry<String, UrlMappingMatcher<Set<T>>> entry : tree.regexpMapping.entrySet())
            if (p.matcher(entry.getKey()).matches())
                addToAll(entry.getValue(), t);
    }
}
