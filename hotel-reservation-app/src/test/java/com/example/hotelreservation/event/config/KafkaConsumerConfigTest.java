package com.example.hotelreservation.event.config;

import com.example.hotelreservation.event.model.BankTransferPaymentEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaConsumerConfigTest {

    private KafkaConsumerConfig kafkaConsumerConfig;

    @BeforeEach
    void setUp() {

        kafkaConsumerConfig = new KafkaConsumerConfig();
        ReflectionTestUtils.setField(kafkaConsumerConfig, "bootstrapServers", "localhost:9092");
        ReflectionTestUtils.setField(kafkaConsumerConfig, "groupId", "test-group");
    }

    @Test
    void bankTransferConsumerFactory_returnsNonNullFactory() {

        ConsumerFactory<String, BankTransferPaymentEvent> factory = kafkaConsumerConfig.bankTransferConsumerFactory();

        assertThat(factory).isNotNull();
    }

    @Test
    void bankTransferConsumerFactory_hasCorrectBootstrapServers() {

        ConsumerFactory<String, BankTransferPaymentEvent> factory = kafkaConsumerConfig.bankTransferConsumerFactory();

        assertThat(factory.getConfigurationProperties())
                .containsEntry(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    }

    @Test
    void bankTransferConsumerFactory_hasCorrectGroupId() {

        ConsumerFactory<String, BankTransferPaymentEvent> factory = kafkaConsumerConfig.bankTransferConsumerFactory();

        assertThat(factory.getConfigurationProperties())
                .containsEntry(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
    }

    @Test
    void bankTransferConsumerFactory_hasCorrectDeserializerConfig() {

        ConsumerFactory<String, BankTransferPaymentEvent> factory = kafkaConsumerConfig.bankTransferConsumerFactory();

        assertThat(factory.getConfigurationProperties())
                .containsEntry(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName())
                .containsEntry(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class.getName())
                .containsEntry(JsonDeserializer.VALUE_DEFAULT_TYPE, BankTransferPaymentEvent.class.getName())
                .containsEntry(JsonDeserializer.USE_TYPE_INFO_HEADERS, "false");
    }

    @Test
    void bankTransferConsumerFactory_setsEarliestAutoOffsetReset() {

        ConsumerFactory<String, BankTransferPaymentEvent> factory = kafkaConsumerConfig.bankTransferConsumerFactory();

        assertThat(factory.getConfigurationProperties())
                .containsEntry(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    }

    @Test
    void bankTransferKafkaListenerContainerFactory_returnsNonNullFactory() {

        ConcurrentKafkaListenerContainerFactory<String, BankTransferPaymentEvent> factory =
                kafkaConsumerConfig.bankTransferKafkaListenerContainerFactory();

        assertThat(factory).isNotNull();
        assertThat(factory.getConsumerFactory()).isNotNull();
    }

}
