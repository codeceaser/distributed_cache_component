package com.example.services;

import com.example.annotations.FetchAndRefreshCache;
import com.example.annotations.RefreshCache;
import com.example.cache.api.AbstractCacheRefreshStrategy;
import com.example.dto.UserDTO;
import com.example.entities.User;
import com.example.repositories.UserRepository;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CachePeekMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.cache.Cache;
import javax.transaction.Transactional;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService implements IUserService{

    public static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    @Autowired
    private UserRepository userRepository;
    @Value("${indicator}")
    private String indicator;
    public static String prop1;
    @Autowired(
            required = false
    )
    Ignite ignite;
    public static final Function<Collection<User>, Map<Long, UserDTO>> CACHE_MAP_CONVERTER = (users) -> {
        Map<Long, UserDTO> userMap = (Map)users.stream().map(UserDTO::new).collect(Collectors.toConcurrentMap(UserDTO::getId, Function.identity(), (existing, newer) -> {
            return newer;
        }));
        return userMap;
    };

    public UserService() {
    }

    @Value("${property1}")
    public void setProp1(String p1) {
        prop1 = p1;
    }

    public String getProp1() {
        return prop1;
    }

    public Collection<UserDTO> findAll() {
        LOGGER.info("Indicator Value is : {} and prop1 is: {}", this.indicator, prop1);
        return (Collection)this.userRepository.findAll().stream().map(UserDTO::new).collect(Collectors.toList());
    }

    @FetchAndRefreshCache(
            cacheName = "usersByLocation",
            repositoryMethod = "findByLocationAndIdNotIn"
    )
    public Map<Long, UserDTO> findByLocation(String location) {
        Map<Long, UserDTO> results = Maps.newHashMap();
        return results;
    }

    public String printCaches() {
        Collection<String> cacheNames = Lists.newArrayList(this.ignite.cacheNames());
        if (CollectionUtils.isEmpty(cacheNames)) {
            cacheNames = Lists.newArrayList();
            cacheNames.add("usersByLocation");
            cacheNames.add("usersByDepartment");
            cacheNames.add("usersByLocationAndDepartment");
        }

        LOGGER.info("### Cache Names are: {} ###", cacheNames);
        cacheNames.stream().forEach((cacheName) -> {
            LOGGER.info("### Cache Name is: {} ###", cacheName);
            Cache cache = this.ignite.cache(cacheName);
            if (cache != null) {
                LOGGER.info("Size of Cache {} is: {}", cacheName, ((IgniteCache)cache).size(new CachePeekMode[0]));
            }

        });
        return "Ok";
    }

    public String clearCaches() {
        Collection<String> cacheNames = Lists.newArrayList(this.ignite.cacheNames());
        if (CollectionUtils.isEmpty(cacheNames)) {
            cacheNames = Lists.newArrayList();
            cacheNames.add("usersByLocation");
            cacheNames.add("usersByDepartment");
            cacheNames.add("usersByLocationAndDepartment");
        }

        LOGGER.info("### Cache Names are: {} ###", cacheNames);
        cacheNames.stream().forEach((cacheName) -> {
            LOGGER.info("### Cache Name is: {} ###", cacheName);
            Cache cache = this.ignite.cache(cacheName);
            if (cache != null) {
                IgniteCache igniteCache = (IgniteCache) AbstractCacheRefreshStrategy.extractIgniteCache.apply(cache);
                igniteCache.clear();
                LOGGER.info("### Cache {} Cleared ###", cacheName);
            }

        });
        return "Ok";
    }

    @FetchAndRefreshCache(
            cacheName = "usersByDepartment",
            repositoryMethod = "findByDepartmentAndIdNotIn"
    )
    public Map<Long, UserDTO> findByDepartment(String department) {
        Map<Long, UserDTO> results = Maps.newHashMap();
        return results;
    }

    @FetchAndRefreshCache(
            cacheName = "usersByLocationAndDepartment",
            repositoryMethod = "findByLocationAndDepartmentAndIdNotIn"
    )
    public Map<Long, UserDTO> findByLocationAndDepartment(String location, String department) {
        Map<Long, UserDTO> results = Maps.newHashMap();
        return results;
    }

    public UserDTO findById(Long id) {
        return (UserDTO)this.userRepository.findById(id).map(UserDTO::new).orElseGet(() -> {
            return null;
        });
    }

    @RefreshCache(
            cacheNames = {"usersByLocation", "usersByDepartment", "usersByLocationAndDepartment"}
    )
    public UserDTO save(User user) {
        UserDTO saved = new UserDTO((User)this.userRepository.save(user));
        return saved;
    }

    @RefreshCache(
            cacheNames = {"usersByLocation", "usersByDepartment", "usersByLocationAndDepartment"}
    )
    public UserDTO create(User user) {
        UserDTO saved = new UserDTO((User)this.userRepository.save(user));
        return saved;
    }

    @RefreshCache(
            cacheNames = {"usersByLocation", "usersByDepartment", "usersByLocationAndDepartment"},
            isDelete = "Y"
    )
    public void deleteById(Long id) {
        this.userRepository.deleteById(id);
    }
}
