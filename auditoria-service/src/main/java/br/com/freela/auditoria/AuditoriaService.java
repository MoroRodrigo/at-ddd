package br.com.freela.auditoria;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class AuditoriaService {
    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);
    private final EventoAuditoriaRepository repository;
    private final ObjectMapper objectMapper;

    public AuditoriaService(EventoAuditoriaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void registrar(UUID eventId, UUID aggregateId, String eventType, String correlationId, String payload) {
        if (repository.existsByEventId(eventId)) {
            log.info("auditoria.ignorada eventId={} aggregateId={} eventType={} correlationId={} motivo=duplicado",
                    eventId, aggregateId, eventType, correlationId);
            return;
        }
        var e = repository.save(new EventoAuditoria(eventId, aggregateId, eventType, correlationId, payload));
        log.info("auditoria.registro.sucesso auditoriaId={} eventId={} aggregateId={} eventType={}", e.getId(), eventId, aggregateId, eventType);
    }

    @Transactional
    public void registrarEvento(ContratosEventEnvelope envelope, String correlationId) {
        if (repository.existsByEventId(envelope.eventId())) {
            log.info("auditoria.evento.ignorado service=auditoria eventId={} contratoId={} correlationId={} motivo=ja_processado",
                    envelope.eventId(), envelope.contratoId(), correlationId);
            return;
        }

        try {
            String payloadJson = objectMapper.writeValueAsString(Map.of(
                    "eventType", envelope.eventType(),
                    "contratoId", envelope.contratoId(),
                    "occurredAt", envelope.occurredAt(),
                    "aggregateVersion", envelope.aggregateVersion(),
                    "correlationId", correlationId == null ? envelope.correlationId() : correlationId,
                    "payload", envelope.payload()
            ));
            registrar(envelope.eventId(), envelope.contratoId(), envelope.eventType(),
                    correlationId == null ? envelope.correlationId() : correlationId, payloadJson);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao registrar auditoria do evento", ex);
        }
    }
}
