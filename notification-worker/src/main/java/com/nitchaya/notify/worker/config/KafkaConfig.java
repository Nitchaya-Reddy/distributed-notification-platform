package com.nitchaya.notify.worker.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    public static final String REQUESTED = "notifications.requested";
    public static final String DELIVERY = "notifications.delivery";
    public static final String STATUS = "notifications.status";

    @Bean
    NewTopic deliveryTopic() {
        return TopicBuilder.name(DELIVERY).partitions(12).replicas(1).build();
    }
}
