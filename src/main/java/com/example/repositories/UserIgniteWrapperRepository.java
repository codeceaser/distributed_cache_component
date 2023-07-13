package com.example.repositories;

import com.example.dto.UserDTO;
import com.example.entities.User;
import com.example.components.IndicesBasedWrapperRepository;
import com.example.components.InvertedIndicesRepository;
import com.example.repositories.ignite.impl.UserIgniteRepositoryImpl;
import com.example.services.IUserService;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.ignite.Ignite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.cache.Cache;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static com.example.cache.user.AbstractUsersCacheRefreshStrategy.USER_DTO_TYPE_REFERENCE;
import static com.example.services.UserService.CACHE_MAP_CONVERTER;

@Service
@ConditionalOnProperty(name = "caching.solution", havingValue = "indexed-ignite")
public class UserIgniteWrapperRepository implements IndicesBasedWrapperRepository<Long, UserDTO, User> {


    @Autowired
    private UserJpaRepository userJpaRepository;

    @Override
    public JpaRepository jpaRepository() {
        return userJpaRepository;
    }

    @Autowired
    IUserService userService;

    @Autowired(required = false)
    Ignite ignite;

    @Autowired(required = false)
    UserIgniteRepositoryImpl userIgniteRepository;

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
    public UserDTO getExistingObjectByIdentifier(Object id) {
        return userService.findById((Long) id);
    }

    @Override
    public void createInvertedIndicesAndCacheEntries(Collection<UserDTO> newerObjects) {
        if(CollectionUtils.isEmpty(newerObjects)) {
            LOGGER.info("+++ It Looks like Everything is uptodate, no newer objects to create inverted indices and cache entries! +++");
            return;
        }
        userIgniteRepository.saveAll(newerObjects);
    }

    @Override
    public Cache<Long, UserDTO> retrieveCache() {
        return ignite.cache("indexed-user");
    }

    @Override
    public InvertedIndicesRepository invertedIndicesRepository() {
        return userIgniteRepository;
    }

    @Override
    public void removeInvertedIndicesAndCacheEntries(Long keyOfObjectToBeRemoved) {
        userIgniteRepository.deleteById(keyOfObjectToBeRemoved);
    }

    @Override
    public UserDTO findOriginalObject(Long keyOfNewerObject) {
        Optional<UserDTO> userDTOById = userIgniteRepository.findById(keyOfNewerObject);
        if (userDTOById.isPresent()) {
            return userDTOById.get();
        } else {
            return getExistingObjectByIdentifier(keyOfNewerObject);
        }
    }

    @Override
    public Supplier<TypeReference> typeReferenceSupplier() {
        return () -> {
            return USER_DTO_TYPE_REFERENCE;
        };
    }
}
