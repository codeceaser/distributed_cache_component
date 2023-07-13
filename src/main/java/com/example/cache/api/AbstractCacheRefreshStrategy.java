package com.example.cache.api;

import com.example.components.Cacheable;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.function.TriFunction;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.*;
import org.apache.ignite.cache.query.annotations.QuerySqlField;
import org.apache.ignite.configuration.CacheConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;

import static com.example.conf.ApplicationConfiguration.cachingSolution;
import static com.example.utils.CommonUtil.INDEXED_FIELDS_EXTRACTOR;

/**
 *
 * @param <K>
 * @param <C>
 * @param <I>
 */
public abstract class AbstractCacheRefreshStrategy<K, C extends Cacheable, I> implements CacheRefreshStrategy<K, C, I>, InvertedIndicesRefreshStrategy<K, C, I> {

    public static final Logger LOGGER = LoggerFactory.getLogger(AbstractCacheRefreshStrategy.class);

    @Autowired(required = false)
    CacheManager cacheManager;

    @Autowired(required = false)
    Ignite ignite;

    public static Function<Cache, IgniteCache> extractIgniteCache = (cache) -> (IgniteCache) cache;

    public static final TriFunction<Ignite, String, Duration, CacheConfiguration> CREATE_IGNITE_CACHE_CONFIGURATION = (ignite, cacheName, duration) -> {
        CacheConfiguration cacheCfg = new CacheConfiguration<>();
        cacheCfg.setName(cacheName);
        cacheCfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        cacheCfg.setCacheMode(CacheMode.PARTITIONED);
        cacheCfg.setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(duration));
        return cacheCfg;
    };

    public static final Function<Class, Collection> CREATE_IGNITE_INDICES = (clazz) -> {
        Set<Field> indexedFields = INDEXED_FIELDS_EXTRACTOR.apply(clazz);
        if(indexedFields.isEmpty()){
            return Collections.emptyList();
        }
        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        LinkedHashMap<String, Boolean> idxFields = new LinkedHashMap<>();
        indexedFields.forEach(field ->{
            fields.put(field.getName(), field.getType().getName());
            QuerySqlField querySqlField = field.getAnnotation(QuerySqlField.class);
            idxFields.put(field.getName(), querySqlField.index());
        });
        QueryEntity qryEntity = new QueryEntity();
        Class<?> returnType = getIdFieldType(clazz);
        qryEntity.setKeyType(returnType.getName());
        qryEntity.setValueType(clazz.getName());
        qryEntity.setIndexes(Collections.singletonList(new QueryIndex(idxFields, QueryIndexType.SORTED)));
        return Collections.singletonList(qryEntity);
    };

    private static Class<?> getIdFieldType(Class clazz) {
        Method getId = null;
        try {
            getId = clazz.getMethod("getId", null);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        Class<?> returnType = getId.getReturnType();
        return returnType;
    }

    public static Cache createIndexCacheForIgnite(Ignite ignite, String cacheName, Duration duration, Class clazz) {
        LOGGER.info("@@@ - {} - Creating cache with name: {}", cachingSolution(), cacheName);
        CacheConfiguration cacheCfg = CREATE_IGNITE_CACHE_CONFIGURATION.apply(ignite, cacheName, duration);
        Class<?> returnType = getIdFieldType(clazz);
        cacheCfg.setIndexedTypes(returnType, clazz);
        Collection queryEntities = CREATE_IGNITE_INDICES.apply(clazz);
        if(!queryEntities.isEmpty()){
            cacheCfg.setQueryEntities(queryEntities);
            LOGGER.info("@@@ - {} - Creating cache with name: {} and indices: {}", cachingSolution(), cacheName, queryEntities);
        }
        Cache nativeCache = ignite.getOrCreateCache(cacheCfg);
        if (Objects.isNull(nativeCache)) {
            LOGGER.info("@@@ - {} - CACHE NOT CREATED!!! {}", cachingSolution(), cacheName);
        }
        return nativeCache;
    }

    @Override
    public final Cache createCacheIfAbsent(String cacheName) {
        LOGGER.info("@@@ - {} - If not present, creating cache with name: {}", cachingSolution(), cacheName);
        if(StringUtils.equalsIgnoreCase("ignite", cachingSolution())){
            TypeReference typeReference = typeReferenceSupplier().get();
            Type type = typeReference.getType();
            return createIndexCacheForIgnite(ignite, cacheName, Duration.FIVE_MINUTES, (Class) type);
        } else if(StringUtils.equalsIgnoreCase("ehcache", cachingSolution())){
            MutableConfiguration<String, Map> mutableConfiguration = new MutableConfiguration<String, Map>()
                    .setTypes(String.class, Map.class)
                    .setStoreByValue(false)
                    .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.FIVE_MINUTES));
            Cache<String, Map> jCache = cacheManager.createCache(cacheName, mutableConfiguration);
            return jCache;
        }
        return null;
    }
    @Override
    public final Cache retrieveCache(String cacheName) {
        LOGGER.info("@@@ - {} - Retrieving cache with name: {}", cachingSolution(), cacheName);
        if(StringUtils.equalsIgnoreCase("ignite", cachingSolution())){
            if(ignite.cacheNames().contains(cacheName)){
                return ignite.cache(cacheName);
            } else {
                return null;
            }
        } else if(StringUtils.equalsIgnoreCase("ehcache", cachingSolution())){
            return cacheManager.getCache(cacheName);
        }
        return null;
    }

    @Override
    public final Cache extractNativeCache(Cache cache, Object keyForExistingCache) {
        if(StringUtils.equalsIgnoreCase("ignite", cachingSolution())){
            return extractIgniteCache.apply(cache);
        } else if(StringUtils.equalsIgnoreCase("ehcache", cachingSolution())){
            return cache;
        }
        return null;
    }

    @Override
    public final Map extractCacheMapUsingKey(Cache cache, Object keyForExistingCache) {
        LOGGER.info("@@@ - {} - From Cache {}, Extracting cache map using key: {}", cachingSolution(), cache.getName(), keyForExistingCache);
        Map cacheMap = (Map) cache.get(keyForExistingCache);
        if(StringUtils.equalsIgnoreCase("ignite", cachingSolution())){
            return cacheMap;
        } else if(StringUtils.equalsIgnoreCase("ehcache", cachingSolution())){
            return cacheMap;
        }
        return null;
    }

    @Override
    public final Lock lockCache(Cache cache, Object keyForExistingCache) {
        if(StringUtils.equalsIgnoreCase("ignite", cachingSolution())){
            return extractIgniteCache.apply(cache).lock(keyForExistingCache);
        } else if(StringUtils.equalsIgnoreCase("ehcache", cachingSolution())){
            return null;
        }
        return null;
    }

    @Override
    public final void cacheSpecificEntryReplacement(Cache cache, Object keyForExistingCache, Map cacheMap) {
        LOGGER.info("@@@ - {} - For Cache {}, Replacing cache map using key: {}", cachingSolution(), cache.getName(), keyForExistingCache);
        if(StringUtils.equalsIgnoreCase("ignite", cachingSolution())){
            extractIgniteCache.apply(cache).replace(keyForExistingCache, cacheMap);
        } else if(StringUtils.equalsIgnoreCase("ehcache", cachingSolution())){
            cache.put(keyForExistingCache, cacheMap);
        }
    }

    @Override
    public final void cacheSpecificEntryCreation(Cache cache, Object keyForCacheEntry, Map cacheMap) {
        LOGGER.info("@@@ - {} - For Cache {}, Creating cache map using key: {}", cachingSolution(), cache.getName(), keyForCacheEntry);
        if(StringUtils.equalsIgnoreCase("ignite", cachingSolution())){
            extractIgniteCache.apply(cache).put(keyForCacheEntry, cacheMap);
        } else if(StringUtils.equalsIgnoreCase("ehcache", cachingSolution())){
            cache.put(keyForCacheEntry, cacheMap);
        }
    }

    @Override
    public final boolean cacheSpecificEntryCheck(Cache cache, Object keyForCacheEntry) {
        LOGGER.info("@@@ - {} - For Cache {}, Checking cache map using key: {}", cachingSolution(), cache.getName(), keyForCacheEntry);
        if(StringUtils.equalsIgnoreCase("ignite", cachingSolution())){
            return extractIgniteCache.apply(cache).containsKey(keyForCacheEntry);
        } else if(StringUtils.equalsIgnoreCase("ehcache", cachingSolution())){
            return cache.containsKey(keyForCacheEntry);
        }
        return false;
    }

    @Override
    public final C removeObjectFromCache(C existingObject, Cache cachesPresent) {
        return CacheRefreshStrategy.super.removeObjectFromCache(existingObject, cachesPresent);
    }

    @Override
    public final C processEvictionFromExistingCache(C existingObject, C newerObject, Cache cachesPresent) {
        return CacheRefreshStrategy.super.processEvictionFromExistingCache(existingObject, newerObject, cachesPresent);
    }

    @Override
    public final C updateCache(C newerObject, Cache cachesPresent) {
        return CacheRefreshStrategy.super.updateCache(newerObject, cachesPresent);
    }

    @Override
    public final Map<I, C> findElementsAndRefreshCache(String cacheName, List arguments, String findMethodName) {
        LOGGER.info(" XXXXXXXXXX - {} - For Cache {}, Finding elements and refreshing cache using arguments: {}", cachingSolution(), cacheName, arguments);
        if(StringUtils.equalsIgnoreCase("elastic", cachingSolution()) || StringUtils.equalsIgnoreCase("indexed-ignite", cachingSolution())){
            return InvertedIndicesRefreshStrategy.super.findElementsAndRefreshCache(cacheName, arguments, findMethodName);
        } else {
            return CacheRefreshStrategy.super.findElementsAndRefreshCache(cacheName, arguments, findMethodName);
        }
    }

    @Override
    public final C refreshCache(C existingObject, C newerObject, String isDelete) {
        Type type = null;
        if(StringUtils.equalsIgnoreCase("elastic", cachingSolution()) || StringUtils.equalsIgnoreCase("indexed-ignite", cachingSolution())){
            return InvertedIndicesRefreshStrategy.super.refreshCache(existingObject, newerObject, isDelete);
        } else {
            return CacheRefreshStrategy.super.refreshCache(existingObject, newerObject, isDelete);
        }

    }

    @Override
    public final Function<Object, String> serializer() {
        return CacheRefreshStrategy.super.serializer();
    }

    @Override
    public final Function<String, C> deSerializer() {
        return CacheRefreshStrategy.super.deSerializer();
    }
}
