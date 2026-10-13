package br.com.freela.reputacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reputacao_eventos_processados", uniqueConstraints = @UniqueConstraint(name = "uk_reputacao_event_id", columnNames = "eventId"))
public class ReputacaoEventoProcessado {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID eventId;

    @Column(nullable = false)
    private String correlationId;

    @Column(nullable = false)
    private Instant processadoEm;

    protected ReputacaoEventoProcessado() {
    }

    public ReputacaoEventoProcessado(UUID eventId, String correlationId) {
        this.id = UUID.randomUUID();
        this.eventId = eventId;
        this.correlationId = correlationId == null ? "unknown" : correlationId;
        this.processadoEm = Instant.now();
    }

    public UUID getId(){ return id; }
    public UUID getEventId(){ return eventId; }
    public String getCorrelationId(){ return correlationId; }
    public Instant getProcessadoEm(){ return processadoEm; }
}
