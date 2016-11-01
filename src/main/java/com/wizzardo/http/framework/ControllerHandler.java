package com.wizzardo.http.framework;

import com.wizzardo.epoll.readable.ReadableData;
import com.wizzardo.http.Handler;
import com.wizzardo.http.MultipartHandler;
import com.wizzardo.http.framework.di.DependencyFactory;
import com.wizzardo.http.framework.parameters.ParametersHelper;
import com.wizzardo.http.framework.template.Renderer;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;
import com.wizzardo.http.response.Status;
import com.wizzardo.tools.collections.CollectionTools;
import com.wizzardo.tools.json.JsonTools;
import com.wizzardo.tools.misc.Mapper;
import com.wizzardo.tools.misc.Unchecked;

import java.io.IOException;
import java.lang.reflect.*;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * Created by wizzardo on 02.05.15.
 */
public class ControllerHandler<T extends Controller> implements Handler {

    protected static final Set<Class> PARSABLE_TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            String.class,
            Integer.class,
            Long.class,
            Short.class,
            Byte.class,
            Float.class,
            Double.class,
            Boolean.class,
            Character.class
    )));

    protected Class<T> controller;
    protected String controllerName;
    protected String actionName;
    protected CollectionTools.Closure<ReadableData, T> renderer;

    public ControllerHandler(Class<T> controller, String action) {
        this.controller = controller;
        controllerName = Controller.getControllerName(controller);
        actionName = action;
        renderer = createRenderer(controller, action);
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
        if (request.isMultipart() && !request.isMultiPartDataPrepared()) {
            return new MultipartHandler(this).handle(request, response);
        }

//        request.controller(controllerName);
//        request.action(actionName);

        RequestContext context = (RequestContext) Thread.currentThread();
        context.setController(controllerName);
        context.setAction(actionName);

        T c = DependencyFactory.get(controller);
        c.request = request;
        c.response = response;

        return doHandle(response, c);
    }

    protected Response doHandle(Response response, T c) {
        ReadableData data = renderer.execute(c);
        if (data != null)
            response.setBody(data);

        return response;
    }

    protected CollectionTools.Closure<ReadableData, T> createRenderer(Class<T> controller, String action) {
        Method actionMethod = findAction(controller, action);
        return createRenderer(actionMethod);
    }

    protected CollectionTools.Closure<ReadableData, T> createRenderer(Method method) {
        if (method.getParameterCount() == 0) {
            return it -> {
                Renderer renderer = Unchecked.call(() -> (Renderer) method.invoke(it));
                return renderer != null ? renderer.renderReadableData() : null;
            };
        } else {
            Parameter[] parameters = method.getParameters();
            for (Parameter parameter : parameters) {
                if (parameter.isNamePresent())
                    continue;
                if (parameter.isAnnotationPresent(com.wizzardo.http.framework.parameters.Parameter.class))
                    continue;

                Class<?> type = parameter.getType();
                if (type.isPrimitive() || type.isEnum() || PARSABLE_TYPES.contains(type))
                    throw new IllegalStateException("Can't parse parameters for '" + controllerName + "." + actionName + "', parameters names are not present. Please run javac with '-parameters' or add an annotation Parameter");
            }

            Mapper<Request, Object>[] argsMappers = new Mapper[parameters.length];
            Type[] types = method.getGenericParameterTypes();
            for (int i = 0; i < parameters.length; i++) {
                try {
                    argsMappers[i] = ParametersHelper.createParametersMapper(parameters[i], types[i]);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Can't create collection mapper ' in '" + controllerName + "." + actionName + "'", e);
                }
            }

            return it -> {
                Mapper<Request, Object>[] mappers = argsMappers;
                Object[] args = new Object[mappers.length];
                Request request = it.request;
                Exceptions exceptions = null;
                for (int i = 0; i < mappers.length; i++) {
                    try {
                        args[i] = mappers[i].map(request);
                    } catch (Exception e) {
                        if (exceptions == null)
                            exceptions = new Exceptions(mappers.length);

//                        e.printStackTrace();
                        exceptions.add(e.getClass().getCanonicalName() + ": " + e.getMessage());
                    }
                }

                if (exceptions != null) {
                    it.response.setBody(JsonTools.serialize(exceptions)).status(Status._400);
                    return null;
                }


                Renderer renderer = Unchecked.call(() -> (Renderer) method.invoke(it, args));
                return renderer != null ? renderer.renderReadableData() : null;
            };
        }
    }

    static class Exceptions {
        List<String> messages;

        Exceptions(int initSize) {
            messages = new ArrayList<>(initSize);
        }

        public void add(String message) {
            messages.add(message);
        }
    }

    protected Method findAction(Class clazz, String action) {
        for (Method method : clazz.getDeclaredMethods()) {
            if ((method.getModifiers() & Modifier.STATIC) == 0 && method.getName().equals(action)) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new IllegalStateException("Can't find action '" + action + "' in class '" + clazz + "'");
    }

    public String getActionName() {
        return actionName;
    }

    public String getControllerName() {
        return controllerName;
    }
}
