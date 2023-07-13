package com.example.services;

import com.example.dto.CacheRefreshDTO;
import com.example.utils.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
        name = {"caching.solution"},
        havingValue = "ehcache"
)
public class KafkaCacheRefreshProducer {
    public static final Logger LOGGER = LoggerFactory.getLogger(KafkaCacheRefreshProducer.class);
    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaCacheRefreshProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(String topic, CacheRefreshDTO cacheRefreshDTO) {
        LOGGER.info("%%%% Sending cacheRefreshDTO='{}' to topic='{}'", cacheRefreshDTO, topic);
        this.kafkaTemplate.send(topic, (String)CommonUtil.OBJECT_TO_JSON_CONVERTER.apply(cacheRefreshDTO));
    }
}
