package br.com.freela.notificacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class NotificacaoService {
    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);
    private final NotificacaoRepository repository;

    public NotificacaoService(NotificacaoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void registrar(UUID eventId, UUID contratoId, UUID destinatarioId, String tipo, String mensagem, String correlationId) {
        if (repository.existsByEventId(eventId)) {
            log.info("notificacao.ignorada eventId={} contratoId={} motivo=duplicado", eventId, contratoId);
            return;
        }
        var saved = repository.save(new Notificacao(eventId, contratoId, destinatarioId, tipo, mensagem, correlationId));
        log.info("notificacao.registro.sucesso notificacaoId={} eventId={} contratoId={} destinatarioId={} tipo={}",
                saved.getId(), saved.getEventId(), saved.getContratoId(), saved.getDestinatarioId(), saved.getTipo());
    }

    @Transactional
    public void processarEvento(ContratosEventEnvelope envelope, String correlationId) {
        UUID eventId = envelope.eventId();
        if (repository.existsByEventId(eventId)) {
            log.info("notificacao.evento.ignorado service=notificacao eventId={} contratoId={} correlationId={} motivo=ja_processado",
                    eventId, envelope.contratoId(), correlationId);
            return;
        }

        switch (envelope.eventType()) {
            case "ContratoCriado" -> registrar(
                    eventId,
                    envelope.contratoId(),
                    UUID.fromString(envelope.payload().get("freelancerId").toString()),
                    "CONTRATO_CRIADO",
                    "Contrato criado para o freelancer com valor de " + envelope.payload().get("valor"),
                    correlationId == null ? envelope.correlationId() : correlationId);
            case "EntregaRegistrada" -> registrar(
                    eventId,
                    envelope.contratoId(),
                    UUID.fromString(envelope.payload().get("freelancerId").toString()),
                    "ENTREGA_REGISTRADA",
                    "Entrega registrada para o contrato " + envelope.contratoId(),
                    correlationId == null ? envelope.correlationId() : correlationId);
            case "ContratoConcluido" -> registrar(
                    eventId,
                    envelope.contratoId(),
                    UUID.fromString(envelope.payload().get("freelancerId").toString()),
                    "CONTRATO_CONCLUIDO",
                    "Contrato concluído com valor de " + envelope.payload().get("valor"),
                    correlationId == null ? envelope.correlationId() : correlationId);
            case "ContratoCancelado" -> registrar(
                    eventId,
                    envelope.contratoId(),
                    UUID.fromString(envelope.payload().get("clienteId").toString()),
                    "CONTRATO_CANCELADO",
                    "Contrato cancelado: " + envelope.contratoId(),
                    correlationId == null ? envelope.correlationId() : correlationId);
            default -> log.info("notificacao.evento.sem_efeito service=notificacao eventId={} contratoId={} eventType={}",
                    eventId, envelope.contratoId(), envelope.eventType());
        }
    }
}
