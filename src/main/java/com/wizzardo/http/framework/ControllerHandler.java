package com.wizzardo.http.framework;

import com.wizzardo.epoll.readable.ReadableBuilder;
import com.wizzardo.http.Handler;
import com.wizzardo.http.framework.di.DependencyFactory;
import com.wizzardo.http.framework.template.RenderResult;
import com.wizzardo.http.framework.template.Renderer;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;
import com.wizzardo.tools.misc.Unchecked;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Created by wizzardo on 02.05.15.
 */
public class ControllerHandler implements Handler {

    protected Class<? extends Controller> controller;
    protected Method action;
    protected String controllerName;
    protected String actionName;

    public ControllerHandler(Class<? extends Controller> controller, String action) {
        this.controller = controller;
        this.action = findAction(controller, action);
        controllerName = Controller.getControllerName(controller);
        actionName = action;
    }


    @Override
    public Response handle(Request request, Response response) throws IOException {
//        request.controller(controllerName);
//        request.action(actionName);

        WebWorker webWorker = (WebWorker) Thread.currentThread();
        webWorker.requestHolder = new RequestHolder(request, response);
        webWorker.controller = controllerName;
        webWorker.action = actionName;

        Controller c = DependencyFactory.getDependency(controller);
        c.request = request;
        c.response = response;

        RenderResult render = Unchecked.call(() -> (Renderer) action.invoke(c)).render();

        ReadableBuilder builder = new ReadableBuilder();
        render.provideBytes(builder::append);
        response.setBody(builder);

        return response;
    }

    protected Method findAction(Class clazz, String action) {
        for (Method method : clazz.getDeclaredMethods()) {
            if ((method.getModifiers() & Modifier.STATIC) == 0 && method.getName().equals(action)) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new IllegalStateException("Can't find action '" + action + "'");
    }

    public String getActionName() {
        return action.getName();
    }

    public String getControllerName() {
        return controllerName;
    }
}
