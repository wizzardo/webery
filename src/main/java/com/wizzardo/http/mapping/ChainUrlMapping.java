package com.wizzardo.http.mapping;

import com.wizzardo.http.Named;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by wizzardo on 19.01.15.
 */
public class ChainUrlMapping<T> extends UrlMapping<ChainUrlMapping.Chain<T>> {

    public ChainUrlMapping() {
        super();
    }

    public ChainUrlMapping(String context) {
        super(context);
    }

    public ChainUrlMapping<T> add(String url, T t) {
        UrlMapping<Chain<T>> tree = this;
        String[] parts = url.split("/");

        int i;
        for (i = 0; i < parts.length && tree != null; i++) {
            String part = parts[i];
            if (part.isEmpty())
                continue;

            if (part.contains("*")) {
                if (i == parts.length - 1 && END.matcher(part).matches())
                    break;

                part = part.replace("*", ".*");
            } else if (part.contains("$"))
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

            if (part.contains("*")) {
                if (i == parts.length - 1 && END.matcher(part).matches()) {
                    if (tree.endsWithMapping == null)
                        break;
                    part = part.substring(1);
                    for (UrlMapping<Chain<T>> chainUrlMapping : tree.endsWithMapping.endsWith.findAllEnds(part)) {
                        chain.chain.addAll(chainUrlMapping.value.chain);
                    }
                    break;
                }
                part = part.replace("*", ".*");
            } else if (part.contains("$"))
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

    protected List<UrlMapping<Chain<T>>> findAllEndsWith(List<String> parts) {
        return endsWithMapping != null ? endsWithMapping.endsWith.findAllEnds(parts.get(parts.size() - 1)) : null;
    }

    @Override
    protected UrlMapping<Chain<T>> find(Path path) {
        return find(path.parts());
    }

    protected UrlMapping<Chain<T>> find(List<String> parts) {
        UrlMapping<Chain<T>> tree = this;
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

    protected UrlMapping<Chain<T>> find(String part, List<String> parts) {
        List<UrlMapping<Chain<T>>> endsWith = findAllEndsWith(parts);

        UrlMapping<Chain<T>> mapping = findStatic(part);
        if (mapping == null)
            mapping = findDynamic(part);

        if (endsWith == null)
            return mapping;

        UrlMapping<Chain<T>> parent = null;
        if (mapping != null)
            parent = mapping.parent;

        UrlMapping<Chain<T>> doubleMapping = new UrlMappingHolder<>(parent);
        doubleMapping.value = new Chain<>();
        if (mapping != null)
            doubleMapping.value.chain.addAll(mapping.value.chain);

        for (UrlMapping<Chain<T>> chainUrlMapping : endsWith) {
            doubleMapping.value.chain.addAll(chainUrlMapping.value.chain);
        }

        return doubleMapping;
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

    public static class Chain<T> implements Iterable<T>, Named {
        Set<T> chain = new LinkedHashSet<>();

        @Override
        public Iterator<T> iterator() {
            return chain.iterator();
        }
    }
}
