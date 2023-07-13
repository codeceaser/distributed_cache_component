package com.example.cache.user;

import com.example.dto.UserDTO;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

import static com.example.utils.AppConstants.USERS_BY_DEPARTMENT;

@Component
@Qualifier(USERS_BY_DEPARTMENT)
public class UsersByDepartmentCacheRefreshStrategy extends AbstractUsersCacheRefreshStrategy{

    @Override
    public String cacheName() {
        return USERS_BY_DEPARTMENT;
    }

    @Override
    public List<String> cacheKeyFields() {
        return Lists.newArrayList("department");
    }

    @Override
    public Boolean areKeysDifferent(UserDTO existingObject, UserDTO newerObject) {
        return Objects.nonNull(existingObject)
                && Objects.nonNull(newerObject)
                && !StringUtils.equals(existingObject.getDepartment(), newerObject.getDepartment());
    }

}
