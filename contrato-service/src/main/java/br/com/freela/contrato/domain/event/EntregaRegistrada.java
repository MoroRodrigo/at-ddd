package br.com.freela.contrato.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import br.com.freela.contrato.domain.model.Contrato;
import br.com.freela.contrato.domain.model.StatusContrato;
import br.com.freela.contrato.domain.shared.DomainEvent;

public record EntregaRegistrada(
        UUID eventId,
        Instant occurredAt,
        UUID contratoId,
        UUID clienteId,
        UUID freelancerId,
        String titulo,
        BigDecimal valor,
        StatusContrato status,
        long aggregateVersion,
        String correlationId
) implements DomainEvent {
    public static EntregaRegistrada novo(Contrato contrato, long aggregateVersion, String correlationId) {
        return new EntregaRegistrada(
                UUID.randomUUID(),
                Instant.now(),
                contrato.id(),
                contrato.clienteId(),
                contrato.freelancerId(),
                contrato.titulo(),
                contrato.valor(),
                contrato.status(),
                aggregateVersion,
                correlationId
        );
    }

    @Override
    public String eventType() {
        return "EntregaRegistrada";
    }
}
