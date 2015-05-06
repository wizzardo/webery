/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.wizzardo.http.framework;

import com.wizzardo.http.Session;
import com.wizzardo.http.framework.di.DependencyScope;
import com.wizzardo.http.framework.di.Injectable;
import com.wizzardo.http.framework.template.Model;
import com.wizzardo.http.framework.template.Renderer;
import com.wizzardo.http.framework.template.TextRenderer;
import com.wizzardo.http.framework.template.ViewRenderer;
import com.wizzardo.http.request.Parameters;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;
import com.wizzardo.http.response.Status;

/**
 * @author Moxa
 */
@Injectable(scope = DependencyScope.PROTOTYPE)
public abstract class Controller {

    protected Request request;
    protected Response response;
    protected String name;
    protected Model model;

    public Model model() {
        if (model == null)
            model = new Model();

        return model;
    }

    public Parameters params() {
        return request.params();
    }

    public Parameters getParams() {
        return request.params();
    }

    public Session session() {
        return request.session();
    }

    public Session getSession() {
        return request.session();
    }

    public Renderer renderView(String view) {
        return new ViewRenderer(model(), name(), view);
    }

    public Renderer renderTemplate(String template) {
        return new ViewRenderer(model(), template);
    }

    public Renderer renderString(String s) {
        return new TextRenderer(s);
    }

    public Renderer redirect(String url) {
        return redirect(url, Status._302);
    }

    public Renderer redirect(String url, Status status) {
        response.setStatus(status);
        response.setHeader("Location", url);
        return null;
    }

    public String name() {
        if (name != null)
            return name;

        name = getControllerName(getClass());
        return name;
    }

    static String getControllerName(Class<? extends Controller> clazz) {
        String controllerName = clazz.getSimpleName();
        if (controllerName.endsWith("Controller"))
            controllerName = controllerName.substring(0, controllerName.length() - "Controller".length());
        controllerName = controllerName.substring(0, 1).toLowerCase() + controllerName.substring(1);
        return controllerName;
    }


    public String getName() {
        return name();
    }
}
