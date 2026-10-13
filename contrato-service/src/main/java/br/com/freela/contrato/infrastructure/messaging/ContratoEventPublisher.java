package br.com.freela.contrato.infrastructure.messaging;

import br.com.freela.contrato.infrastructure.outbox.OutboxEventEntity;
import br.com.freela.contrato.infrastructure.outbox.OutboxEventRepository;
import br.com.freela.contrato.infrastructure.outbox.OutboxStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class ContratoEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(ContratoEventPublisher.class);
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public ContratoEventPublisher(OutboxEventRepository outboxEventRepository,
                                 KafkaTemplate<String, String> kafkaTemplate,
                                 ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 2500)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEventEntity> pending = outboxEventRepository.findPendingForPublish(OutboxStatus.PENDING,
                org.springframework.data.domain.PageRequest.of(0, 20));
        for (OutboxEventEntity event : pending) {
            try {
                var payload = objectMapper.readTree(event.getPayload());
                var correlationId = Optional.ofNullable(event.getCorrelationId()).orElse("unknown");
                Message<String> message = MessageBuilder
                        .withPayload(event.getPayload())
                        .setHeader(KafkaHeaders.TOPIC, ContratoKafkaConfiguration.EVENT_TOPIC)
                        .setHeader(KafkaHeaders.KEY, event.getAggregateId().toString())
                        .setHeader("X-Correlation-Id", correlationId)
                        .build();
                kafkaTemplate.send(message).get();
                event.setStatus(OutboxStatus.PUBLISHED);
                event.setPublishedAt(java.time.Instant.now());
                outboxEventRepository.save(event);
                log.info("contrato.outbox.publicado contratoId={} eventType={} aggregateVersion={}",
                        event.getAggregateId(), event.getEventType(), event.getAggregateVersion());
            } catch (Exception ex) {
                log.warn("contrato.outbox.falha contratoId={} eventType={} attempts={} error={}",
                        event.getAggregateId(), event.getEventType(), event.getAttempts() + 1, ex.getMessage());
                event.incrementAttempts();
                event.setFailureDetails(ex.getMessage());
                event.setStatus(OutboxStatus.FAILED);
                outboxEventRepository.save(event);
            }
        }
    }
}
