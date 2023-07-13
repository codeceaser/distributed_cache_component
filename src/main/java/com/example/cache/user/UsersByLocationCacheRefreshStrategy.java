package com.example.cache.user;

import com.example.dto.UserDTO;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

import static com.example.utils.AppConstants.USERS_BY_LOCATION;

@Component
@Qualifier(USERS_BY_LOCATION)
public class UsersByLocationCacheRefreshStrategy extends AbstractUsersCacheRefreshStrategy{

    @Override
    public String cacheName() {
        return USERS_BY_LOCATION;
    }

    @Override
    public List<String> cacheKeyFields() {
        return Lists.newArrayList("location");
    }

    @Override
    public Boolean areKeysDifferent(UserDTO existing, UserDTO newer) {
        return Objects.nonNull(existing)
                && Objects.nonNull(newer)
                && !StringUtils.equals(existing.getLocation(), newer.getLocation());
    }

}
