package com.wizzardo.http.framework;

import com.wizzardo.epoll.readable.ReadableData;
import com.wizzardo.http.Handler;
import com.wizzardo.http.framework.di.DependencyFactory;
import com.wizzardo.http.framework.template.Renderer;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;
import com.wizzardo.tools.collections.CollectionTools;
import com.wizzardo.tools.misc.Unchecked;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Created by wizzardo on 02.05.15.
 */
public class ControllerHandler<T extends Controller> implements Handler {

    protected Class<T> controller;
    protected String controllerName;
    protected String actionName;
    protected CollectionTools.Closure<ReadableData, T> renderer;

    public ControllerHandler(Class<T> controller, String action) {
        this.controller = controller;
        controllerName = Controller.getControllerName(controller);
        actionName = action;

        Method actionMethod = findAction(controller, action);
        renderer = it -> {
            Renderer renderer = Unchecked.call(() -> (Renderer) actionMethod.invoke(it));
            return renderer != null ? renderer.renderReadableData() : null;
        };
    }

    public ControllerHandler(Class<T> controller, String action, CollectionTools.Closure<ReadableData, T> renderer) {
        this.controller = controller;
        this.renderer = renderer;
        controllerName = Controller.getControllerName(controller);
        actionName = action;
    }

    @Override
    public String name() {
        return getControllerName() + "." + getActionName();
    }

    @Override
    public Response handle(Request request, Response response) throws IOException {
//        request.controller(controllerName);
//        request.action(actionName);

        RequestContext context = (RequestContext) Thread.currentThread();
        context.setRequestHolder(new RequestHolder(request, response));
        context.setController(controllerName);
        context.setAction(actionName);

        T c = DependencyFactory.get(controller);
        c.request = request;
        c.response = response;

        ReadableData data = renderer.execute(c);
        if (data != null)
            response.setBody(data);

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
        return actionName;
    }

    public String getControllerName() {
        return controllerName;
    }
}
