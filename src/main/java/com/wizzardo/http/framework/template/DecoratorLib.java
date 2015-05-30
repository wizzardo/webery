package com.wizzardo.http.framework.template;

import com.wizzardo.tools.misc.Unchecked;

import java.util.*;

/**
 * Created by wizzardo on 24.04.15.
 */
public class DecoratorLib {

    private static Set<Decorator> decorators = new LinkedHashSet<>();

    public static void findTags(List<Class> l) {
        for (Class c : l) {
            if (Decorator.class.isAssignableFrom(c) && c != Decorator.class) {
                decorators.add(Unchecked.call(() -> (Decorator) c.newInstance()));
            }
        }
    }

    public static Collection<Decorator> decorators() {
        return decorators;
    }
}
