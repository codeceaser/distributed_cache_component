package com.example.cache.user;

import com.example.cache.api.AbstractCacheRefreshStrategy;
import com.example.dto.UserDTO;
import com.example.repositories.UserElasticWrapperRepository;
import com.example.repositories.UserIgniteWrapperRepository;
import com.example.repositories.UserJpaRepository;
import com.example.repositories.api.IndicesBasedWrapperRepository;
import com.example.services.IUserService;
import com.example.services.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static com.example.conf.ApplicationConfiguration.cachingSolution;

public abstract class AbstractUsersCacheRefreshStrategy extends AbstractCacheRefreshStrategy<String, UserDTO, Long> {

    public static final TypeReference<UserDTO> USER_DTO_TYPE_REFERENCE = new TypeReference<UserDTO>() {
    };
    @Autowired
    IUserService userService;
    @Autowired
    UserJpaRepository userJpaRepository;
    @Autowired(
            required = false
    )
    UserElasticWrapperRepository userElasticWrapperRepository;

    @Autowired(required = false)
    UserIgniteWrapperRepository userIgniteWrapperRepository;

    public AbstractUsersCacheRefreshStrategy() {
    }

    public String cacheIdentifierField() {
        return "id";
    }

    public Boolean isEvictionFromExistingCacheRequired(UserDTO existingObject) {
        return Objects.nonNull(existingObject);
    }

    public UserDTO getExistingObjectByIdentifier(Object id) {
        return this.userService.findById((Long)id);
    }

    public JpaRepository jpaRepository() {
        return this.userJpaRepository;
    }

    public IndicesBasedWrapperRepository indicesBasedWrapperRepository() {
        if(StringUtils.equalsIgnoreCase("elastic", cachingSolution())){
            return userElasticWrapperRepository;
        } else if(StringUtils.equalsIgnoreCase("indexed-ignite", cachingSolution())){
            return userIgniteWrapperRepository;
        } else {
            return null;
        }
    }

    public Long getMaxValueForIdentifier() {
        return Long.MAX_VALUE;
    }

    public Map<Long, UserDTO> convertCollectionToMap(Collection elements) {
        return (Map) UserService.CACHE_MAP_CONVERTER.apply(elements);
    }

    public Supplier<TypeReference> typeReferenceSupplier() {
        return () -> {
            return USER_DTO_TYPE_REFERENCE;
        };
    }
}
