package com.example.components;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Maps;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import javax.cache.expiry.Duration;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;

import static com.example.cache.api.AbstractCacheRefreshStrategy.createIndexCacheForIgnite;

public interface ElasticWrapperRepository<I, C extends Cacheable, O> extends IndicesBasedWrapperRepository<I, C, O> {

    Logger LOGGER = LoggerFactory.getLogger(ElasticWrapperRepository.class);

    RestHighLevelClient client();
    Ignite ignite();

    C convertInvertedIndicesMapToObject(Map<String, Object> map);

    Map<String, Object> convertObjectToInvertedIndicesMap(C object);

    default void createInvertedIndicesAndCacheEntries(Collection<C> newerObjects) {
        if(CollectionUtils.isEmpty(newerObjects)) {
            LOGGER.info("+++ It Looks like Everything is uptodate, no newer objects to create inverted indices and cache entries! +++");
            return;
        }
        BulkRequest bulkRequest = new BulkRequest();
        Map<I, C> cacheEntries = Maps.newConcurrentMap();
        LOGGER.info("Creating inverted indices and cache entries for {} objects", newerObjects.size());
        for (C newerObject : newerObjects) {
            I keyOfNewerCache = (I) newerObject.getId();
            //There is always a newer object from client and hence it is not null and we can always extract the id
            String id = String.valueOf(keyOfNewerCache);

            try {
                IndexRequest indexRequest = new IndexRequest(cacheName());
                indexRequest.id(id);
                indexRequest.source(convertObjectToInvertedIndicesMap(newerObject));
                bulkRequest.add(indexRequest);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Prepare for Ignite
            cacheEntries.put(keyOfNewerCache, newerObject);
        }

        // Make a bulk entry into ElasticSearch
        LOGGER.info("Making a bulk entry into ElasticSearch");
        try{
            BulkResponse bulkResponse = client().bulk(bulkRequest, RequestOptions.DEFAULT);
        }catch (IOException e) {
            LOGGER.error("Error while making a bulk entry into ElasticSearch", e);
            throw new RuntimeException(e);
        }

        LOGGER.info("Making a bulk entry into Ignite");
        // Make an entry into Ignite
        try{
            IgniteCache<I, C> cache = ignite().cache(cacheName());
            if (Objects.isNull(cache)) {
                TypeReference typeReference = typeReferenceSupplier().get();
                Type type = typeReference.getType();
                cache = (IgniteCache<I, C>) createIndexCacheForIgnite(ignite(), cacheName(), Duration.ONE_DAY, (Class) type);
            } else{
                Lock lock = cache.lockAll(cacheEntries.keySet());
                lock.lock();
                try {
                    cache.removeAll(cacheEntries.keySet());
                } finally {
                    lock.unlock();
                }
            }
            cache.putAll(cacheEntries);
        } catch (Exception e) {
            LOGGER.error("Error while making a bulk entry into Ignite", e);
            throw new RuntimeException(e);
        }
    }

    default Map<I,C> findByAttributesAndRefreshInvertedIndicesAndCache(List arguments, String findMethodName){
//        arguments.add(getMaxValueForIdentifier());
        return IndicesBasedWrapperRepository.super.findByAttributesAndRefreshInvertedIndicesAndCache(arguments, findMethodName);
    }

    default void removeInvertedIndicesAndCacheEntries(I keyOfObjectToBeRemoved) {
        LOGGER.info("From Cache {}, Removing inverted indices and cache entries for {} object", cacheName(), keyOfObjectToBeRemoved);
        try {
            DeleteRequest deleteRequest = new DeleteRequest(cacheName(), String.valueOf(keyOfObjectToBeRemoved));
            client().delete(deleteRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            LOGGER.error("Error while removing inverted indices for {} object", keyOfObjectToBeRemoved, e);
            throw new RuntimeException(e);
        }
        try{
            IgniteCache<I, C> cache = ignite().cache(cacheName());
            Lock lock = cache.lock(keyOfObjectToBeRemoved);
            try{
                lock.lock();
                cache.remove(keyOfObjectToBeRemoved);
            } finally {
                lock.unlock();
            }
        }catch (Exception e) {
            LOGGER.error("Error while removing cache entries for {} object", keyOfObjectToBeRemoved, e);
            throw new RuntimeException(e);
        }
        LOGGER.info("From Cache {}, Removed inverted indices and cache entries for {} object", cacheName(), keyOfObjectToBeRemoved);
    }

    default C findOriginalObject(I keyOfNewerObject){
        //There is always a newer object from client and hence it is not null and we can always extract the id
        String id = String.valueOf(keyOfNewerObject);
        C originalObject = null;
        try{
            //Using Cache Name and Id, we can get the original object from Elastic Search
            GetResponse getResponse = client().get(new GetRequest(cacheName(), id), RequestOptions.DEFAULT);
            if(Objects.isNull(getResponse) || !getResponse.isExists()){
                LOGGER.info("Document with id {} does not exist", id);
            } else {
                if(getResponse.isSourceEmpty()){
                    LOGGER.info("Document with id {} does not have any source", id);
                } else{
                    Map<String, Object> originalSource = getResponse.getSource();
                    LOGGER.info("Document with id {} has source {}", id, originalSource);
                    originalObject = convertInvertedIndicesMapToObject(originalSource);
                    LOGGER.info("Value of Original Object from Elastic Search {}", originalObject);
                }
            }
        }catch (IOException e) {
            LOGGER.error("Error while fetching inverted indices for id {}", id);
            throw new RuntimeException(e);
        }
        IgniteCache<I, C> cache = ignite().cache(cacheName());
        originalObject = (C) cache.get(keyOfNewerObject);
        if(!Objects.isNull(originalObject)){
            LOGGER.info("Value of Original Object from Ignite {}", originalObject);
        } else {
            originalObject = getExistingObjectByIdentifier(keyOfNewerObject);
            LOGGER.warn("Original Object was not found in Cache, hence Value of Original Object from Database {}", originalObject);
        }
        return originalObject;
    }



}
