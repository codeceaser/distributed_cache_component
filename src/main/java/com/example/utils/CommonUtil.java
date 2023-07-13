package com.example.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.function.TriFunction;
import org.springframework.util.CollectionUtils;

import java.beans.Expression;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class CommonUtil {

    public static final BiFunction<String, Supplier<TypeReference>, Object> JSON_STRING_TO_OBJECT_CONVERTER = (jsonString, typeReferenceSupplier) -> {
        try {
            return getObjectMapper().readValue(jsonString, (TypeReference)typeReferenceSupplier.get());
        } catch (Exception var3) {
            throw new RuntimeException(var3);
        }
    };
    public static final Function<Object, String> OBJECT_TO_JSON_CONVERTER = (object) -> {
        if (object == null) {
            return "";
        } else {
            try {
                return getObjectMapper().writeValueAsString(object);
            } catch (Exception var2) {
                throw new RuntimeException(var2);
            }
        }
    };
    public static final Map<Class, Map<String, Field>> CLASS_FIELD_MAP = Maps.newHashMap();
    public static final BiFunction<Class, String, Field> extractField = (clazz, fieldName) -> {
        Field f = null;
        if (!CLASS_FIELD_MAP.containsKey(clazz)) {
            CLASS_FIELD_MAP.put(clazz, Maps.newHashMap());
        }

        Map<String, Field> stringFieldMap = (Map)CLASS_FIELD_MAP.get(clazz);
        if (stringFieldMap.containsKey(fieldName)) {
            f = (Field)stringFieldMap.get(fieldName);
        } else {
            try {
                f = clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException var5) {
                f = (Field)ClassUtils.getAllSuperclasses(clazz).stream().map((cls) -> {
                    try {
                        Field declaredField = cls.getDeclaredField(fieldName);
                        return declaredField;
                    } catch (NoSuchFieldException var3) {
                        return null;
                    }
                }).filter(Objects::nonNull).findFirst().orElseGet(() -> {
                    return null;
                });
            }

            if (Objects.nonNull(f)) {
                f.setAccessible(true);
                stringFieldMap.put(fieldName, f);
            }
        }

        return f;
    };
    public static final BiFunction<Class, String, Object> extractValue = (clazz, fieldName) -> {
        Field field = (Field)extractField.apply(clazz, fieldName);
        if (Objects.nonNull(field)) {
            try {
                return field.get(clazz);
            } catch (IllegalAccessException var4) {
            }
        }

        return null;
    };
    public static final BiFunction<Object, String, Object> get = (source, getter) -> {
        Object result = null;

        try {
            if (StringUtils.isBlank(getter)) {
                return result;
            }

            Expression expr = new Expression(source, getter, (Object[])null);
            expr.execute();
            result = expr.getValue();
        } catch (Exception var4) {
        }

        return result;
    };
    public static final TriFunction<Object, String, Object[], Object> retrieve = (source, getter, arguments) -> {
        Object result = null;

        try {
            if (StringUtils.isBlank(getter)) {
                return result;
            }

            Expression expr = new Expression(source, getter, arguments);
            expr.execute();
            result = expr.getValue();
        } catch (Exception var5) {
        }

        return result;
    };
    public static final BiFunction<Class, Collection<String>, Map<Field, Method>> fieldToGetterExtractor = (clazz, methodFields) -> {
        Map<Field, Method> fieldGetterMap = Maps.newHashMap();
        if (!CollectionUtils.isEmpty(methodFields)) {
            try {
                Arrays.stream(Introspector.getBeanInfo(clazz).getPropertyDescriptors()).filter((propertyDescriptor) -> {
                    return methodFields.contains(propertyDescriptor.getName());
                }).forEach((propertyDescriptor) -> {
                    fieldGetterMap.put((Field)extractField.apply(clazz, propertyDescriptor.getName()), propertyDescriptor.getReadMethod());
                });
            } catch (IntrospectionException var4) {
            }
        }

        return fieldGetterMap;
    };

    public CommonUtil() {
    }

    public static ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
