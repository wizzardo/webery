package com.wizzardo.http.framework.parameters;

import com.wizzardo.http.MultiValue;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.MultiPartEntry;
import com.wizzardo.http.request.MultiPartFileEntry;
import com.wizzardo.http.request.Request;
import com.wizzardo.tools.interfaces.Consumer;
import com.wizzardo.tools.misc.Pair;
import com.wizzardo.tools.interfaces.Mapper;
import com.wizzardo.tools.interfaces.Supplier;
import com.wizzardo.tools.json.JsonTools;
import com.wizzardo.tools.reflection.FieldInfo;
import com.wizzardo.tools.reflection.Fields;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Created by wizzardo on 25/10/16.
 */
public class ParametersHelper {

    public interface ParameterMapper<T> {
        void map(Request request, String name, Consumer<T> consumer);
    }

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
        String name = getParameterName(parameter);
        Parameter annotation = parameter.getAnnotation(Parameter.class);
        String def = annotation != null && !annotation.def().equals(Parameter.Constants.DEFAULT_NONE) ? annotation.def() : null;
        return createParametersMapper(name, def, genericType);
    }

    public static Mapper<Request, Object> createParametersMapper(String name, String def, Type genericType) {
        return createParametersMapper(name, def, genericType, Collections.emptyMap());
    }

    public static Mapper<Request, Object> createParametersMapper(String name, String def, Type genericType, Map<Class, ParameterMapper<?>> customMappers) {
        if (genericType instanceof Class)
            return createParametersMapper(name, def, ((Class) genericType), customMappers);

        if (genericType instanceof ParameterizedType) {
            ParameterizedType type = (ParameterizedType) genericType;
            if (type.getRawType().equals(Optional.class)) {
                Mapper<Request, Object> mapper = createParametersMapper(name, def, type.getActualTypeArguments()[0], customMappers);
                return parameters -> Optional.ofNullable(mapper.map(parameters));
            }
            if (Iterable.class.isAssignableFrom((Class<?>) type.getRawType())) {
                Class subtype = (Class) type.getActualTypeArguments()[0];
                Class<? extends Iterable> collectionClass = (Class<? extends Iterable>) type.getRawType();
                Mapper<Request, Object> parametersMapper = createParametersMapper(name, def, createCollection(collectionClass), subtype, customMappers);

                return request -> {
                    if (request.data() != null && Header.VALUE_APPLICATION_JSON.value.equalsIgnoreCase(request.header(Header.KEY_CONTENT_TYPE)))
                        return JsonTools.parse(request.data(), collectionClass, subtype);

                    if (parametersMapper != null)
                        return parametersMapper.map(request);

                    throw new IllegalArgumentException("Can't parse " + collectionClass.getSimpleName() + "<" + subtype.getSimpleName() + "> for parameter '" + name + "'");
                };
            }
        }

        throw new IllegalArgumentException("Can't create mapper for parameter '" + name + "' of type '" + genericType + "'");
    }

    public static <C extends Collection> Mapper<Request, Object> createParametersMapper(String name, String def, Supplier<C> collectionSupplier, Class subtype, Map<Class, ParameterMapper<?>> customMappers) {
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

        ParameterMapper<?> mapper = customMappers.get(subtype);
        if (mapper != null)
            return request -> {
                C c = collectionSupplier.supply();
                mapper.map(request, name, c::add);
                return c;
            };

        return null;
    }

    public static Mapper<Request, Object> createParametersMapper(String name, String def, Class type) {
        return createParametersMapper(name, def, type, Collections.emptyMap());
    }

    public static Mapper<Request, Object> createParametersMapper(String name, String def, Class type, Map<Class, ParameterMapper<?>> customMappers) {
        Mapper<Mapper<String, Object>, Mapper<Request, Object>> failIfEmpty = mapper -> {
            return request -> {
                MultiValue<String> multiValue = request.params().get(name);
                String value;
                if (multiValue != null)
                    value = multiValue.getValue();
                else
                    value = def;

                if (value == null || value.isEmpty())
                    throw new NullPointerException("parameter '" + name + "' is not present");

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
                MultiValue<String> multiValue = request.params().get(name);
                String value;
                if (multiValue != null)
                    value = multiValue.getValue();
                else
                    value = def;

                if (value == null)
                    return null;

                return mapper.map(value);
            };
        };

        if (type.isEnum())
            return parseNonNull.map(value -> Enum.valueOf((Class<? extends Enum>) type, value));

        if (type == String.class)
            return parseNonNull.map(value -> value);

        if (File.class.isAssignableFrom(type)) {
            return request -> {
                if (!request.isMultipart())
                    return null;

                MultiPartEntry entry = request.entry(name);
                if (entry == null)
                    return null;

                if (!(entry instanceof MultiPartFileEntry))
                    return null;

                return ((MultiPartFileEntry) entry).getFile();
            };
        }

        if (MultiPartEntry.class.isAssignableFrom(type)) {
            return request -> !request.isMultipart() ? null : request.entry(name);
        }

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
                    return new PrimitiveArrayConstructor<byte[]>(name, def, byte[]::new, (arr, values) -> {
                        for (int i = 0; i < values.size(); i++) {
                            arr[i] = Byte.parseByte(values.get(i));
                        }
                        return arr;
                    }, arr -> Arrays.copyOf(arr, arr.length)) {
                        @Override
                        public byte[] map(Request request) {
                            if (request.getBody() != null) {
                                return request.getBody().bytes();
                            }
                            if (request.isMultipart()) {
                                MultiPartEntry entry = request.entry(name);
                                if (entry != null)
                                    return entry.asBytes();
                            }
                            return super.map(request);
                        }
                    };
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

                ParameterMapper<?> mapper = customMappers.get(subtype);
                if (mapper != null)
                    return request -> {
                        ArrayList c = new ArrayList();
                        mapper.map(request, name, c::add);
                        return c.toArray((Object[]) Array.newInstance(subtype, c.size()));
                    };
            }
        }

        return createPojoMapper(type, name, customMappers);
    }

    protected static Mapper<Request, Object> createPojoMapper(Class type, String parameterName, Map<Class, ParameterMapper<?>> customMappers) {
        Fields<FieldInfo> fields = Fields.getFields(type);
        List<Pair<FieldInfo, Mapper<Request, Object>>> mappers = new ArrayList<>(fields.size());
        for (FieldInfo field : fields) {
            if (field.field.getType().equals(type))
                continue; // doesn't support recursion in simple form data

            Parameter annotation = field.field.getAnnotation(Parameter.class);
            String name = field.field.getName();
            String def = null;
            if (annotation != null) {
                if (!annotation.name().isEmpty())
                    name = annotation.name();
                if (!annotation.def().isEmpty())
                    def = annotation.def();
            }
            try {
                mappers.add(new Pair<>(field, createParametersMapper(name, def, field.field.getGenericType())));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return request -> {
            String contentType = request.header(Header.KEY_CONTENT_TYPE);
            if (request.data() != null && contentType != null && contentType.toLowerCase().startsWith(Header.VALUE_APPLICATION_JSON.value))
                return JsonTools.parse(request.data(), type);

            if (request.isMultipart()) {
                MultiPartEntry entry = request.entry(parameterName);
                if (entry != null) {
                    String entryContentType = entry.contentType();
                    if (entryContentType != null && entryContentType.toLowerCase().startsWith(Header.VALUE_APPLICATION_JSON.value)) {
                        return JsonTools.parse(entry.asBytes(), type);
                    }
                }
            }

            ParameterMapper<?> m = customMappers.get(type);
            if (m != null) {
                Object[] ref = new Object[1];
                m.map(request, parameterName, o -> ref[0] = o);
                return ref[0];
            }

            try {
                Object instance = type.newInstance();
                for (Pair<FieldInfo, Mapper<Request, Object>> mapper : mappers) {
                    Object value = mapper.value.map(request);
                    if (value != null)
                        mapper.key.field.set(instance, value);
                }
                return instance;
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException("Cannot create instance of argument type " + type.getCanonicalName(), e);
            }
        };
    }
}
