package br.com.freela.contrato.domain.event;

import br.com.freela.contrato.domain.model.Contrato;
import br.com.freela.contrato.domain.model.StatusContrato;
import br.com.freela.contrato.domain.shared.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ContratoConcluido(
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
    public static ContratoConcluido novo(Contrato contrato, long aggregateVersion, String correlationId) {
        return new ContratoConcluido(
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
        return "ContratoConcluido";
    }
}
