package com.example.components;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.springdata.repository.support.IgniteRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.expiry.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.cache.api.AbstractCacheRefreshStrategy.createIndexCacheForIgnite;

public abstract class AbstractIgniteRepositoryImpl<C extends Cacheable, I> extends IgniteRepositoryImpl  {

    private Logger LOGGER = LoggerFactory.getLogger(AbstractIgniteRepositoryImpl.class);

    protected Ignite ignite;
    
    private String cacheName;
    public String cacheName(){
        return cacheName;
    }

    private Class<C> clazz;
    
    public Class<C> clazz(){
        return clazz;
    }
    public AbstractIgniteRepositoryImpl(Ignite ignite, String cacheName, Class clazz) {
        super(ignite.cache(cacheName));
        this.cacheName = cacheName;
        this.ignite = ignite;
        this.clazz = clazz;
    }

    @Override
    public void deleteAll(Iterable entities) {
        IgniteCache<I, C> cache = ignite.cache(cacheName());
        cache.removeAll(Sets.newHashSet(entities));
    }

    @Override
    public Collection<C> saveAll(Iterable entities) {
        Collection<C> toBeSaved = Lists.newArrayList(entities);
        Map<I, C> objectMap = (Map)toBeSaved.stream().collect(Collectors.toConcurrentMap(c -> c.getId(), Function.identity(), (existing, newer) -> newer));
        IgniteCache<I, C> cache = ignite.cache(cacheName());
        if (Objects.isNull(cache)) {
            cache = (IgniteCache<I, C>) createIndexCacheForIgnite(ignite, cacheName(), Duration.ONE_DAY, clazz());
        }
        cache.putAll(objectMap);
        return toBeSaved;
    }

    @Override
    public Optional<C> findById(Object id) {
        IgniteCache<I, C> cache = ignite.cache(cacheName());
        return Optional.ofNullable(cache.get((I)id));
    }

    @Override
    public boolean existsById(Object id) {
        return findById(id).isPresent();
    }

    @Override
    public Iterable<C> findAllById(Iterable ids) {
        IgniteCache<I, C> cache = ignite.cache(cacheName());
        Collection<C> values = cache.getAll(Sets.newHashSet(ids)).values();
        return values;
    }

    @Override
    public void deleteById(Object id) {
        IgniteCache<I, C> cache = ignite.cache(cacheName());
        cache.remove((I)id);
    }

    @Override
    public void deleteAllById(Iterable ids) {
        IgniteCache<I, C> cache = ignite.cache(cacheName());
        cache.removeAll(Sets.newHashSet(ids));
    }
}
