package com.example.repositories;

import com.example.dto.UserDTO;
import com.example.entities.User;
import com.example.repositories.api.ElasticWrapperRepository;
import com.example.repositories.api.InvertedIndicesRepository;
import com.example.repositories.elastic.UserElasticRepository;
import com.example.services.IUserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Maps;
import org.apache.ignite.Ignite;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import javax.cache.Cache;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

import static com.example.cache.user.AbstractUsersCacheRefreshStrategy.USER_DTO_TYPE_REFERENCE;
import static com.example.services.UserService.CACHE_MAP_CONVERTER;

@Service
@ConditionalOnProperty(name = "caching.solution", havingValue = "elastic")
public class UserElasticWrapperRepository implements ElasticWrapperRepository<Long, UserDTO, User> {

    @Autowired(required = false)
    private UserElasticRepository userElasticRepository;

    @Autowired(required = false)
    private RestHighLevelClient client;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    IUserService userService;

    @Autowired(required = false)
    Ignite ignite;

    @Override
    public RestHighLevelClient client() {
        return client;
    }

    @Override
    public JpaRepository jpaRepository() {
        return userJpaRepository;
    }

    @Override
    public Ignite ignite() {
        return ignite;
    }

    @Override
    public Long getMaxValueForIdentifier() {
        return Long.MAX_VALUE;
    }

    @Override
    public String cacheName() {
        return "indexed-user";
    }

    @Override
    public Map<Long, UserDTO> convertCollectionToMap(Collection elements) {
        return CACHE_MAP_CONVERTER.apply(elements);
    }

    @Override
    public Map<String, Object> convertObjectToInvertedIndicesMap(UserDTO userDTO) {
        Map<String, Object> userMap = Maps.newHashMap();
        userMap.put("id", userDTO.getId());
        userMap.put("name", userDTO.getName());
        userMap.put("email", userDTO.getEmail());
        userMap.put("location", userDTO.getLocation());
        userMap.put("department", userDTO.getDepartment());
        return userMap;
    }

    @Override
    public UserDTO convertInvertedIndicesMapToObject(Map<String, Object> map) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(Long.parseLong(String.valueOf(map.get("id"))));
        userDTO.setName((String) map.get("name"));
        userDTO.setEmail((String) map.get("email"));
        userDTO.setLocation((String) map.get("location"));
        userDTO.setDepartment((String) map.get("department"));
        return userDTO;
    }

    @Override
    public UserDTO getExistingObjectByIdentifier(Object id) {
        return userService.findById((Long) id);
    }

    @Override
    public Cache<Long, UserDTO> retrieveCache() {
        return ignite.cache("indexed-user");
    }

    @Override
    public InvertedIndicesRepository invertedIndicesRepository() {
        return userElasticRepository;
    }

    @Override
    public Supplier<TypeReference> typeReferenceSupplier() {
        return () -> {
            return USER_DTO_TYPE_REFERENCE;
        };
    }
}
