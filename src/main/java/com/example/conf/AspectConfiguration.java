package com.example.conf;

import com.example.aspects.CacheRefresherAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
public class AspectConfiguration {

    @Bean
    public CacheRefresherAspect cacheRefresherAspect() {
        return new CacheRefresherAspect();
    }
}
