package com.example.repositories.api;

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
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.example.utils.CommonUtil.retrieve;

public interface IndicesBasedWrapperRepository<I, C extends Cacheable, O> {

    Logger LOGGER = LoggerFactory.getLogger(IndicesBasedWrapperRepository.class);

    Supplier<TypeReference> typeReferenceSupplier();

    JpaRepository jpaRepository();


    I getMaxValueForIdentifier();

    String cacheName();

    Map<I,C> convertCollectionToMap(Collection elements);

    C getExistingObjectByIdentifier(Object id);

    void createInvertedIndicesAndCacheEntries(Collection<C> newerObjects);

    Cache<I,C> retrieveCache();

    InvertedIndicesRepository invertedIndicesRepository();

    default Map<I,C> findByAttributesAndRefreshInvertedIndicesAndCache(List arguments, String findMethodName){
//        arguments.add(getMaxValueForIdentifier());
        Collection<C> invertedIndicesSearchResults = (Collection<C>) retrieve.apply(invertedIndicesRepository(), StringUtils.replace(findMethodName, "AndIdNotIn", ""), arguments.toArray());
        LOGGER.info("@@@ >>> From InvertedIndicesAndCache, these elements {} were fetched", invertedIndicesSearchResults);
        Map<I, C> mapFromUsingInvertedIndices = null;
        Map<I, C> mapFromCache = null;
        if(!CollectionUtils.isEmpty(invertedIndicesSearchResults)){
            mapFromUsingInvertedIndices = (Map<I, C>) invertedIndicesSearchResults.stream().collect(Collectors.toMap(c -> c.getId(), Function.identity(), (existing, newer) -> newer));
            Cache<I, C> cache = retrieveCache();
            if(Objects.nonNull(cache)){
                mapFromCache = cache.getAll(mapFromUsingInvertedIndices.keySet());
            }
        }
        boolean igniteMapEmpty = Objects.isNull(mapFromCache) || mapFromCache.isEmpty();
        LOGGER.info("@@@ >>> From Cache {}, these elements {} were fetched", cacheName(), Optional.ofNullable(mapFromCache).map(String::valueOf).orElseGet(() -> "Empty"));
        Collection<I> idsToExclude = !igniteMapEmpty ? mapFromCache.keySet() : Lists.newArrayList(getMaxValueForIdentifier());

        LOGGER.info("@@@ >>> From Cache {}, these elements {} were fetched hence avoding them fetching again", cacheName(), idsToExclude);
        arguments.add(idsToExclude);
//        Collection objectsFromDB = (Collection) retrieve.apply(jpaRepository(), findMethodName, arguments.toArray());
        Collection<O> objectsFromDB = (Collection<O>) retrieve.apply(jpaRepository(), findMethodName, arguments.toArray());//jpaRepository().findByLocationAndIdNotIn(location, idsToExclude);
        Map<I, C> mapFromDB = Maps.newHashMap();
        if(!CollectionUtils.isEmpty(objectsFromDB)){
            mapFromDB = convertCollectionToMap(objectsFromDB);
        }
        LOGGER.info("@@@ >>> From DB, these elements {} were fetched", mapFromDB);
        if (igniteMapEmpty) {
            mapFromCache = Maps.newHashMap();
        }
        Set<I> dbIdToRefresh = Sets.newHashSet(mapFromDB.keySet());
        dbIdToRefresh.removeAll(mapFromCache.keySet());
        Map<I, C> mapThatNeedsToBeRefreshed = Maps.newHashMap(mapFromDB);
        mapThatNeedsToBeRefreshed.keySet().retainAll(dbIdToRefresh);
        LOGGER.info("@@@ >>> From DB, these elements {} are to be refreshed", mapThatNeedsToBeRefreshed);
        Map<I, C> finalMap = Maps.newConcurrentMap();
        finalMap.putAll(mapFromCache);
        finalMap.putAll(mapThatNeedsToBeRefreshed);

        createInvertedIndicesAndCacheEntries(mapThatNeedsToBeRefreshed.values());
        LOGGER.info("@@@ >>> From Cache {}, these elements {} were refreshed", cacheName(), finalMap);
        return finalMap;
    }

    void removeInvertedIndicesAndCacheEntries(I keyOfObjectToBeRemoved);

    C findOriginalObject(I keyOfNewerObject);


}
