package br.com.freela.contrato.infrastructure.messaging;

import br.com.freela.contrato.domain.shared.DomainEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DomainEventEnvelope(
        UUID eventId,
        String eventType,
        UUID contratoId,
        Instant occurredAt,
        String correlationId,
        long aggregateVersion,
        String schemaVersion,
        Map<String, Object> payload
) {
    public static DomainEventEnvelope from(DomainEvent event, Map<String, Object> payload) {
        return new DomainEventEnvelope(
                event.eventId(),
                event.eventType(),
                event.contratoId(),
                event.occurredAt(),
                event.correlationId(),
                event.aggregateVersion(),
                event.schemaVersion(),
                payload
        );
    }
}
