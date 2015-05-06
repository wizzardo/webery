package com.wizzardo.http.framework;

import com.wizzardo.http.Handler;
import com.wizzardo.http.mapping.UrlMapping;

/**
 * Created by wizzardo on 05.05.15.
 */
public class ControllerUrlMapping extends UrlMapping<Handler> {

    public ControllerUrlMapping() {
        super();
    }

    public ControllerUrlMapping(String context) {
        super(context);
    }

    public ControllerUrlMapping(String host, int port, String context) {
        super(host, port, context);
    }

    public ControllerUrlMapping append(String url, ControllerHandler handler) {
        super.append(url, handler.getControllerName() + "." + handler.getActionName(), handler);
        return this;
    }

    public ControllerUrlMapping append(String url, Class<? extends Controller> controllerClass, String action) {
        append(url, new ControllerHandler(controllerClass, action));
        return this;
    }

    @Override
    public ControllerUrlMapping append(String url, Handler handler) {
        super.append(url, handler);
        return this;
    }

    @Override
    public ControllerUrlMapping append(String url, String name, Handler handler) {
        super.append(url, name, handler);
        return this;
    }
}
