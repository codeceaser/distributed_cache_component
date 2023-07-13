package com.example.conf;

import com.example.dto.UserDTO;
import com.example.repositories.ignite.impl.UserIgniteRepositoryImpl;
import com.google.common.collect.Lists;
import org.apache.ignite.Ignite;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.springdata.repository.config.EnableIgniteRepositories;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.expiry.Duration;
import java.util.List;

import static com.example.cache.api.AbstractCacheRefreshStrategy.CREATE_IGNITE_CACHE_CONFIGURATION;

@Configuration
@ConditionalOnExpression("'${caching.solution}'=='ignite' || '${caching.solution}'=='elastic' || '${caching.solution}'=='indexed-ignite'")
@EnableIgniteRepositories(basePackages = "com.example.repositories.ignite")

public class IgniteCacheConfiguration {

    @Autowired(required = false)
    Ignite ignite;


    @Bean(name = "cacheConfiguration")
    @ConditionalOnExpression("'${caching.solution}'=='ignite' || '${caching.solution}'=='elastic' || '${caching.solution}'=='indexed-ignite'")
    public CacheConfiguration[] cacheConfiguration() {
        List<CacheConfiguration> cacheConfigurations = Lists.newArrayList();
        // Defining and creating a new cache to be used by Ignite Spring Data
        // repository.
        CacheConfiguration ccfg = CREATE_IGNITE_CACHE_CONFIGURATION.apply(ignite, "indexed-user", Duration.ONE_DAY);
        // Setting SQL schema for the cache.
        ccfg.setIndexedTypes(Long.class, UserDTO.class);

        cacheConfigurations.add(ccfg);

        return cacheConfigurations.toArray(new CacheConfiguration[cacheConfigurations.size()]);
    }

    @Bean
    public UserIgniteRepositoryImpl userIgniteRepository() {
        /*IgniteRepositoryFactory igniteRepositoryFactory = new IgniteRepositoryFactory(ignite);
        UserIgniteRepository userIgniteRepository = igniteRepositoryFactory.getRepository(UserIgniteRepository.class);
        return userIgniteRepository;*/
        return new UserIgniteRepositoryImpl(ignite);
    }
}
