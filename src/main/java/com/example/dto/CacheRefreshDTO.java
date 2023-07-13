package com.example.dto;

import com.example.annotations.RefreshCache;
import com.example.annotations.RefreshCacheDTO;

import java.io.Serializable;

public class CacheRefreshDTO implements Serializable {

    Object existingObject;
    Object newObject;

    RefreshCacheDTO refreshCache;

    public CacheRefreshDTO(Object existingObject, Object newObject, RefreshCacheDTO refreshCache) {
        this.existingObject = existingObject;
        this.newObject = newObject;
        this.refreshCache = refreshCache;
    }

    protected CacheRefreshDTO() {
    }

    public Object getExistingObject() {
        return existingObject;
    }

    public Object getNewObject() {
        return newObject;
    }

    public RefreshCache getRefreshCache() {
        return refreshCache;
    }
}
