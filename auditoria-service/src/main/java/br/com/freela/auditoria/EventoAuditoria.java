package br.com.freela.auditoria;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auditoria_eventos", uniqueConstraints = @UniqueConstraint(name = "uk_auditoria_event_id", columnNames = "eventId"))
public class EventoAuditoria {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID eventId;

    @Column(nullable = false)
    private UUID aggregateId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String correlationId;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(nullable = false)
    private Instant recebidoEm;

    protected EventoAuditoria() {
    }

    public EventoAuditoria(UUID eventId, UUID aggregateId, String eventType, String correlationId, String payload) {
        this.id = UUID.randomUUID();
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.correlationId = correlationId == null ? "unknown" : correlationId;
        this.payload = payload;
        this.recebidoEm = Instant.now();
    }

    public UUID getId(){ return id; }
    public UUID getEventId(){ return eventId; }
    public UUID getAggregateId(){ return aggregateId; }
    public String getEventType(){ return eventType; }
    public String getCorrelationId(){ return correlationId; }
    public String getPayload(){ return payload; }
    public Instant getRecebidoEm(){ return recebidoEm; }
}
