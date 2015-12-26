package com.wizzardo.http.framework.template;

import com.wizzardo.http.framework.Environment;
import com.wizzardo.http.framework.Holders;
import com.wizzardo.tools.evaluation.Expression;
import com.wizzardo.tools.evaluation.UserFunction;
import com.wizzardo.tools.evaluation.Variable;
import com.wizzardo.tools.misc.Unchecked;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wizzardo on 24.04.15.
 */
public class TagLib {

    private static Map<String, Class<? extends Tag>> taglib = new HashMap<>();
    private static Map<String, UserFunction> tagFunctions = new HashMap<String, UserFunction>();

    public static void findTags(List<Class> l) {
        for (Class c : l) {
            if (Tag.class.isAssignableFrom(c) && !Modifier.isAbstract(c.getModifiers())) {
                String namespace = c.getPackage().getName();
                namespace = namespace.substring(namespace.lastIndexOf('.') + 1);

                String name = c.getSimpleName().substring(0, 1).toLowerCase() + c.getSimpleName().substring(1);
                String tag = namespace + ":" + name;
                if (Holders.getEnvironment() != Environment.TEST)
                    System.out.println("register tag: " + tag);

                taglib.put(tag, c);
                if (RenderableString.class.isAssignableFrom(c))
                    tagFunctions.put(name, new TagFunction(name, c));
            }
        }
    }

    static class TagFunction extends UserFunction {

        public TagFunction(String name, Class<? extends RenderableString> aClass) {
            super(name, new Expression() {
                @Override
                public void setVariable(Variable v) {
                }

                @Override
                public Expression clone() {
                    return this;
                }

                @Override
                public Object get(Map<String, Object> model) {
                    return Unchecked.call(() -> aClass.newInstance().render((Map<String, Object>) model.get("attrs")));
                }
            }, "attrs");
        }
    }

    public static boolean hasTag(String name) {
        return taglib.containsKey(name);
    }


    public static Map<String, UserFunction> getTagFunctions() {
        return tagFunctions;
    }

    public static Tag createTag(String name) {
        Class<? extends Tag> c = taglib.get(name);
        if (c == null)
            return null;

        return Unchecked.call(c::newInstance);
    }

}
