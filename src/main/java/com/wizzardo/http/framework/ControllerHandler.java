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
import com.wizzardo.tools.misc.Supplier;
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
        com.wizzardo.http.framework.Parameter annotation = parameter.getAnnotation(com.wizzardo.http.framework.Parameter.class);
        String def = annotation != null ? annotation.def() : null;

        Mapper<Mapper<String, Object>, Mapper<Parameters, Object>> failIfEmpty = mapper -> {
            return params -> {
                MultiValue multiValue = params.get(name);
                String value;
                if (multiValue != null)
                    value = multiValue.getValue();
                else
                    value = def;

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
                return failIfEmpty.map(ControllerHandler::parseChar);
        }

        Mapper<Mapper<String, Object>, Mapper<Parameters, Object>> parseNonNull = mapper -> {
            return params -> {
                MultiValue multiValue = params.get(name);
                String value;
                if (multiValue != null)
                    value = multiValue.getValue();
                else
                    value = def;

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
        if (type == Long.class)
            return parseNonNull.map(Long::valueOf);
        if (type == Float.class)
            return parseNonNull.map(Float::valueOf);
        if (type == Double.class)
            return parseNonNull.map(Double::valueOf);
        if (type == Boolean.class)
            return parseNonNull.map(Boolean::valueOf);
        if (type == Short.class)
            return parseNonNull.map(Short::valueOf);
        if (type == Byte.class)
            return parseNonNull.map(Byte::valueOf);
        if (type == Character.class)
            return parseNonNull.map(ControllerHandler::parseChar);

        if (type.isArray()) {
            Class subtype = type.getComponentType();
            if (subtype.isPrimitive()) {
                if (subtype == int.class)
                    return new PrimitiveArrayConstructor<>(name, def, int[]::new, (arr, values) -> {
                        for (int i = 0; i < values.size(); i++) {
                            arr[i] = Integer.parseInt(values.get(i));
                        }
                        return arr;
                    }, arr -> Arrays.copyOf(arr, arr.length));
                if (subtype == long.class)
                    return new PrimitiveArrayConstructor<>(name, def, long[]::new, (arr, values) -> {
                        for (int i = 0; i < values.size(); i++) {
                            arr[i] = Long.parseLong(values.get(i));
                        }
                        return arr;
                    }, arr -> Arrays.copyOf(arr, arr.length));
                if (subtype == float.class)
                    return new PrimitiveArrayConstructor<>(name, def, float[]::new, (arr, values) -> {
                        for (int i = 0; i < values.size(); i++) {
                            arr[i] = Float.parseFloat(values.get(i));
                        }
                        return arr;
                    }, arr -> Arrays.copyOf(arr, arr.length));
                if (subtype == double.class)
                    return new PrimitiveArrayConstructor<>(name, def, double[]::new, (arr, values) -> {
                        for (int i = 0; i < values.size(); i++) {
                            arr[i] = Double.parseDouble(values.get(i));
                        }
                        return arr;
                    }, arr -> Arrays.copyOf(arr, arr.length));
                if (subtype == boolean.class)
                    return new PrimitiveArrayConstructor<>(name, def, boolean[]::new, (arr, values) -> {
                        for (int i = 0; i < values.size(); i++) {
                            arr[i] = Boolean.parseBoolean(values.get(i));
                        }
                        return arr;
                    }, arr -> Arrays.copyOf(arr, arr.length));
                if (subtype == short.class)
                    return new PrimitiveArrayConstructor<>(name, def, short[]::new, (arr, values) -> {
                        for (int i = 0; i < values.size(); i++) {
                            arr[i] = Short.parseShort(values.get(i));
                        }
                        return arr;
                    }, arr -> Arrays.copyOf(arr, arr.length));
                if (subtype == byte.class)
                    return new PrimitiveArrayConstructor<>(name, def, byte[]::new, (arr, values) -> {
                        for (int i = 0; i < values.size(); i++) {
                            arr[i] = Byte.parseByte(values.get(i));
                        }
                        return arr;
                    }, arr -> Arrays.copyOf(arr, arr.length));
                if (subtype == char.class)
                    return new PrimitiveArrayConstructor<>(name, def, char[]::new, (arr, values) -> {
                        for (int i = 0; i < values.size(); i++) {
                            arr[i] = parseChar(values.get(i));
                        }
                        return arr;
                    }, arr -> Arrays.copyOf(arr, arr.length));
            } else {
                if (subtype == Integer.class)
                    return new ArrayConstructor<>(name, def, Integer[]::new, Integer::valueOf);
                if (subtype == Long.class)
                    return new ArrayConstructor<>(name, def, Long[]::new, Long::valueOf);
                if (subtype == Float.class)
                    return new ArrayConstructor<>(name, def, Float[]::new, Float::valueOf);
                if (subtype == Double.class)
                    return new ArrayConstructor<>(name, def, Double[]::new, Double::valueOf);
                if (subtype == Boolean.class)
                    return new ArrayConstructor<>(name, def, Boolean[]::new, Boolean::valueOf);
                if (subtype == Short.class)
                    return new ArrayConstructor<>(name, def, Short[]::new, Short::valueOf);
                if (subtype == Byte.class)
                    return new ArrayConstructor<>(name, def, Byte[]::new, Byte::valueOf);
                if (subtype == Character.class)
                    return new ArrayConstructor<>(name, def, Character[]::new, ControllerHandler::parseChar);

                if (subtype == String.class)
                    return new ArrayConstructor<>(name, def, String[]::new, s -> s);

                if (subtype.isEnum())
                    return new ArrayConstructor<>(name, def, size -> (Enum[]) Array.newInstance(subtype, size), s -> Enum.valueOf((Class<? extends Enum>) subtype, s));
            }
        }

        throw new IllegalArgumentException("Can't create mapper for parameter '" + name + "' of type '" + type + "' in '" + controllerName + "." + actionName + "'");
    }

    static char parseChar(String value) {
        if (value.length() > 1)
            throw new IllegalArgumentException("Can't assign to char String with more then 1 character");
        return value.charAt(0);
    }

    static class PrimitiveArrayConstructor<T> implements Mapper<Parameters, Object> {
        final String name;
        final Mapper<Integer, T> creator;
        final Mapper<T, T> cloner;
        final CollectionTools.Closure2<T, T, List<String>> populator;
        final T def;

        PrimitiveArrayConstructor(String name, String def, Mapper<Integer, T> creator, CollectionTools.Closure2<T, T, List<String>> populator, Mapper<T, T> cloner) {
            this.name = name;
            this.creator = creator;
            this.populator = populator;
            this.cloner = cloner;
            if (def != null && !def.isEmpty()) {
                List<String> strings = Arrays.asList(def.split(","));
                this.def = populator.execute(creator.map(strings.size()), strings);
            } else {
                this.def = null;
            }
        }

        @Override
        public T map(Parameters parameters) {
            MultiValue multiValue = parameters.get(name);
            if (multiValue != null) {
                T t = creator.map(multiValue.size());
                return populator.execute(t, multiValue.getValues());
            }

            if (def == null)
                return null;

            return cloner.map(def);
        }
    }

    static class ArrayConstructor<T> implements Mapper<Parameters, Object> {
        final String name;
        final Mapper<Integer, T[]> creator;
        final Mapper<String, T> converter;
        final T[] def;

        ArrayConstructor(String name, String def, Mapper<Integer, T[]> creator, Mapper<String, T> converter) {
            this.name = name;
            this.creator = creator;
            this.converter = converter;
            if (def != null && !def.isEmpty()) {
                List<String> strings = Arrays.asList(def.split(","));
                this.def = creator.map(strings.size());
                populate(this.def, strings, converter);
            } else {
                this.def = null;
            }
        }

        @Override
        public T[] map(Parameters parameters) {
            MultiValue multiValue = parameters.get(name);
            if (multiValue != null) {
                T[] arr = creator.map(multiValue.size());
                populate(arr, multiValue.getValues(), converter);
                return arr;
            }

            if (def == null)
                return null;

            return Arrays.copyOf(def, def.length);
        }

        protected void populate(T[] arr, List<String> values, Mapper<String, T> converter) {
            for (int i = 0; i < values.size(); i++) {
                arr[i] = converter.map(values.get(i));
            }
        }
    }

    static class CollectionConstructor<C extends Collection<T>, T> implements Mapper<Parameters, Object> {
        final String name;
        final Supplier<C> supplier;
        final Mapper<String, T> converter;
        final List<T> def;

        CollectionConstructor(String name, String def, Supplier<C> supplier, Mapper<String, T> converter) {
            this.name = name;
            this.supplier = supplier;
            this.converter = converter;
            if (def != null && !def.isEmpty()) {
                populate(this.def = new ArrayList<>(), Arrays.asList(def.split(",")), converter);
            } else {
                this.def = null;
            }
        }

        @Override
        public C map(Parameters parameters) {
            MultiValue multiValue = parameters.get(name);
            if (multiValue != null) {
                C arr = supplier.supply();
                populate(arr, multiValue.getValues(), converter);
                return arr;
            }

            if (def == null)
                return null;

            C c = supplier.supply();
            c.addAll(def);
            return c;
        }

        protected void populate(Collection<T> c, List<String> values, Mapper<String, T> converter) {
            for (String value : values) {
                c.add(converter.map(value));
            }
        }
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
            if (Iterable.class.isAssignableFrom((Class<?>) type.getRawType())) {
                Class subtype = (Class) type.getActualTypeArguments()[0];
                return createParametersMapper(parameter, createCollection((Class<? extends Iterable>) type.getRawType()), subtype);
            }
        }

        throw new IllegalArgumentException("Can't create mapper for parameter '" + parameter.getName() + "' of type '" + genericType + "' in '" + controllerName + "." + actionName + "'");
    }

    protected <C extends Collection> Mapper<Parameters, Object> createParametersMapper(Parameter parameter, Supplier<C> collectionSupplier, Class subtype) {
        String name = getParameterName(parameter);
        com.wizzardo.http.framework.Parameter annotation = parameter.getAnnotation(com.wizzardo.http.framework.Parameter.class);
        String def = annotation != null ? annotation.def() : null;

        if (subtype == Integer.class)
            return new CollectionConstructor<>(name, def, (Supplier<Collection<Integer>>) collectionSupplier, Integer::valueOf);
        if (subtype == Long.class)
            return new CollectionConstructor<>(name, def, (Supplier<Collection<Long>>) collectionSupplier, Long::valueOf);
        if (subtype == Float.class)
            return new CollectionConstructor<>(name, def, (Supplier<Collection<Float>>) collectionSupplier, Float::valueOf);
        if (subtype == Double.class)
            return new CollectionConstructor<>(name, def, (Supplier<Collection<Double>>) collectionSupplier, Double::valueOf);
        if (subtype == Boolean.class)
            return new CollectionConstructor<>(name, def, (Supplier<Collection<Boolean>>) collectionSupplier, Boolean::valueOf);
        if (subtype == Short.class)
            return new CollectionConstructor<>(name, def, (Supplier<Collection<Short>>) collectionSupplier, Short::valueOf);
        if (subtype == Byte.class)
            return new CollectionConstructor<>(name, def, (Supplier<Collection<Byte>>) collectionSupplier, Byte::valueOf);
        if (subtype == Character.class)
            return new CollectionConstructor<>(name, def, (Supplier<Collection<Character>>) collectionSupplier, ControllerHandler::parseChar);

        if (subtype == String.class)
            return new CollectionConstructor<>(name, def, (Supplier<Collection<String>>) collectionSupplier, s -> s);

        if (subtype.isEnum())
            return new CollectionConstructor<>(name, def, collectionSupplier, s -> (Object) Enum.valueOf((Class<? extends Enum>) subtype, s));


        throw new IllegalArgumentException("Can't create collection mapper for parameter '" + parameter.getName() + "' with subtype '" + subtype + "' in '" + controllerName + "." + actionName + "'");
    }

    protected <C extends Collection> Supplier<C> createCollection(Class clazz) {
        int modifiers = clazz.getModifiers();
        if (Modifier.isInterface(modifiers) || Modifier.isAbstract(modifiers)) {
            if (List.class.isAssignableFrom(clazz))
                return () -> (C) new ArrayList();

            if (Set.class.isAssignableFrom(clazz))
                return () -> (C) new HashSet();

            if (Iterable.class.isAssignableFrom(clazz))
                return () -> (C) new ArrayList();

        }

        return () -> {
            try {
                return (C) clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException("Cannot create instance of class '" + clazz.getCanonicalName() + "'");
            }
        };
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
