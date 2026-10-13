package br.com.freela.notificacao;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ContratosEventEnvelope(
        UUID eventId,
        String eventType,
        UUID contratoId,
        Instant occurredAt,
        String correlationId,
        long aggregateVersion,
        String schemaVersion,
        Map<String, Object> payload
) {
}
