package com.wizzardo.http.framework.template;

import com.wizzardo.tools.xml.Node;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public abstract class Tag extends RenderableList {

    protected List<String> imports;

    public Tag init(Node node, String offset, String dir, ViewRenderingService viewRenderingService) {
        return init(node.attributes(), new Body(node, getBodyOffset(offset), dir, imports, viewRenderingService), offset);
    }

    protected String getBodyOffset(String offset) {
        return offset + ViewRenderingService.OFFSET;
    }

    public Tag init(Map<String, String> attrs) {
        return init(attrs, null, "");
    }

    public Tag init(Map<String, String> attrs, Body body) {
        return init(attrs, body, "");
    }

    public abstract Tag init(Map<String, String> attrs, Body body, String offset);

    protected void prepareAttrs(Map<String, String> attrs) {
        for (Map.Entry<String, String> attr : attrs.entrySet()) {
            append(" ");
            ViewRenderingService.prepare(attr.getKey(), this, imports);
            append("=\"");
            ViewRenderingService.prepare(attr.getValue(), this, imports);
            append("\"");
        }
    }

    public void appendTo(RenderableList l) {
        l.append(this);
    }

    protected String remove(Map<String, String> attrs, String name) {
        return remove(attrs, name, true);
    }

    protected String remove(Map<String, String> attrs, String name, boolean mandatory) {
        String value = attrs.remove(name);
        if (value == null && mandatory)
            throw new IllegalStateException("variable '" + name + "' is mandatory");

        return value;
    }

    protected String remove(Map<String, String> attrs, String name, Supplier<String> defaultSupplier) {
        String value = attrs.remove(name);
        return value == null ? defaultSupplier.get() : value;
    }

    protected String remove(Map<String, String> attrs, String name, String def) {
        String value = attrs.remove(name);
        return value == null ? def : value;
    }

    protected <T> ExpressionHolder<T> asExpression(Map<String, String> attrs, String key, boolean template, boolean mandatory) {
        String s = remove(attrs, key, mandatory);
        if (s == null)
            return null;

        return new ExpressionHolder<>(s, imports, template);
    }

    protected <T> ExpressionHolder<T> asExpression(String value, boolean template) {
        return new ExpressionHolder<>(value, imports, template);
    }

    public void setImports(List<String> imports) {
        this.imports = imports;
    }
}
