package com.wizzardo.http.template;

import com.wizzardo.tools.misc.Unchecked;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wizzardo on 24.04.15.
 */
public class TagLib {

    private static Map<String, Constructor<Tag>> taglib = new HashMap<String, Constructor<Tag>>();

    public static void findTags(List<Class> l) {
        for (Class c : l) {
            if (Tag.class.isAssignableFrom(c) && c != Tag.class) {
                String namespace = c.getPackage().getName();
                namespace = namespace.substring(namespace.lastIndexOf('.') + 1);

                String tag = namespace + ":" + c.getSimpleName().substring(0, 1).toLowerCase() + c.getSimpleName().substring(1);
                System.out.println("register tag: " + tag);
                taglib.put(tag, c.getConstructors()[0]);
            }
        }
    }

    private static void registerTags(String clazzFile) {
        if (!clazzFile.contains("$") && clazzFile.endsWith(".class")) {
            clazzFile = clazzFile.substring(0, clazzFile.length() - 6).replace('/', '.');
            try {
                Class c = ClassLoader.getSystemClassLoader().loadClass(clazzFile);
                if (Tag.class.isAssignableFrom(c) && c != Tag.class) {
                    String namespace = c.getPackage().getName();
                    namespace = namespace.substring(namespace.lastIndexOf('.') + 1);

                    String tag = namespace + ":" + c.getSimpleName().substring(0, 1).toLowerCase() + c.getSimpleName().substring(1);
//                    System.out.println(tag);
                    taglib.put(tag, c.getConstructors()[0]);

                }
            } catch (ClassNotFoundException ignored) {
            }
        }
    }

    public static boolean hasTag(String name) {
        return taglib.containsKey(name);
    }

    public static Tag createTag(String name, Map<String, String> attrs, Body body) {
        Constructor<Tag> c = taglib.get(name);
        if (c == null)
            return null;

        return Unchecked.call(() -> c.newInstance(attrs, body));
    }

}
