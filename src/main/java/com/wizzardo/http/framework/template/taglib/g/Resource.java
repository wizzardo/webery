package com.wizzardo.http.framework.template.taglib.g;

import com.wizzardo.http.FileTreeHandler;
import com.wizzardo.http.framework.di.DependencyFactory;
import com.wizzardo.http.framework.template.*;
import com.wizzardo.http.mapping.UrlMapping;
import com.wizzardo.http.mapping.UrlTemplate;
import com.wizzardo.tools.evaluation.AsBooleanExpression;

import java.util.Map;

/**
 * Created by wizzardo on 27.06.15.
 */
public class Resource extends Tag implements RenderableString {

    protected UrlMapping urlMapping = DependencyFactory.get(UrlMapping.class);
    protected FileTreeHandler fileTreeHandler = DependencyFactory.get(FileTreeHandler.class);

    @Override
    public Tag init(Map<String, String> attrs, Body body, String offset) {
        ExpressionHolder absolute = asExpression(attrs, "absolute", false, false);
        ExpressionHolder dir = asExpression(attrs, "dir", true, false);
        ExpressionHolder file = asExpression(attrs, "file", true, true);

        String fileName = file.toString();
        boolean isStatic = !fileName.contains("$");

        UrlTemplate template = urlMapping.getUrlTemplate("resources");

        append(offset);

        if (isStatic)
            if (fileName.endsWith(".js"))
                append("<script type=\"text/javascript\" src=\"");
            else if (fileName.endsWith(".css"))
                append("<link rel=\"stylesheet\" href=\"");

        append(model -> {
            String f = String.valueOf(file.raw(model));
            StringBuilder path = new StringBuilder();
            if (dir == null)
                path.append('/').append(f);
            else {
                String d = String.valueOf(dir.raw(model));
                if(!d.startsWith("/"))
                    path.append('/');

                if (d.endsWith("/"))
                    path.append(d).append(f);
                else
                    path.append(d).append('/').append(f);
            }

            String p = fileTreeHandler.getVersionedPath(path.toString());
            String url;
            if (absolute != null && AsBooleanExpression.toBoolean(absolute.raw(model)))
                url = template.getAbsoluteUrl(p);
            else
                url = template.getRelativeUrl(p);

            if (isStatic)
                return new RenderResult(url);

            if (f.endsWith(".js"))
                return new RenderResult()
                        .append("<script type=\"text/javascript\" src=\"")
                        .append(url)
                        .append("\"></script>");
            else if (f.endsWith(".css"))
                return new RenderResult()
                        .append("<link rel=\"stylesheet\" href=\"")
                        .append(url)
                        .append("\">");

            return new RenderResult();
        });

        if (isStatic)
            if (fileName.endsWith(".js"))
                append("\"></script>");
            else if (fileName.endsWith(".css"))
                append("\">");

        append("\n");

        return this;
    }

    @Override
    public String render(Map<String, Object> attrs) {
        String dir = (String) attrs.remove("dir");
        String file = (String) attrs.remove("file");
        Boolean absolute = (Boolean) attrs.remove("absolute");

        StringBuilder path = new StringBuilder();
        if (dir == null)
            path.append("/").append(file);
        else {
            if (!dir.startsWith("/"))
                path.append('/');

            if (dir.endsWith("/"))
                path.append(dir).append(file);
            else
                path.append(dir).append('/').append(file);
        }

        UrlTemplate template = urlMapping.getUrlTemplate("resources");

        String p = fileTreeHandler.getVersionedPath(path.toString());
        if (absolute != null && absolute)
            return template.getAbsoluteUrl(p);
        else
            return template.getRelativeUrl(p);
    }
}
