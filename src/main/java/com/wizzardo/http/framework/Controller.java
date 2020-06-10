/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.wizzardo.http.framework;

import com.wizzardo.epoll.readable.ReadableByteArray;
import com.wizzardo.epoll.readable.ReadableData;
import com.wizzardo.http.Session;
import com.wizzardo.http.framework.di.DependencyScope;
import com.wizzardo.http.framework.di.Injectable;
import com.wizzardo.http.framework.template.*;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Parameters;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.JsonResponseHelper;
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

    public Renderer renderView() {
        return renderView(RequestContext.get().action());
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

    public Renderer renderData(ReadableData data) {
        return new ReadableDataRenderer(data);
    }

    public Renderer renderData(byte[] data) {
        return renderData(new ReadableByteArray(data));
    }

    public Renderer renderJson(Object o) {
        response.appendHeader(Header.KV_CONTENT_TYPE_APPLICATION_JSON);
        return new ReadableDataRenderer(JsonResponseHelper.renderJson(o));
    }

    public Renderer renderJson(String s) {
        response.appendHeader(Header.KV_CONTENT_TYPE_APPLICATION_JSON);
        return renderString(s);
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

    public static String getControllerName(Class clazz) {
        String controllerName = clazz.getSimpleName();
        if (controllerName.endsWith("Controller"))
            controllerName = controllerName.substring(0, controllerName.length() - "Controller".length());
        int i = 0;
        while (i < controllerName.length() && controllerName.charAt(i) >= 'A' && controllerName.charAt(i) <= 'Z') {
            i++;
        }

        controllerName = controllerName.substring(0, i).toLowerCase() + controllerName.substring(i);
        return controllerName;
    }


    public String getName() {
        return name();
    }
}
