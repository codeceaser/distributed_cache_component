package com.example.user_service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Set;

import static com.example.cache.api.AbstractCacheRefreshStrategy.CREATE_IGNITE_INDICES;
import static com.example.cache.user.AbstractUsersCacheRefreshStrategy.USER_DTO_TYPE_REFERENCE;
import static com.example.utils.CommonUtil.INDEXED_FIELDS_EXTRACTOR;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommonUtilTest {

    @Test
    public void testGetCacheIdentifierField() throws NoSuchMethodException {
        Type type = USER_DTO_TYPE_REFERENCE.getType();
        Class clazz = (Class)type;
        Set<Field> indexedFields = INDEXED_FIELDS_EXTRACTOR.apply(clazz);
        Collection collection = CREATE_IGNITE_INDICES.apply(clazz);
        assertTrue(!collection.isEmpty());

        Method getId = clazz.getMethod("getId", null);
        Class<?> returnType = getId.getReturnType();
        System.out.println(returnType.getName());
    }
}
