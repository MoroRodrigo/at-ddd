package br.com.freela.contrato.infrastructure.outbox;

import br.com.freela.contrato.domain.shared.DomainEvent;
import br.com.freela.contrato.infrastructure.messaging.DomainEventEnvelope;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID aggregateId;

    @Column(nullable = false)
    private String aggregateType;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private String correlationId;

    @Column(nullable = false)
    private long aggregateVersion;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(columnDefinition = "TEXT")
    private String failureDetails;

    private Instant publishedAt;

    protected OutboxEventEntity() {
    }

    private OutboxEventEntity(UUID id, UUID aggregateId, String aggregateType, String eventType, String payload,
                              String correlationId, long aggregateVersion, Instant createdAt,
                              OutboxStatus status, int attempts, String failureDetails, Instant publishedAt) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.payload = payload;
        this.correlationId = correlationId;
        this.aggregateVersion = aggregateVersion;
        this.createdAt = createdAt;
        this.status = status;
        this.attempts = attempts;
        this.failureDetails = failureDetails;
        this.publishedAt = publishedAt;
    }

    public static OutboxEventEntity fromSerialized(DomainEvent event, String serializedPayload, String correlationId) {
        return new OutboxEventEntity(
                event.eventId(),
                event.contratoId(),
                "Contrato",
                event.eventType(),
                serializedPayload,
                correlationId == null || correlationId.isBlank() ? "unknown" : correlationId,
                event.aggregateVersion(),
                Instant.now(),
                OutboxStatus.PENDING,
                0,
                null,
                null
        );
    }

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }

    public void markFailed(String details) {
        this.status = OutboxStatus.FAILED;
        this.failureDetails = details;
        this.attempts = this.attempts + 1;
    }

    public void incrementAttempts() {
        this.attempts += 1;
    }

    public UUID getId() { return id; }
    public UUID getAggregateId() { return aggregateId; }
    public String getAggregateType() { return aggregateType; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public String getCorrelationId() { return correlationId; }
    public long getAggregateVersion() { return aggregateVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public OutboxStatus getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public String getFailureDetails() { return failureDetails; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setStatus(OutboxStatus status) { this.status = status; }
    public void setFailureDetails(String failureDetails) { this.failureDetails = failureDetails; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public void setPayload(String payload) { this.payload = payload; }
}
