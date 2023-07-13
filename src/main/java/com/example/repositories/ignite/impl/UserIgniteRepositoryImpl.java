package com.example.repositories.ignite.impl;

import com.example.dto.UserDTO;
import com.example.repositories.ignite.UserIgniteRepository;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.SqlQuery;
import org.apache.ignite.springdata.repository.support.IgniteRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.cache.Cache;
import javax.cache.expiry.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.cache.api.AbstractCacheRefreshStrategy.createIndexCacheForIgnite;

//@RepositoryConfig(cacheName = "indexed-user")
@Service
@ConditionalOnProperty(name = "caching.solution", havingValue = "indexed-ignite")
public class UserIgniteRepositoryImpl extends IgniteRepositoryImpl<UserDTO, Long> implements UserIgniteRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(UserIgniteRepositoryImpl.class);

    Ignite ignite;

    public UserIgniteRepositoryImpl(Ignite ignite) {
        super(ignite.cache("indexed-user"));
        this.ignite = ignite;
    }

    @Override
    public Collection<UserDTO> findByLocation(String location) {
        IgniteCache<Long, UserDTO> cache = ignite.cache("indexed-user");
        String sql = "location = ?";
        SqlQuery<Long, UserDTO> query = new SqlQuery<>(UserDTO.class, sql);
        query.setArgs(location);
        List<Cache.Entry<Long, UserDTO>> users = cache.query(query).getAll();
        List<UserDTO> userDTOS = users.stream()
                .map(Cache.Entry::getValue)
                .collect(Collectors.toList());
        LOGGER.info("Found {} users for location {}", userDTOS, location);
        return userDTOS;
    }

    @Override
    public Collection<UserDTO> findByDepartment(String department) {
        IgniteCache<Long, UserDTO> cache = ignite.cache("indexed-user");
        String sql = "department = ?";
        SqlQuery<Long, UserDTO> query = new SqlQuery<>(UserDTO.class, sql);
        query.setArgs(department);
        List<Cache.Entry<Long, UserDTO>> users = cache.query(query).getAll();
        return users.stream()
                .map(Cache.Entry::getValue)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<UserDTO> findByLocationAndDepartment(String location, String department) {
        IgniteCache<Long, UserDTO> cache = ignite.cache("indexed-user");
        String sql = "location = ? AND department = ?";
        SqlQuery<Long, UserDTO> query = new SqlQuery<>(UserDTO.class, sql);
        query.setArgs(location, department);
        List<Cache.Entry<Long, UserDTO>> users = cache.query(query).getAll();
        return users.stream()
                .map(Cache.Entry::getValue)
                .collect(Collectors.toList());
    }
    @Override
    public void deleteAll(Iterable entities) {
        IgniteCache<Long, UserDTO> cache = ignite.cache("indexed-user");
        cache.removeAll(Sets.newHashSet(entities));
    }

    @Override
    public Collection<UserDTO> saveAll(Iterable entities) {
        Collection<UserDTO> toBeSaved = Lists.newArrayList(entities);
        Map<Long, UserDTO> userMap = (Map)toBeSaved.stream().collect(Collectors.toConcurrentMap(UserDTO::getId, Function.identity(), (existing, newer) -> newer));
        IgniteCache<Long, UserDTO> cache = ignite.cache("indexed-user");
        if (Objects.isNull(cache)) {
            cache = (IgniteCache<Long, UserDTO>) createIndexCacheForIgnite(ignite, "indexed-user", Duration.ONE_DAY, UserDTO.class);
        }
        cache.putAll(userMap);
        return toBeSaved;
    }

    @Override
    public Optional<UserDTO> findById(Long id) {
        IgniteCache<Long, UserDTO> cache = ignite.cache("indexed-user");
        return Optional.ofNullable(cache.get(id));
    }

    @Override
    public boolean existsById(Long id) {
        return findById(id).isPresent();
    }

    @Override
    public Iterable<UserDTO> findAllById(Iterable<Long> ids) {
        IgniteCache<Long, UserDTO> cache = ignite.cache("indexed-user");
        Collection<UserDTO> values = cache.getAll(Sets.newHashSet(ids)).values();
        return values;
    }

    @Override
    public void deleteById(Long id) {
        IgniteCache<Long, UserDTO> cache = ignite.cache("indexed-user");
        cache.remove(id);
    }

    @Override
    public void deleteAllById(Iterable<? extends Long> ids) {
        IgniteCache<Long, UserDTO> cache = ignite.cache("indexed-user");
        cache.removeAll(Sets.newHashSet(ids));
    }



}
