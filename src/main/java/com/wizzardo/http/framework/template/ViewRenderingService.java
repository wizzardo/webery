package com.wizzardo.http.framework.template;

import com.wizzardo.http.framework.ServerConfiguration;
import com.wizzardo.http.framework.di.DependencyFactory;
import com.wizzardo.http.framework.di.PostConstruct;
import com.wizzardo.http.framework.di.Service;
import com.wizzardo.tools.cache.Cache;
import com.wizzardo.tools.collections.Pair;
import com.wizzardo.tools.misc.With;
import com.wizzardo.tools.xml.GspParser;
import com.wizzardo.tools.xml.Node;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ViewRenderingService implements Service, PostConstruct {
    protected static final String OFFSET = "    ";
    protected static Pattern p = Pattern.compile("\\$\\{(.+)\\}|\\$([\\w]+)");

    protected Cache<Pair<String, String>, RenderableList> viewsCache;
    protected Cache<Pair<String, String>, RenderableList> templatesCache;
    protected ServerConfiguration configuration;
    protected ResourceTools resourceTools;

    @Override
    public void init() {
        ServerConfiguration.Renderer renderer = configuration.renderer;

        viewsCache = new Cache<>("views", renderer.viewCacheTtl, s -> prepareView(s.key, s.value));
        templatesCache = new Cache<>("templates", renderer.templateCacheTtl, s -> With.with(new RenderableList(), l -> prepare(s.key, l, null)));
    }

    protected RenderableList prepareView(String view, String offset) {
        String template = resourceTools.getResourceAsString(view);
        if (template == null)
            throw new IllegalArgumentException("view '" + view + "' not found");

        String dir = view.substring(0, view.lastIndexOf("/") + 1);

        RenderableList l = new RenderableList();
        Node html = new GspParser().parse(template);
        List<String> imports = null;

        Node page = html.get("%@");
        if (page != null && page.hasAttr("page")) {
            html.children().remove(page);

            if (page.hasAttr("import")) {
                imports = Arrays.asList(page.attr("import").split("; *"));
            }
        }

        Node layoutTag = html.get("html/head/meta[@name=layout]");
        if (layoutTag != null) {
            layoutTag.parent().children().remove(layoutTag);

            String layoutContent = resourceTools.getResourceAsString("views/layouts/" + layoutTag.attr("content") + ".gsp");
            if (layoutContent == null)
                throw new IllegalArgumentException("Layout '" + layoutTag.attr("content") + "' not found");

            Node layout = new GspParser().parse(layoutContent);
            for (Decorator decorator : DependencyFactory.get(DecoratorLib.class).list()) {
                decorator.decorate(html, layout);
            }
            html = layout;
        }

        prepare(html.children(), l, dir, offset, imports);
        return l;
    }

    public void prepare(String s, RenderableList l) {
        prepare(s, l, null);
    }

    public static void prepare(String s, RenderableList l, List<String> imports) {
        if (s.contains("$")) {
            Matcher m = p.matcher(s);
            int last = 0;
            while (m.find()) {
                l.append(s.substring(last, m.start()));
                String exp = m.group();
                l.add(new ExpressionHolder(exp, imports, true));
                last = m.end();
            }
            if (last != s.length()) {
                l.append(s.substring(last));
            }
        } else {
            l.append(s);
        }
    }

    public void prepare(List<Node> n, RenderableList l, String dir, String offset) {
        prepare(n, l, dir, offset, null);
    }

    public void prepare(List<Node> n, RenderableList l, String dir, String offset, List<String> imports) {
        for (Node node : n) {
            prepare(node, l, dir, offset, imports);
        }
    }

    public void prepare(Node n, RenderableList l, String dir, String offset, List<String> imports) {
        prepare(n, l, dir, offset, true, imports);
    }

    public void prepare(Node n, RenderableList l, String dir, String offset, boolean addNewLine, List<String> imports) {
        if (n.isComment() && n instanceof GspParser.GspComment)
            return;

        if (n.name() == null) {
            l.append(offset);
            prepare(n.textOwn(), l, imports);
            if (addNewLine)
                l.append("\n");
            return;
        }

        if (checkTagLib(n, l, dir, offset, imports)) {
            return;
        }

        l.append(offset);
        l.append("<").append(n.name());
        for (Map.Entry<String, String> attr : n.attributes().entrySet()) {
            l.append(" ");

            prepare(attr.getKey(), l, imports);

            String value = attr.getValue();
            if (value != null) {
                l.append("=\"");
                prepare(value, l, imports);
                l.append("\"");
            }
        }
        if (!n.isEmpty()) {
            l.append(">");
            if (addNewLine)
                l.append("\n");
            for (Node child : n.children()) {
                prepare(child, l, dir, offset + OFFSET, addNewLine, imports);
            }
            l.append(offset);
            l.append("</").append(n.name()).append(">");
        } else if (n.name().equalsIgnoreCase("div"))
            l.append("></").append(n.name()).append(">");
        else if (n.name().equalsIgnoreCase("!DOCTYPE"))
            l.append(">");
        else
            l.append("/>");

        if (addNewLine)
            l.append("\n");
    }


    public boolean checkTagLib(final Node n, RenderableList l, String dir, String offset, List<String> imports) {
        if (n.name().equals("g:render")) {
            String model = n.attr("model");
            if (model == null)
                model = "${[:]}";

            String template = n.attr("template");
            int i = template.lastIndexOf("/");
            String path = "";
            if (i > 0) {
                path = template.substring(0, i + 1);
                if (path.startsWith("/"))
                    path = ".." + path;

                template = template.substring(i + 1);
            }
            String collection = n.attr("collection");
            if (collection != null) {
                Node fakeEach = new Node("g:each")
                        .attr("in", n.attributes().remove("collection"))
                        .add(n);

                String var = n.attributes().remove("var");
                if (var == null)
                    var = "it";

                n.attr("model", "[" + var + ": it]");
                return checkTagLib(fakeEach, l, dir, offset, imports);
            } else {
                l.add(createRenderClosure(dir + path + "_" + template + ".gsp", model, offset));
            }
            return true;
        }

        if (TagLib.hasTag(n.name())) {
            Tag t = TagLib.createTag(n.name());
            if (t != null) {
                t.setImports(imports);
                t.init(n, offset, dir, this);
                t.appendTo(l);
                return true;
            }
            return false;
        }

        return false;
    }

    private Renderable createRenderClosure(final String pathToView, String params, final String offset) {
        AttributeVariableMapper<Map<String, Object>> p = new AttributeVariableMapper<>(params);
        RenderableList l = prepareView(pathToView, offset);
        return model -> l.get(p.map(model));
    }

    public RenderResult render(String controller, String view, Model model) {
        RenderableList l = viewsCache.get(new Pair<>(getViewPath(controller, view), ""));
        RenderResult result = l.get(model);
        return result;
    }

    public RenderResult render(String template, Model model) {
        RenderableList l = templatesCache.get(new Pair<>(template, ""));
        RenderResult result = l.get(model);
        return result;
    }

    public boolean hasView(String controller, String view) {
        return resourceTools.getResourceAsString(getViewPath(controller, view)) != null;
    }

    protected String getViewPath(String controller, String view) {
        return "views/" + controller + "/" + view + ".gsp";
    }
}
