package com.example.cache.api;

import com.example.components.Cacheable;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.util.CollectionUtils;

import javax.cache.Cache;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.example.utils.CommonUtil.*;
import static java.util.stream.Collectors.joining;

//All Operations are having Time Complexity: O(1)
public interface CacheRefreshStrategy<K, C  extends Cacheable, I> extends RefreshStrategy<K, C, I>{

    Logger LOGGER = LoggerFactory.getLogger(CacheRefreshStrategy.class);

    String cacheName();
    List<String> cacheKeyFields();
    String cacheIdentifierField();

    Boolean isEvictionFromExistingCacheRequired(C existingObject);

    Boolean areKeysDifferent(C existing, C newer);

    public static final BiFunction<Object, List<String>, Object> EXTRACT_CACHE_KEY = (cacheObject, cacheKeyFields) -> {
        Object keyForExistingCache = null;
        Collection<Method> gettersForCacheKey = cacheKeyFields.stream().map(cacheKeyField -> fieldToGetterExtractor.apply(cacheObject.getClass(), Sets.newHashSet(cacheKeyField)).get(extractField.apply(cacheObject.getClass(), cacheKeyField))).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(cacheKeyFields)) {
            if (cacheKeyFields.size() > 1) {
                keyForExistingCache = gettersForCacheKey.stream().map(getter -> get.apply(cacheObject, getter.getName())).map(String::valueOf).collect(joining("-"));
            } else{
                keyForExistingCache = gettersForCacheKey.stream().map(getter -> get.apply(cacheObject, getter.getName())).findFirst().get();
            }
        }
        return keyForExistingCache;
    };

    Cache retrieveCache(String cacheName);

    Cache extractNativeCache(Cache cache, Object keyForExistingCache);

    Map extractCacheMapUsingKey(Cache cache, Object keyForExistingCache);

    Lock lockCache(Cache cache, Object keyForExistingCache);

    void cacheSpecificEntryReplacement(Cache cache, Object keyForExistingCache, Map cacheMap);

    void cacheSpecificEntryCreation(Cache cache, Object keyForCacheEntry, Map cacheMap);

    boolean cacheSpecificEntryCheck(Cache cache, Object keyForCacheEntry);

    default C removeObjectFromCache(C existingObject, Cache cachesPresent){
        C removedObject = null;
        Object keyForExistingCache = EXTRACT_CACHE_KEY.apply(existingObject, cacheKeyFields());
        LOGGER.info("@@@ keyForExistingCache: {}", keyForExistingCache);
        Cache existingCache = extractNativeCache(cachesPresent, keyForExistingCache);
        if (Objects.nonNull(existingCache)) {
            Map cacheMap = (Map) extractCacheMapUsingKey(existingCache, keyForExistingCache);
            if(Objects.nonNull(cacheMap)){
                LOGGER.info("@@@ BEFORE: For Key: {}, Value From Native Cache is: {} ###", keyForExistingCache, cacheMap);
                Lock cacheLock = lockCache(existingCache, keyForExistingCache);
                if(Objects.nonNull(cacheLock)){
                    cacheLock.lock();
                }
                try{
                    LOGGER.info("@@@ Cache for Key {} is found", keyForExistingCache);
                    Method getterForCacheObjectIdentifier = fieldToGetterExtractor.apply(existingObject.getClass(), Sets.newHashSet(cacheIdentifierField())).get(extractField.apply(existingObject.getClass(), cacheIdentifierField()));
                    Object id = get.apply(existingObject, getterForCacheObjectIdentifier.getName());
                    if (cacheMap.containsKey(id)) {
                        LOGGER.info("@@@ Removing an entry {} from existing cache for key {}", existingObject, keyForExistingCache);
                        removedObject = (C)cacheMap.remove(id);
                        cacheSpecificEntryReplacement(existingCache, keyForExistingCache, cacheMap);
                    }
                }finally {
                    if(Objects.nonNull(cacheLock)){
                        cacheLock.unlock();
                    }
                }
                LOGGER.info("@@@ AFTER: For Key: {}, Value From Native Cache is: {} ###", keyForExistingCache, extractCacheMapUsingKey(existingCache, keyForExistingCache));
            }
        }
        return removedObject;
    }

    //Time Complexity: O(1)
    default C processEvictionFromExistingCache(C existingObject, C newerObject, Cache cachesPresent) {
        C removedObject = null;
        if (Objects.nonNull(cachesPresent) && isEvictionFromExistingCacheRequired(existingObject)) {
            if (areKeysDifferent(existingObject, newerObject)) {
                removedObject = removeObjectFromCache(existingObject, cachesPresent);
                Object keyForNewerCache = EXTRACT_CACHE_KEY.apply(newerObject, cacheKeyFields());
                Object keyForExistingCache = EXTRACT_CACHE_KEY.apply(existingObject, cacheKeyFields());
                LOGGER.info("@@@ Key for Cached Object {} has changed from {} to {}", existingObject, keyForExistingCache, keyForNewerCache);
            }
        }
        return removedObject;
    }

    //Time Complexity: O(1)
    default C updateCache(C newerObject, Cache cachesPresent) {
        C addedObject = null;
        if (Objects.nonNull(cachesPresent)) {
            Object keyForNewerCache = EXTRACT_CACHE_KEY.apply(newerObject, cacheKeyFields());
            Method getterForCacheObjectIdentifier = fieldToGetterExtractor.apply(newerObject.getClass(), Sets.newHashSet(cacheIdentifierField())).get(extractField.apply(newerObject.getClass(), cacheIdentifierField()));
            Object id = get.apply(newerObject, getterForCacheObjectIdentifier.getName());
            Cache nativeCache = extractNativeCache(cachesPresent, keyForNewerCache);
            if(nativeCache != null){
                LOGGER.info("@@@ BEFORE: For Key: {}, Value From Native Cache is: {} ###", keyForNewerCache, extractCacheMapUsingKey(nativeCache, keyForNewerCache));
                Lock cacheLock = lockCache(nativeCache, keyForNewerCache);
                if(Objects.nonNull(cacheLock)){
                    cacheLock.lock();
                }
                try{
                    Map cacheMap = (Map) extractCacheMapUsingKey(nativeCache, keyForNewerCache);
                    if (Objects.isNull(cacheMap)) {
                        cacheMap = Maps.newConcurrentMap();
                    }
                    if (cacheMap.containsKey(id)) {
                        LOGGER.info("@@@ Removing an existing entry {} from cache, for the cache-value-key {}", cacheMap.get(id), keyForNewerCache);
                        cacheMap.remove(id);
                    }
                    LOGGER.info("@@@ Adding an entry {} into cache, for the cache-value-key {}", newerObject, keyForNewerCache);
                    addedObject = (C) cacheMap.put(id, newerObject);
                    cacheSpecificEntryReplacement(nativeCache, keyForNewerCache, cacheMap);
                }finally {
                    if(Objects.nonNull(cacheLock)){
                        cacheLock.unlock();
                    }
                }
                LOGGER.info("@@@ AFTER: For Key: {}, Value From Native Cache is: {} ###", keyForNewerCache, nativeCache.get(keyForNewerCache));
            }
        }
        return newerObject;
    }

    //Time Complexity: O(1)
    default C refreshCache(C existingObject, C newerObject, String isDelete){
        if (StringUtils.isBlank(isDelete)) {
            isDelete = "N";
        }
        C removedObject = null;
        //Input: Cache Name
        Cache caches = retrieveCache(cacheName());
        if (StringUtils.equalsIgnoreCase("N", isDelete)) {
            removedObject = processEvictionFromExistingCache(existingObject, newerObject, caches);
            C addedObject = updateCache(newerObject, caches);
        } else {
            removedObject = removeObjectFromCache(existingObject, caches);
        }
        return removedObject;
    }

    JpaRepository jpaRepository();

    I getMaxValueForIdentifier();

    Cache createCacheIfAbsent(String cacheName);

    default Map<I, C> findElementsAndRefreshCache(String cacheName, List arguments, String findMethodName) {
        Map<I, C> allElements = Maps.newConcurrentMap();
        Cache caches = retrieveCache(cacheName);
        Object keyForCacheEntry = arguments.stream().map(String::valueOf).collect(joining("-"));
        if (Objects.isNull(caches)) {
            LOGGER.info("@@@ There is no cache for Cache Name - {}, hence creating an entry for the cache", cacheName);
            caches = createCacheIfAbsent(cacheName);
            caches.put(keyForCacheEntry, Maps.newConcurrentMap());
        }
        Cache nativeCache = extractNativeCache(caches, keyForCacheEntry);

        if (Objects.nonNull(nativeCache)) {
            LOGGER.info("@@@ BEFORE: For Key: {}, Value From Native Cache is: {}", keyForCacheEntry, extractCacheMapUsingKey(nativeCache, keyForCacheEntry));

            Map<I, C> cachedElements = (Map<I, C>) extractCacheMapUsingKey(nativeCache, keyForCacheEntry);

            boolean elementsFoundInCache = Objects.nonNull(cachedElements) && !cachedElements.isEmpty();
            if (elementsFoundInCache) {
                allElements.putAll(cachedElements);
            }

            Collection<I> idsToExclude = elementsFoundInCache ? cachedElements.keySet() : Lists.newArrayList(getMaxValueForIdentifier());

            LOGGER.info("@@@ >>> From Cache {}, these elements {} were avoding them fetching again", cacheName, idsToExclude);
            arguments.add(idsToExclude);
            Collection elementsFormDb = (Collection) retrieve.apply(jpaRepository(), findMethodName, arguments.toArray());

            if(!CollectionUtils.isEmpty(elementsFormDb)){
                Map<I, C> dbElementMap = convertCollectionToMap(elementsFormDb);
                LOGGER.info("### From DataBase, these elements with Ids {} were fetched, hence adding them to the final cache", dbElementMap.keySet());
                allElements.putAll(dbElementMap);

                Lock cacheLock = lockCache(nativeCache, keyForCacheEntry);
                if(Objects.nonNull(cacheLock)){
                    cacheLock.lock();
                }
                try{
                    if (cacheSpecificEntryCheck(nativeCache, keyForCacheEntry)) {
                        LOGGER.info("@@@ Replacing the existing Cache Value Key {} with latest results", keyForCacheEntry);
                        cacheSpecificEntryReplacement(nativeCache, keyForCacheEntry, allElements);
                    } else {
                        LOGGER.info("@@@ Creating an entry for fresh Cache Valye Key {} with results", keyForCacheEntry);
                        cacheSpecificEntryCreation(nativeCache, keyForCacheEntry, allElements);
                    }
                } finally {
                    if(Objects.nonNull(cacheLock)){
                        cacheLock.unlock();
                    }
                }
                LOGGER.info("@@@ Since not all elements were found in the cache {}, hence adding them to the cache, for the cache-value-key {}", cacheName, keyForCacheEntry);
            } else {
                LOGGER.info("+++ No records were needed to be retrieved from DB, since all elements were found in the cache {}, for the cache-value-key {}", cacheName, keyForCacheEntry);
            }
        } else {
            LOGGER.error("@@@ Cache is not found or created for the cache name {}", cacheName);
        }
        return allElements;
    }

    default Function<Object, String> serializer(){
        return (dto) -> OBJECT_TO_JSON_CONVERTER.apply(dto);
    }

    Supplier<TypeReference> typeReferenceSupplier();
    default Function<String, C> deSerializer(){
        return  (json) -> (C)JSON_STRING_TO_OBJECT_CONVERTER.apply(json, typeReferenceSupplier());
    }


}


