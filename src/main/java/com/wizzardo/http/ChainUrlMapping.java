package com.wizzardo.http;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by wizzardo on 19.01.15.
 */
public class ChainUrlMapping<T> extends UrlMapping<Set<T>> {

    public ChainUrlMapping<T> add(String url, T t) {
        UrlMapping<Set<T>> tree = this;
        Set<T> set = new LinkedHashSet<>();
        String[] parts = url.split("/");
        int i;
        for (i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty())
                continue;

            if (part.contains("*"))
                part = part.replace("*", ".*");
            else if (part.contains("$"))
                part = convertRegexpVariables(part);

            UrlMapping<Set<T>> next = tree.find(part);
            if (next != null) {
                if (next.value != null)
                    set.addAll(next.value);
                tree = next;
            } else
                break;
        }
        set.add(t);
        append(url, set);

        return this;
    }
}
