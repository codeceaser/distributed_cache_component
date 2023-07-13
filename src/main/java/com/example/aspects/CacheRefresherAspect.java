package com.example.aspects;

import com.example.annotations.FetchAndRefreshCache;
import com.example.annotations.RefreshCache;
import com.example.annotations.RefreshCacheDTO;
import com.example.cache.api.CacheRefreshStrategy;
import com.example.cache.api.RefreshStrategy;
import com.example.components.ApplicationContextProvider;
import com.example.components.Cacheable;
import com.example.dto.CacheRefreshDTO;
import com.example.services.KafkaCacheRefreshProducer;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.function.TriFunction;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Aspect
public class CacheRefresherAspect {

    public static final Logger LOGGER = LoggerFactory.getLogger(CacheRefresherAspect.class);


    private static String cachingSolution;

    @Value("${caching.solution}")
    public void setCachingSolution(String cachingSolutionVal) {
        cachingSolution = cachingSolutionVal;
    }

    @Autowired(required = false)
    KafkaCacheRefreshProducer kafkaCacheRefreshProducer;

    @Pointcut("@annotation(com.example.annotations.RefreshCache)")
    public void refreshCacheAnnotateMethods(){}

    @Pointcut("@annotation(com.example.annotations.FetchAndRefreshCache)")
    public void fetchAndRefreshCacheAnnotationMethods(){}

    @Pointcut("within(com.example..services..*)")
    public void withinServiceLayer(){}

    public static final TriFunction<Class, MethodSignature, Class, Object> EXTRACT_ANNOTATION_METADATA = (aClass, signature, annotationClass) -> {
        final Method method = signature.getMethod();
        String methodName = signature.getName();
        try {
            return aClass.getDeclaredMethod(methodName, method.getParameterTypes()).getAnnotation(annotationClass);
        } catch (NoSuchMethodException e) {
            LOGGER.error("RefreshCache Configuration Error, Method {} is missing the annotation {}", methodName, annotationClass.getSimpleName());
        }
        return null;
    };

    @Around(value = "withinServiceLayer() && fetchAndRefreshCacheAnnotationMethods()")
    public Object fetchAndRefreshCache(ProceedingJoinPoint joinPoint) {
        Class<?> aClass = joinPoint.getTarget().getClass();
        String className = aClass.getSimpleName();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getName();
        LOGGER.info("Intercepting [{}#{}] for Refreshing the Cache", className, methodName);
        Object result = null;

        try{
            result = joinPoint.proceed();
        } catch (Throwable throwable) {
            LOGGER.error("Error while executing the method {}#{}", className, methodName, throwable);
            throw new RuntimeException(throwable);
        }

        List arguments = Optional.ofNullable(joinPoint.getArgs()).map(args -> Lists.newArrayList(args)).orElseGet(Lists::newArrayList);
        FetchAndRefreshCache fetchAndRefreshCache = (FetchAndRefreshCache) EXTRACT_ANNOTATION_METADATA.apply(aClass, signature, FetchAndRefreshCache.class);
        if(Objects.nonNull(fetchAndRefreshCache)) {
            RefreshStrategy cacheRefreshStrategy = (RefreshStrategy) ApplicationContextProvider.getBeanUsingQualifier(RefreshStrategy.class, fetchAndRefreshCache.cacheName());
            if(Objects.nonNull(cacheRefreshStrategy)){
                LOGGER.info("Fetch and Refresh Cache Strategy found for {}", fetchAndRefreshCache.cacheName());
                Map refreshedElementsFromCache = cacheRefreshStrategy.findElementsAndRefreshCache(fetchAndRefreshCache.cacheName(), arguments, fetchAndRefreshCache.repositoryMethod());
                if (Map.class.isAssignableFrom(result.getClass())) {
                    ((Map) result).putAll(refreshedElementsFromCache);
                    LOGGER.info("Fetch And Refresh Strategy brought {} elements from the cache", refreshedElementsFromCache.size());
                }
            }
        } else {
            LOGGER.error("No Fetch and Refresh Cache Annotation Found");
        }
        return result;
    }

    @Around(value = "withinServiceLayer() && refreshCacheAnnotateMethods()")
    public Object refreshCaches(ProceedingJoinPoint joinPoint) {
        Object savedObject = null;
        Class<?> aClass = joinPoint.getTarget().getClass();
        String className = aClass.getSimpleName();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getName();
        LOGGER.info("Intercepting [{}#{}] for Refreshing the Cache", className, methodName);
        Object existingObject = null;
        RefreshCache refreshCache = (RefreshCache) EXTRACT_ANNOTATION_METADATA.apply(aClass, signature, RefreshCache.class);
        List<Object> arguments = Optional.ofNullable(joinPoint.getArgs()).map(args -> Lists.newArrayList(args)).orElseGet(Lists::newArrayList);
        if (Objects.nonNull(refreshCache)) {
            existingObject = retrieveExistingObject(arguments, refreshCache);
        } else {
            LOGGER.error("No Refresh Cache Annotation Found");
        }

        try {
            savedObject = joinPoint.proceed();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        if(StringUtils.equalsIgnoreCase("ehcache", cachingSolution)){
            LOGGER.info("**** Going to send the Event to Kafka for Cache Refresh ****");
            CacheRefreshDTO cacheRefreshDTO = new CacheRefreshDTO(existingObject, savedObject, new RefreshCacheDTO(refreshCache));
            kafkaCacheRefreshProducer.send("cache-refresh", cacheRefreshDTO);
            LOGGER.info("**** Event Sent to Kafka for Cache Refresh ****");
        } else {
            LOGGER.info("**** Going to Refresh Cache ****");
            refreshCaches(existingObject, savedObject, refreshCache);
        }
        return savedObject;
    }

    public static void refreshCaches(Object existingObject, Object savedObject, RefreshCache refreshCache) {
        if (Objects.nonNull(refreshCache)) {
            /*Stream<CompletableFuture<Object>> futureStream = Arrays.stream(refreshCache.cacheNames()).map(cacheName -> supplyAsync(() -> {
                LOGGER.info("Refreshing the Cache : {}", cacheName);
                CacheRefreshStrategy cacheRefreshStrategy = (CacheRefreshStrategy) ApplicationContextProvider.getBeanUsingQualifier(CacheRefreshStrategy.class, cacheName);
                Object replacedObject = cacheRefreshStrategy.refreshCache(existing, newer, refreshCache.isDelete());
                LOGGER.info("Object {} replaced {} in Cache {}", newer, existing, cacheName);
                return replacedObject;
            })).map((future) -> future.whenComplete((evictedObject, exception) -> {
                if (Objects.nonNull(exception)) {
                    LOGGER.error("Error while refreshing cache ", exception);
                } else {
                    LOGGER.info("As a result of Cache Refresh, object {} was evicted", evictedObject);
                }
            }));
            futureStream.collect(Collectors.toList());*/
            boolean typeConversionRequired = true;
            for (String cacheName : refreshCache.cacheNames()) {
                LOGGER.info("Refreshing the Cache : {}", cacheName);
                RefreshStrategy cacheRefreshStrategy = (RefreshStrategy) ApplicationContextProvider.getBeanUsingQualifier(RefreshStrategy.class, cacheName);
                Type type = null;
                if(StringUtils.equalsIgnoreCase("ehcache", cachingSolution) && typeConversionRequired){
                    CacheRefreshStrategy ehCacheRefreshStrategy = (CacheRefreshStrategy) cacheRefreshStrategy;
                    if (Objects.nonNull(existingObject)) {
                        existingObject = ehCacheRefreshStrategy.deSerializer().apply(ehCacheRefreshStrategy.serializer().apply(existingObject));
                        type = existingObject.getClass();
                    }
                    if(Objects.nonNull(savedObject)){
                        savedObject = ehCacheRefreshStrategy.deSerializer().apply(ehCacheRefreshStrategy.serializer().apply(savedObject));
                        type = savedObject.getClass();
                    }
                    typeConversionRequired = false;
                    LOGGER.info("@#$ Type of the Cacheable Object is: {}", type);
                }

                Object replacedObject = cacheRefreshStrategy.refreshCache((Cacheable) existingObject, (Cacheable) savedObject, refreshCache.isDelete());
                LOGGER.info("Object {} replaced {} in Cache {}", savedObject, existingObject, cacheName);
                if(StringUtils.equalsIgnoreCase("elastic", cachingSolution)){
                    break;
                }
            }
        }
    }

    public static Object retrieveExistingObject(List<Object> arguments, RefreshCache refreshCache) {
        Object existingObject = null;
        Object id = extractIdFormArguments(arguments, refreshCache);
        if (Objects.nonNull(id)) {
            for (String cacheName : refreshCache.cacheNames()) {
                RefreshStrategy cacheRefreshStrategy = (RefreshStrategy) ApplicationContextProvider.getBeanUsingQualifier(RefreshStrategy.class, cacheName);
                if (Objects.isNull(existingObject) && Objects.nonNull(id)) {
                    existingObject = cacheRefreshStrategy.getExistingObjectByIdentifier(id);
                    break;
                }
            }
        }
        if (Objects.nonNull(existingObject)) {
            LOGGER.info("Existing Object {} is found", existingObject);
        } else {
            LOGGER.info("No Existing Object is found with Id {}", id);
        }
        return existingObject;
    }

    public static Object extractIdFormArguments(List<Object> arguments, RefreshCache refreshCache) {
        Object id = null;
        if (StringUtils.equalsIgnoreCase("N", refreshCache.isDelete())) {
            Cacheable objectToBeSaved = arguments.stream().filter(arg -> Cacheable.class.isAssignableFrom(arg.getClass())).map(arg -> (Cacheable)arg).findFirst().orElseGet(() -> null);
            if (Objects.nonNull(objectToBeSaved) && Objects.nonNull(objectToBeSaved.getId())) {
                id = objectToBeSaved.getId();
            } else {
                LOGGER.error("No Cacheable Object is being Saved or it does not have the id");
            }
        } else {
            id = arguments.stream().findFirst().orElseGet(() -> null);
        }
        return id;
    }

}
