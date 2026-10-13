package br.com.freela.contrato.application;

import br.com.freela.contrato.domain.model.Contrato;
import br.com.freela.contrato.domain.repository.ContratoRepository;
import br.com.freela.contrato.domain.shared.DomainEvent;
import br.com.freela.contrato.infrastructure.outbox.OutboxEventEntity;
import br.com.freela.contrato.infrastructure.outbox.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ContratoApplicationService {
    private static final Logger log = LoggerFactory.getLogger(ContratoApplicationService.class);
    private final ContratoRepository repository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public ContratoApplicationService(ContratoRepository repository,
                                      OutboxEventRepository outboxEventRepository,
                                      ObjectMapper objectMapper) {
        this.repository = repository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Contrato criar(CriarContratoCommand cmd, String correlationId) {
        log.info("contrato.criacao.inicio correlationId={} clienteId={} freelancerId={} titulo={} valor={}",
                correlationId, cmd.clienteId(), cmd.freelancerId(), cmd.titulo(), cmd.valor());
        Contrato contrato = Contrato.criar(cmd.clienteId(), cmd.freelancerId(), cmd.titulo(), cmd.valor());
        Contrato salvo = repository.salvar(contrato);
        persistOutboxEvents(salvo, correlationId);

        log.info("contrato.criacao.sucesso correlationId={} contratoId={} clienteId={} freelancerId={} status={}",
                correlationId, salvo.id(), salvo.clienteId(), salvo.freelancerId(), salvo.status());
        return salvo;
    }

    @Transactional
    public Contrato registrarEntrega(UUID id, String correlationId) {
        Contrato contrato = repository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Contrato não encontrado: " + id));
        contrato.registrarEntrega(correlationId);
        Contrato salvo = repository.salvar(contrato);
        persistOutboxEvents(salvo, correlationId);
        log.info("contrato.entrega.registrada correlationId={} contratoId={} status={}", correlationId, salvo.id(), salvo.status());
        return salvo;
    }

    @Transactional
    public Contrato concluir(UUID id, String correlationId) {
        Contrato contrato = repository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Contrato não encontrado: " + id));
        contrato.concluir(correlationId);
        Contrato salvo = repository.salvar(contrato);
        persistOutboxEvents(salvo, correlationId);
        log.info("contrato.concluido correlationId={} contratoId={} status={}", correlationId, salvo.id(), salvo.status());
        return salvo;
    }

    @Transactional
    public Contrato cancelar(UUID id, String correlationId) {
        Contrato contrato = repository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Contrato não encontrado: " + id));
        contrato.cancelar(correlationId);
        Contrato salvo = repository.salvar(contrato);
        persistOutboxEvents(salvo, correlationId);
        log.info("contrato.cancelado correlationId={} contratoId={} status={}", correlationId, salvo.id(), salvo.status());
        return salvo;
    }

    @Transactional(readOnly = true)
    public Contrato buscar(UUID id) {
        log.info("contrato.busca.inicio contratoId={}", id);
        var contrato = repository.buscarPorId(id).orElseThrow(() -> new IllegalArgumentException("Contrato não encontrado: " + id));
        log.info("contrato.busca.sucesso contratoId={} status={}", id, contrato.status());
        return contrato;
    }

    @Transactional(readOnly = true)
    public List<Contrato> listar() {
        log.info("contrato.listagem.inicio");
        var contratos = repository.listar();
        log.info("contrato.listagem.sucesso quantidade={}", contratos.size());
        return contratos;
    }

    private void persistOutboxEvents(Contrato contrato, String correlationId) {
        for (DomainEvent event : contrato.pullDomainEvents()) {
            String serialized = serializeEvent(event, correlationId);
            OutboxEventEntity entry = OutboxEventEntity.fromSerialized(event, serialized, correlationId);
            outboxEventRepository.save(entry);
            log.info("contrato.evento.pendente contratoId={} eventId={} eventType={} aggregateVersion={}",
                    contrato.id(), event.eventId(), event.eventType(), event.aggregateVersion());
        }
    }

    private String serializeEvent(DomainEvent event, String correlationId) {
        try {
            Map<String, Object> payload = payloadFor(event);
            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("eventId", event.eventId().toString());
            envelope.put("eventType", event.eventType());
            envelope.put("contratoId", event.contratoId().toString());
            envelope.put("occurredAt", event.occurredAt().toString());
            envelope.put("correlationId", correlationId == null ? "unknown" : correlationId);
            envelope.put("aggregateVersion", event.aggregateVersion());
            envelope.put("schemaVersion", event.schemaVersion());
            envelope.put("payload", payload);
            return objectMapper.writeValueAsString(envelope);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao serializar evento do contrato", ex);
        }
    }

    private Map<String, Object> payloadFor(DomainEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (event instanceof br.com.freela.contrato.domain.event.ContratoCriado e) {
            payload.put("clienteId", e.clienteId().toString());
            payload.put("freelancerId", e.freelancerId().toString());
            payload.put("titulo", e.titulo());
            payload.put("valor", e.valor());
            payload.put("status", "ATIVO");
        } else if (event instanceof br.com.freela.contrato.domain.event.EntregaRegistrada e) {
            payload.put("clienteId", e.clienteId().toString());
            payload.put("freelancerId", e.freelancerId().toString());
            payload.put("titulo", e.titulo());
            payload.put("valor", e.valor());
            payload.put("status", e.status().name());
        } else if (event instanceof br.com.freela.contrato.domain.event.ContratoConcluido e) {
            payload.put("clienteId", e.clienteId().toString());
            payload.put("freelancerId", e.freelancerId().toString());
            payload.put("titulo", e.titulo());
            payload.put("valor", e.valor());
            payload.put("status", e.status().name());
        } else if (event instanceof br.com.freela.contrato.domain.event.ContratoCancelado e) {
            payload.put("clienteId", e.clienteId().toString());
            payload.put("freelancerId", e.freelancerId().toString());
            payload.put("titulo", e.titulo());
            payload.put("valor", e.valor());
            payload.put("status", e.status().name());
        }
        return payload;
    }
}
