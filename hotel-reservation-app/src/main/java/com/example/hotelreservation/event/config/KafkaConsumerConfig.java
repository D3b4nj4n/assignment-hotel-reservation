package com.example.hotelreservation.event.config;

import com.example.hotelreservation.event.model.BankTransferPaymentEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka consumer configuration for processing bank transfer payment events.
 */
@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    /**
     * Creates a {@link ConsumerFactory} configured to consume {@link BankTransferPaymentEvent}
     * messages with a {@link StringDeserializer} for keys and a {@link JsonDeserializer} for values.
     *
     * <p>The consumer is set to reset offsets to {@code earliest} so no events are missed on startup.
     * Type info headers are disabled so the deserializer always maps the payload to {@link BankTransferPaymentEvent}
     * regardless of the producer's type headers.
     *
     * @return a fully configured {@link ConsumerFactory} for bank transfer payment events
     */
    public ConsumerFactory<String, BankTransferPaymentEvent> bankTransferConsumerFactory() {

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.hotelreservation.model");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, BankTransferPaymentEvent.class.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, String.valueOf(false));

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(BankTransferPaymentEvent.class, false)
        );
    }

    /**
     * Creates a {@link ConcurrentKafkaListenerContainerFactory} backed by {@link #bankTransferConsumerFactory()}.
     *
     * <p>Listener methods annotated with {@code @KafkaListener(containerFactory = "bankTransferKafkaListenerContainerFactory")}
     * will use this factory to consume and deserialize bank transfer payment events.
     *
     * @return a {@link ConcurrentKafkaListenerContainerFactory} for bank transfer payment events
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, BankTransferPaymentEvent> bankTransferKafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, BankTransferPaymentEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(bankTransferConsumerFactory());
        return factory;
    }

}
