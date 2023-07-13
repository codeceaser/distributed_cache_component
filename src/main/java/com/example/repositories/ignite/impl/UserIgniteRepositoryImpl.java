package com.example.repositories.ignite.impl;

import com.example.dto.UserDTO;
import com.example.components.AbstractIgniteRepositoryImpl;
import com.example.repositories.ignite.UserIgniteRepository;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.SqlQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.cache.Cache;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "caching.solution", havingValue = "indexed-ignite")
public class UserIgniteRepositoryImpl extends AbstractIgniteRepositoryImpl<UserDTO, Long> implements UserIgniteRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(UserIgniteRepositoryImpl.class);

    Ignite ignite;

    public UserIgniteRepositoryImpl(Ignite ignite) {
        super(ignite,"indexed-user", UserDTO.class);
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
        super.deleteAll(entities);
    }

    @Override
    public Collection<UserDTO> saveAll(Iterable entities) {
        return super.saveAll(entities);
    }

    @Override
    public Optional<UserDTO> findById(Object id) {
        return super.findById(id);
    }

    @Override
    public boolean existsById(Object id) {
        return super.existsById(id);
    }

    @Override
    public Iterable<UserDTO> findAllById(Iterable ids) {
        return super.findAllById(ids);
    }

    @Override
    public void deleteById(Object id) {
        super.deleteById(id);
    }

    @Override
    public void deleteAllById(Iterable ids) {
        super.deleteAllById(ids);
    }
}
