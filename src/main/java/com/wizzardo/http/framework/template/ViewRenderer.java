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

    private static Pattern p = Pattern.compile("\\$\\{([^\\{\\}]+)\\}|\\$([^\\., -]+)");
    private static Cache<Pair<String, String>, RenderableList> viewsCache = new Cache<>(10, s -> prepareView(s.key, s.value));

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

    private static RenderableList prepareView(String view, String offset) {
        String template = resourceTools.getResourceAsString(view);
        if (template == null)
            throw new IllegalArgumentException("view '" + view + "' not found");

        String dir = view.substring(0, view.lastIndexOf("/") + 1);

        RenderableList l = new RenderableList();
        Node html = Node.parse(template, true, true);

        Node layoutTag = html.get("html/head/meta[@name=layout]");
        if (layoutTag != null) {
            layoutTag.parent().children().remove(layoutTag);

            Node layout = Node.parse(resourceTools.getResourceAsString("layouts/" + layoutTag.attr("content") + ".gsp"), true, true);
            for (Decorator decorator : DecoratorLib.decorators()) {
                decorator.decorate(html, layout);
            }
            html = layout;
        }

        prepare(html.children(), l, dir, offset);
        return l;
    }

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
        prepare(n, l, dir, offset, true);
    }

    public static void prepare(Node n, RenderableList l, String dir, String offset, boolean addNewLine) {
        if (n.name() == null) {
            l.append(offset);
            prepare(n.textOwn(), l);
            if (addNewLine)
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
            l.append("/>");
        } else {
            l.append(">");
            if (addNewLine)
                l.append("\n");
            for (Node child : n.children()) {
                prepare(child, l, dir, offset + OFFSET, addNewLine);
            }
            l.append(offset);
            l.append("</").append(n.name()).append(">");
        }
        if (addNewLine)
            l.append("\n");
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
            Tag t = TagLib.createTag(n.name());
            if (t != null) {
                t.init(n, offset, dir);
                t.appendTo(l);
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
