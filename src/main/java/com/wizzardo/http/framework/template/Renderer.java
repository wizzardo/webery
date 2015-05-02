package com.wizzardo.http.framework.template;


/**
 * @author: moxa
 * Date: 5/6/13
 */
public abstract class Renderer {

    protected Model model;

    public Renderer(Model model) {
        this.model = model;
    }

    public Renderer() {
    }

    public Model getModel() {
        return model;
    }

    public abstract RenderResult render();
}
