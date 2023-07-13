package com.example.cache.api;

import com.example.components.Cacheable;
import com.example.repositories.api.IndicesBasedWrapperRepository;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface InvertedIndicesRefreshStrategy<K, C extends Cacheable, I> extends RefreshStrategy<K, C, I>{

    Logger LOGGER = LoggerFactory.getLogger(InvertedIndicesRefreshStrategy.class);
    IndicesBasedWrapperRepository indicesBasedWrapperRepository();

    default C refreshCache(C existingObject, C newerObject, String isDelete){
        if(null == newerObject && null == existingObject){
            String errorMessage = "Both existing and newer objects are null, Refresh can not be performed without any of them";
            LOGGER.error("### " + errorMessage + " ###");
            throw new RuntimeException(errorMessage);
        }
        if (null != newerObject) {
            LOGGER.info("Newer Object Id is: {}", newerObject.getId());
        }
        if(null != existingObject) {
            LOGGER.info("Existing Object Id is: {}", existingObject.getId());
        }
        I keyForRefresh = (I) Optional.ofNullable(newerObject).map(Cacheable::getId).orElseGet(() -> Optional.ofNullable(existingObject).map(Cacheable::getId).orElseGet(() -> null));
        if (null == keyForRefresh) {
            String errorMessage = "Object Identifier is null, Refresh can not be performed without such Identifier";
            LOGGER.error("### " + errorMessage + " ###");
            throw new RuntimeException(errorMessage);
        }
        LOGGER.info("Refreshing cache for object with id: {}", keyForRefresh);
        //There is always a newer object from client and hence it is not null and we can always extract the id
        C originalObject = (C) indicesBasedWrapperRepository().findOriginalObject(keyForRefresh);
        LOGGER.info("Original Object is: {}", Optional.ofNullable(originalObject).map(String::valueOf).orElseGet(() -> "null"));
        if (StringUtils.isBlank(isDelete)) {
            isDelete = "N";
        }
        if(StringUtils.equalsIgnoreCase("N", isDelete)){
            // Here is where we are making an entry into Elastic Search
            LOGGER.info("Creating inverted indices and cache entries for object with id: {}", keyForRefresh);
            indicesBasedWrapperRepository().createInvertedIndicesAndCacheEntries(Lists.newArrayList(newerObject));
        } else {
            LOGGER.info("Removing inverted indices and cache entries for object with id: {}", keyForRefresh);
            indicesBasedWrapperRepository().removeInvertedIndicesAndCacheEntries(keyForRefresh);
        }
        return originalObject;
    }
    default Map<I, C> findElementsAndRefreshCache(String cacheName, List arguments, String findMethodName) {
        LOGGER.info("Finding elements using inverted indices and cache for : {}", cacheName);
        Map<I, C> finalObjects = null;
        finalObjects = indicesBasedWrapperRepository().findByAttributesAndRefreshInvertedIndicesAndCache(arguments, findMethodName);
        return finalObjects;
    }
}
