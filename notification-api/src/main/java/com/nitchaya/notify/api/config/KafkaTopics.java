package com.nitchaya.notify.api.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopics {
    public static final String REQUESTED = "notifications.requested";
    public static final String STATUS = "notifications.status";

    @Bean
    NewTopic requestedTopic() {
        return TopicBuilder.name(REQUESTED).partitions(6).replicas(1).build();
    }

    @Bean
    NewTopic statusTopic() {
        return TopicBuilder.name(STATUS).partitions(3).replicas(1).build();
    }
}
