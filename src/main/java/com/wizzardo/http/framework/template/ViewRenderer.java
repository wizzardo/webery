package com.wizzardo.http.framework.template;

import com.wizzardo.tools.cache.Cache;
import com.wizzardo.tools.collections.CollectionTools;
import com.wizzardo.tools.collections.Pair;
import com.wizzardo.tools.xml.Node;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: moxa
 * Date: 11/23/12
 */
public class ViewRenderer extends Renderer {

    private static ResourceTools resourceTools;

    static final String OFFSET = "    ";
    private String view;
    private String controller;
    private String template;

    public ViewRenderer(Model model, String controller, String view) {
        super(model);
        this.view = view;
        this.controller = controller;
    }

    public ViewRenderer(Model model, String template) {
        super(model);
        this.template = template;
    }

    public static void setResourceTools(ResourceTools resourceTools) {
        ViewRenderer.resourceTools = resourceTools;
    }

    @Override
    public RenderResult render() {
        if (template != null)
            return render(template, model);

        return render(controller, view, model);
    }

    private static Pattern p = Pattern.compile("\\$\\{([^\\{\\}]+)\\}|\\$([^\\., -]+)");
    //    private static Cache<Pair<String, String>, RenderableList> viewsCache = new Cache<Pair<String, String>, RenderableList>(60 * 60 * 24, 60, new Cache.Computable<Pair<String, String>, RenderableList>() {
    private static Cache<Pair<String, String>, RenderableList> viewsCache = new Cache<>(10, s -> {
        String template1 = resourceTools.getResourceAsString(s.key);
        if (template1 == null)
            throw new IllegalArgumentException("view '" + s.key + "' not found");

        String dir = s.key.substring(0, s.key.lastIndexOf("/") + 1);

        RenderableList l = new RenderableList();
        prepare(Node.parse(template1, true, true).children(), l, dir, s.value);
        return l;
    });

    private static Cache<Pair<String, String>, RenderableList> templatesCache = new Cache<>(60 * 60 * 24, s -> {
        RenderableList l = new RenderableList();
        prepare(s.key, l);
        return l;
    });

    public static void prepare(String s, RenderableList l) {
        if (s.contains("$")) {
            Matcher m = p.matcher(s);
            int last = 0;
            while (m.find()) {
                l.append(s.substring(last, m.start()));
                String exp = m.group(1);
                if (exp == null) {
                    exp = m.group(2);
                }
                l.add(new ExpressionHolder(exp));
                last = m.end();
            }
            if (last != s.length()) {
                l.append(s.substring(last));
            }
        } else {
            l.append(s);
        }
    }

    public static void prepare(List<Node> n, RenderableList l, String dir, String offset) {
        for (Node node : n) {
            prepare(node, l, dir, offset);
        }
    }

    public static void prepare(Node n, RenderableList l, String dir, String offset) {
        if (n.name() == null) {
            l.append(offset);
            prepare(n.textOwn(), l);
            l.append("\n");
            return;
        }

        if (checkTagLib(n, l, dir, offset)) {
            return;
        }

        l.append(offset);
        l.append("<").append(n.name());
        for (Map.Entry<String, String> attr : n.attributes().entrySet()) {
            l.append(" ");

            prepare(attr.getKey(), l);

            String value = attr.getValue();
            if (value != null) {
                l.append("=\"");
                prepare(value, l);
                l.append("\"");
            }
        }
        if (n.isEmpty()) {
            l.append("/>\n");
        } else {
            l.append(">\n");
            for (Node child : n.children()) {
                prepare(child, l, dir, offset + OFFSET);
            }
            l.append(offset);
            l.append("</").append(n.name()).append(">\n");
        }
    }


    public static boolean checkTagLib(final Node n, RenderableList l, String dir, String offset) {
        if (n.name().equals("g:render")) {

            String params = n.attr("params");
            if (params == null)
                params = "${[:]}";
            l.add(createRenderClosure(dir + "_" + n.attr("template"), params, offset));
            return true;
        }

        if (TagLib.hasTag(n.name())) {
            Tag t = TagLib.createTag(n.name(), n.attributes(), new Body(n, offset, dir), offset);
            if (t != null) {
                l.append(t);
                return true;
            }
            return false;
        }

        return false;
    }

    private static Renderable createRenderClosure(final String pathToView, String params, final String offset) {
        ExecutableTagHolder.InnerHolderHelper p = new ExecutableTagHolder.InnerHolderHelper(params);
        CollectionTools.Closure2<RenderResult, String, Map<String, Object>> c = (path, model) -> {
            List<Renderable> l = viewsCache.get(new Pair<>(path, offset));
            RenderResult result = new RenderResult();
            for (Renderable renderable : l) {
                result.append(renderable.get(model));
            }
            return result;
        };

        return new ExpressionHolder<Object>() {
            @Override
            public RenderResult get(Map<String, Object> model) {
                Map m = (Map) p.get(model);
                return c.execute(pathToView, m);
            }
        };
    }

    public static RenderResult render(String controller, String view, Model model) {
        String path = "views/" + controller + "/" + view + ".gsp";
        RenderableList l = viewsCache.get(new Pair<>(path, ""));
        RenderResult result = l.get(model);
        return result;
    }

    public static RenderResult render(String template, Model model) {
        RenderableList l = templatesCache.get(new Pair<>(template, ""));
        RenderResult result = l.get(model);
        return result;
    }
}
