package com.wizzardo.http;

import com.wizzardo.http.request.Request;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Created by wizzardo on 19.01.15.
 */
public class ChainUrlMapping<T> extends UrlMapping<ChainUrlMapping.Chain<T>> {

    public ChainUrlMapping<T> add(String url, T t) {
        UrlMapping<Chain<T>> tree = this;
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

            UrlMapping<Chain<T>> next = tree.find(part, parts);
            if (next != null && next.value != null && i == parts.length - 1)
                next.value.chain.add(t);

            if (next == null && i == parts.length - 1)
                addToAll(part, tree, t);
            tree = next;
        }


        Chain<T> chain = new Chain<>();
        tree = this;
        for (i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty())
                continue;

            if (part.contains("*"))
                part = part.replace("*", ".*");
            else if (part.contains("$"))
                part = convertRegexpVariables(part);

            addAll(part, tree, chain);
            tree = tree.find(part, parts);
            if (tree == null)
                break;
        }
        chain.chain.add(t);
        append(url, chain);

        return this;
    }

    @Override
    public Chain<T> get(Request request, Path path) {
        return super.get(request, path);
    }

    private void addAll(String part, UrlMapping<Chain<T>> tree, Chain<T> set) {
        UrlMapping<Chain<T>> values = tree.mapping.get(part);
        if (values != null && values.value != null)
            set.chain.addAll(values.value.chain);

        for (Map.Entry<String, UrlMappingMatcher<Chain<T>>> entry : regexpMapping.entrySet()) {
            if (entry.getValue().matches(part) && entry.getValue().value != null)
                set.chain.addAll(entry.getValue().value.chain);
        }
    }

    private void addToAll(UrlMapping<Chain<T>> tree, T t) {
        if (tree.value != null)
            tree.value.chain.add(t);

        for (Map.Entry<String, UrlMapping<Chain<T>>> entry : tree.mapping.entrySet())
            addToAll(entry.getValue(), t);

        for (Map.Entry<String, UrlMappingMatcher<Chain<T>>> entry : tree.regexpMapping.entrySet())
            addToAll(entry.getValue(), t);
    }

    private void addToAll(String pattern, UrlMapping<Chain<T>> tree, T t) {
        Pattern p = Pattern.compile(pattern);

        for (Map.Entry<String, UrlMapping<Chain<T>>> entry : tree.mapping.entrySet())
            if (p.matcher(entry.getKey()).matches())
                addToAll(entry.getValue(), t);

        for (Map.Entry<String, UrlMappingMatcher<Chain<T>>> entry : tree.regexpMapping.entrySet())
            if (p.matcher(entry.getKey()).matches())
                addToAll(entry.getValue(), t);
    }

    public static class Chain<T> implements Iterable<T> {
        Set<T> chain = new LinkedHashSet<>();

        @Override
        public Iterator<T> iterator() {
            return chain.iterator();
        }
    }
}
