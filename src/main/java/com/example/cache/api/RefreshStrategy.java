package com.example.cache.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface RefreshStrategy<K, C, I> {
    Map<I,C> convertCollectionToMap(Collection elements);
    C refreshCache(C existingObject, C newerObject, String isDelete);

    Map<I, C> findElementsAndRefreshCache(String cacheName, List arguments, String findMethodName);

    C getExistingObjectByIdentifier(Object id);
}
