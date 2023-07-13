package com.example.services;

import com.example.aspects.CacheRefresherAspect;
import com.example.dto.CacheRefreshDTO;
import com.example.utils.CommonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@EnableKafka
@ConditionalOnProperty(
        name = {"caching.solution"},
        havingValue = "ehcache"
)
public class KafkaCacheRefreshConsumer {
    public static final Logger LOGGER = LoggerFactory.getLogger(KafkaCacheRefreshConsumer.class);

    public KafkaCacheRefreshConsumer() {
    }

    @KafkaListener(
            topics = {"cache-refresh"}
    )
    public void listen(String cacheRefreshPayload) {
        LOGGER.info("%%%%  Received Messasge : {}", cacheRefreshPayload);
        CacheRefreshDTO cacheRefreshDTO = (CacheRefreshDTO)CommonUtil.JSON_STRING_TO_OBJECT_CONVERTER.apply(cacheRefreshPayload, () -> {
            return new TypeReference<CacheRefreshDTO>() {
            };
        });
        CacheRefresherAspect.refreshCaches(cacheRefreshDTO.getExistingObject(), cacheRefreshDTO.getNewObject(), cacheRefreshDTO.getRefreshCache());
    }
}
