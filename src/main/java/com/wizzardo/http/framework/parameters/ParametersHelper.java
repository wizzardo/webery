package com.wizzardo.http.framework.parameters;

import com.wizzardo.http.MultiValue;
import com.wizzardo.http.request.MultiPartEntry;
import com.wizzardo.http.request.Request;
import com.wizzardo.tools.misc.Mapper;
import com.wizzardo.tools.misc.Supplier;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Created by wizzardo on 25/10/16.
 */
public class ParametersHelper {

    public static String getParameterName(java.lang.reflect.Parameter parameter) {
        if (parameter.isNamePresent())
            return parameter.getName();

        Parameter annotation = parameter.getAnnotation(Parameter.class);
        if (annotation != null)
            return annotation.name();

        return null;
    }


    static char parseChar(String value) {
        if (value.length() > 1)
            throw new IllegalArgumentException("Can't assign to char String with more then 1 character");
        return value.charAt(0);
    }

    public static <C extends Collection> Supplier<C> createCollection(Class clazz) {
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

    public static Mapper<Request, Object> createParametersMapper(java.lang.reflect.Parameter parameter, Type genericType) {
        if (genericType instanceof Class)
            return createParametersMapper(parameter, ((Class) genericType));

        if (genericType instanceof ParameterizedType) {
            ParameterizedType type = (ParameterizedType) genericType;
            if (type.getRawType().equals(Optional.class)) {
                Mapper<Request, Object> mapper = createParametersMapper(parameter, type.getActualTypeArguments()[0]);
                return parameters -> Optional.ofNullable(mapper.map(parameters));
            }
            if (Iterable.class.isAssignableFrom((Class<?>) type.getRawType())) {
                Class subtype = (Class) type.getActualTypeArguments()[0];
                return createParametersMapper(parameter, createCollection((Class<? extends Iterable>) type.getRawType()), subtype);
            }
        }

        throw new IllegalArgumentException("Can't create mapper for parameter '" + parameter.getName() + "' of type '" + genericType + "'");
    }

    public static <C extends Collection> Mapper<Request, Object> createParametersMapper(java.lang.reflect.Parameter parameter, Supplier<C> collectionSupplier, Class subtype) {
        String name = getParameterName(parameter);
        Parameter annotation = parameter.getAnnotation(Parameter.class);
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
            return new CollectionConstructor<>(name, def, (Supplier<Collection<Character>>) collectionSupplier, ParametersHelper::parseChar);

        if (subtype == String.class)
            return new CollectionConstructor<>(name, def, (Supplier<Collection<String>>) collectionSupplier, s -> s);

        if (subtype.isEnum())
            return new CollectionConstructor<>(name, def, collectionSupplier, s -> (Object) Enum.valueOf((Class<? extends Enum>) subtype, s));


        throw new IllegalArgumentException("Can't create collection mapper for parameter '" + parameter.getName() + "' with subtype '" + subtype + "'");
    }

    public static Mapper<Request, Object> createParametersMapper(java.lang.reflect.Parameter parameter, Class type) {
        String name = getParameterName(parameter);
        Parameter annotation = parameter.getAnnotation(Parameter.class);
        String def = annotation != null ? annotation.def() : null;

        Mapper<Mapper<String, Object>, Mapper<Request, Object>> failIfEmpty = mapper -> {
            return request -> {
                MultiValue multiValue = request.params().get(name);
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
                return failIfEmpty.map(ParametersHelper::parseChar);
        }

        Mapper<Mapper<String, Object>, Mapper<Request, Object>> parseNonNull = mapper -> {
            return request -> {
                MultiValue multiValue = request.params().get(name);
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
            return parseNonNull.map(ParametersHelper::parseChar);

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
                    return new ArrayConstructor<>(name, def, Character[]::new, ParametersHelper::parseChar);

                if (subtype == String.class)
                    return new ArrayConstructor<>(name, def, String[]::new, s -> s);

                if (subtype.isEnum())
                    return new ArrayConstructor<>(name, def, size -> (Enum[]) Array.newInstance(subtype, size), s -> Enum.valueOf((Class<? extends Enum>) subtype, s));
            }
        }

        throw new IllegalArgumentException("Can't create mapper for parameter '" + name + "' of type '" + type + "'");
    }
}
