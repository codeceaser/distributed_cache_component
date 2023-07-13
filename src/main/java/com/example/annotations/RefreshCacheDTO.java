//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.example.annotations;

import com.example.utils.CommonUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.function.Function;

public class RefreshCacheDTO implements RefreshCache {
    private String[] cacheNames;
    private String isDelete;
    private Class annotationType;
    @JsonIgnore
    public static final Function<String, RefreshCache> DE_SERIALIZE_REFRESH_CACHE = (refreshCacheJson) -> {
        RefreshCacheDTO refreshCacheDTO = (RefreshCacheDTO)CommonUtil.JSON_STRING_TO_OBJECT_CONVERTER.apply(refreshCacheJson, () -> {
            return new TypeReference<RefreshCacheDTO>() {
            };
        });
        return refreshCacheDTO;
    };
    @JsonIgnore
    public static final Function<RefreshCache, String> SERIALIZE_REFRESH_CACHE = (refreshCache) -> {
        RefreshCacheDTO refreshCacheDTO = new RefreshCacheDTO(refreshCache);
        String refreshCacheJson = (String)CommonUtil.OBJECT_TO_JSON_CONVERTER.apply(refreshCacheDTO);
        return refreshCacheJson;
    };

    public RefreshCacheDTO(RefreshCache refreshCache) {
        this.cacheNames = refreshCache.cacheNames();
        this.isDelete = refreshCache.isDelete();
        this.annotationType = refreshCache.annotationType();
    }

    protected RefreshCacheDTO() {
    }

    public String[] cacheNames() {
        return this.cacheNames;
    }

    public String isDelete() {
        return this.isDelete;
    }

    public Class annotationType() {
        return this.annotationType;
    }
}
