package com.wizzardo.http.framework.template;

import com.wizzardo.http.framework.di.DependencyFactory;

public class ViewRenderer extends Renderer {

    private String view;
    private String controller;
    private String template;
    private ViewRenderingService renderingService = DependencyFactory.get(ViewRenderingService.class);

    public ViewRenderer(Model model, String controller, String view) {
        super(model);
        this.view = view;
        this.controller = controller;
    }

    public ViewRenderer(Model model, String template) {
        super(model);
        this.template = template;
    }

    @Override
    public RenderResult render() {
        if (template != null)
            return renderingService.render(template, model);
        else
            return renderingService.render(controller, view, model);
    }
}
