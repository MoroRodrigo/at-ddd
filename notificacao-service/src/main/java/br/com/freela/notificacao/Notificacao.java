package br.com.freela.notificacao;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notificacoes", uniqueConstraints = @UniqueConstraint(name = "uk_notificacao_event_id", columnNames = "eventId"))
public class Notificacao {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID eventId;

    @Column(nullable = false)
    private UUID contratoId;

    @Column(nullable = false)
    private UUID destinatarioId;

    @Column(nullable = false, length = 80)
    private String tipo;

    @Column(nullable = false, length = 500)
    private String mensagem;

    @Column(nullable = false)
    private String correlationId;

    @Column(nullable = false)
    private Instant criadaEm;

    protected Notificacao() {
    }

    public Notificacao(UUID eventId, UUID contratoId, UUID destinatarioId, String tipo, String mensagem, String correlationId) {
        this.id = UUID.randomUUID();
        this.eventId = eventId;
        this.contratoId = contratoId;
        this.destinatarioId = destinatarioId;
        this.tipo = tipo;
        this.mensagem = mensagem;
        this.correlationId = correlationId == null ? "unknown" : correlationId;
        this.criadaEm = Instant.now();
    }

    public UUID getId(){ return id; }
    public UUID getEventId(){ return eventId; }
    public UUID getContratoId(){ return contratoId; }
    public UUID getDestinatarioId(){ return destinatarioId; }
    public String getTipo(){ return tipo; }
    public String getMensagem(){ return mensagem; }
    public String getCorrelationId(){ return correlationId; }
    public Instant getCriadaEm(){ return criadaEm; }
}
