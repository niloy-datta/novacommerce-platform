package com.novacommerce.payment.infrastructure.outbox;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class PaymentOutboxPublisher {
    private final OutboxRepository repository;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;
    private final String topic;

    public PaymentOutboxPublisher(OutboxRepository repository, KafkaTemplate<String, String> kafka,
                                  ObjectMapper mapper, @Value("${payment.events-topic:novacommerce.payment.events}") String topic) {
        this.repository = repository; this.kafka = kafka; this.mapper = mapper; this.topic = topic;
    }

    @Scheduled(fixedDelayString = "${payment.outbox-interval:PT2S}")
    public void publishBatch() {
        List<OutboxEvent> events = repository.findTop100ByStatusOrderByCreatedAtAsc("PENDING");
        for (OutboxEvent event : events) {
            try {
                String envelope = mapper.writeValueAsString(Map.of(
                        "eventId", event.getId(), "eventType", event.getEventType(),
                        "eventVersion", 1, "aggregateId", event.getAggregateId(),
                        "occurredAt", event.getCreatedAt(), "payload", mapper.readTree(event.getPayload())));
                kafka.send(topic, event.getAggregateId(), envelope).get(3, TimeUnit.SECONDS);
                event.markPublished();
                repository.save(event);
            } catch (Exception ex) {
                event.markFailed();
                repository.save(event);
            }
        }
    }
}
