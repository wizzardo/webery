package com.wizzardo.http.framework.template;

import com.wizzardo.tools.misc.Unchecked;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wizzardo on 24.04.15.
 */
public class TagLib {

    private static Map<String, Class<? extends Tag>> taglib = new HashMap<>();

    public static void findTags(List<Class> l) {
        for (Class c : l) {
            if (Tag.class.isAssignableFrom(c) && c != Tag.class) {
                String namespace = c.getPackage().getName();
                namespace = namespace.substring(namespace.lastIndexOf('.') + 1);

                String tag = namespace + ":" + c.getSimpleName().substring(0, 1).toLowerCase() + c.getSimpleName().substring(1);
                System.out.println("register tag: " + tag);
                taglib.put(tag, c);
            }
        }
    }

    public static boolean hasTag(String name) {
        return taglib.containsKey(name);
    }

    public static Tag createTag(String name) {
        Class<? extends Tag> c = taglib.get(name);
        if (c == null)
            return null;

        return Unchecked.call(c::newInstance);
    }

}
