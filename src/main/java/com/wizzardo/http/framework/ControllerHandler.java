package com.wizzardo.http.framework;

import com.wizzardo.epoll.readable.ReadableData;
import com.wizzardo.http.Handler;
import com.wizzardo.http.MultiValue;
import com.wizzardo.http.framework.di.DependencyFactory;
import com.wizzardo.http.framework.template.Renderer;
import com.wizzardo.http.request.Parameters;
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
//        request.controller(controllerName);
//        request.action(actionName);

        RequestContext context = (RequestContext) Thread.currentThread();
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
                if (parameter.isAnnotationPresent(com.wizzardo.http.framework.Parameter.class))
                    continue;

                Class<?> type = parameter.getType();
                if (type.isPrimitive() || type.isEnum() || PARSABLE_TYPES.contains(type))
                    throw new IllegalStateException("Can't parse parameters for '" + controllerName + "." + actionName + "', parameters names are not present. Please run javac with '-parameters' or add an annotation Parameter");
            }

            Mapper<Parameters, Object>[] argsMappers = new Mapper[parameters.length];
            Type[] types = method.getGenericParameterTypes();
            for (int i = 0; i < parameters.length; i++) {
                argsMappers[i] = createParametersMapper(parameters[i], types[i]);
            }

            return it -> {
                Mapper<Parameters, Object>[] mappers = argsMappers;
                Object[] args = new Object[mappers.length];
                Parameters params = it.getParams();
                Exceptions exceptions = null;
                for (int i = 0; i < mappers.length; i++) {
                    try {
                        args[i] = mappers[i].map(params);
                    } catch (Exception e) {
                        if (exceptions == null)
                            exceptions = new Exceptions(mappers.length);

                        e.printStackTrace();
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

    protected String getParameterName(Parameter parameter) {
        if (parameter.isNamePresent())
            return parameter.getName();

        com.wizzardo.http.framework.Parameter annotation = parameter.getAnnotation(com.wizzardo.http.framework.Parameter.class);
        if (annotation != null)
            return annotation.name();

        return null;
    }

    protected Mapper<Parameters, Object> createParametersMapper(Parameter parameter, Class type) {
        String name = getParameterName(parameter);

        Mapper<Mapper<String, Object>, Mapper<Parameters, Object>> failIfEmpty = mapper -> {
            return params -> {
                MultiValue multiValue = params.get(name);
                if (multiValue == null)
                    throw new NullPointerException("parameter '" + name + "' it not present");

                String value = multiValue.getValue();
                if (value == null || value.isEmpty())
                    throw new NullPointerException("parameter '" + name + "' it not present");

                return mapper.map(value);
            };
        };

        if (type.isPrimitive()) {
            if (type == int.class)
                return failIfEmpty.map(Integer::parseInt);
            if (type == long.class)
                return failIfEmpty.map(Long::parseLong);
            if (type == float.class)
                return failIfEmpty.map(Float::parseFloat);
            if (type == double.class)
                return failIfEmpty.map(Double::parseDouble);
            if (type == boolean.class)
                return failIfEmpty.map(Boolean::parseBoolean);
            if (type == short.class)
                return failIfEmpty.map(Short::parseShort);
            if (type == byte.class)
                return failIfEmpty.map(Byte::parseByte);
            if (type == char.class)
                return failIfEmpty.map(value -> {
                    if (value.length() > 1)
                        throw new IllegalArgumentException("Can't assign to char String with more then 1 character");
                    return value.charAt(0);
                });
        }

        Mapper<Mapper<String, Object>, Mapper<Parameters, Object>> parseNonNull = mapper -> {
            return params -> {
                MultiValue multiValue = params.get(name);
                if (multiValue == null)
                    return null;

                String value = multiValue.getValue();
                if (value == null || value.isEmpty())
                    return null;

                return mapper.map(value);
            };
        };

        if (type.isEnum())
            return parseNonNull.map(value -> Enum.valueOf((Class<? extends Enum>) type, value));

        if (type == String.class)
            return parseNonNull.map(value -> value);

        if (type == Integer.class)
            return parseNonNull.map(Integer::valueOf);
        if (type == long.class)
            return parseNonNull.map(Long::valueOf);
        if (type == float.class)
            return parseNonNull.map(Float::valueOf);
        if (type == double.class)
            return parseNonNull.map(Double::valueOf);
        if (type == boolean.class)
            return parseNonNull.map(Boolean::valueOf);
        if (type == short.class)
            return parseNonNull.map(Short::valueOf);
        if (type == byte.class)
            return parseNonNull.map(Byte::valueOf);
        if (type == char.class)
            return parseNonNull.map(value -> {
                if (value.length() > 1)
                    throw new IllegalArgumentException("Can't assign to char String with more then 1 character");
                return value.charAt(0);
            });

        throw new IllegalArgumentException("Can't create mapper for parameter '" + name + "' of type '" + type + "' in '" + controllerName + "." + actionName + "'");
    }

    protected Mapper<Parameters, Object> createParametersMapper(Parameter parameter, Type genericType) {
        if (genericType instanceof Class)
            return createParametersMapper(parameter, ((Class) genericType));

        if (genericType instanceof ParameterizedType) {
            ParameterizedType type = (ParameterizedType) genericType;
            if (type.getRawType().equals(Optional.class)) {
                Mapper<Parameters, Object> mapper = createParametersMapper(parameter, type.getActualTypeArguments()[0]);
                return parameters -> Optional.ofNullable(mapper.map(parameters));
            }
        }

        throw new IllegalArgumentException("Can't create mapper for parameter '" + parameter.getName() + "' of type '" + genericType + "' in '" + controllerName + "." + actionName + "'");
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
