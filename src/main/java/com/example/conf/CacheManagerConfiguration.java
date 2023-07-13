package com.example.conf;

import org.apache.commons.lang3.StringUtils;
import org.apache.ignite.Ignite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import java.net.URI;

import static com.example.conf.ApplicationConfiguration.cachingSolution;


@Configuration
@EnableCaching
public class CacheManagerConfiguration {

    @Autowired(required = false)
    private Ignite ignite;

    @Bean
    public CacheManager customCacheManager() {
        CachingProvider cachingProvider = null;
        CacheManager cacheManager = null;
        Class providerClass = null;
        if(StringUtils.equalsIgnoreCase("ignite", cachingSolution())){
            providerClass = org.apache.ignite.cache.CachingProvider.class;
            cachingProvider = Caching.getCachingProvider(providerClass.getName());
            URI uri = cachingProvider.getDefaultURI();
            ClassLoader classLoader = ignite.configuration().getClassLoader();
            cacheManager = cachingProvider.getCacheManager(uri, classLoader);
        } else if(StringUtils.equalsIgnoreCase("ehcache", cachingSolution())){
            providerClass = org.ehcache.jsr107.EhcacheCachingProvider.class;
            cachingProvider = Caching.getCachingProvider(providerClass.getName());
            cacheManager = cachingProvider.getCacheManager();
        }
        return cacheManager;
    }


}
