package br.com.freela.reputacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class ReputacaoService {
    private static final Logger log = LoggerFactory.getLogger(ReputacaoService.class);
    private final ReputacaoRepository repository;
    private final ReputacaoEventoProcessadoRepository eventoProcessadoRepository;

    public ReputacaoService(ReputacaoRepository repository, ReputacaoEventoProcessadoRepository eventoProcessadoRepository) {
        this.repository = repository;
        this.eventoProcessadoRepository = eventoProcessadoRepository;
    }

    @Transactional
    public void registrarContratoConcluido(UUID eventId, UUID contratoId, UUID freelancerId, BigDecimal valor, String correlationId) {
        if (eventoProcessadoRepository.existsByEventId(eventId)) {
            log.info("reputacao.ignorada contratoId={} freelancerId={} correlationId={} motivo=evento_duplicado", contratoId, freelancerId, correlationId);
            return;
        }

        var reputacao = repository.findById(freelancerId).orElseGet(() -> new ReputacaoFreelancer(freelancerId));
        reputacao.registrarContrato(valor);
        repository.save(reputacao);
        eventoProcessadoRepository.save(new ReputacaoEventoProcessado(eventId, correlationId));
        log.info("reputacao.atualizacao.sucesso contratoId={} freelancerId={} contratosConcluidos={} valorTotal={} correlationId={}",
                contratoId, freelancerId, reputacao.getContratosConcluidos(), reputacao.getValorTotal(), correlationId);
    }

    @Transactional
    public void processarEvento(ContratosEventEnvelope envelope, String correlationId) {
        UUID eventId = envelope.eventId();
        if (eventoProcessadoRepository.existsByEventId(eventId)) {
            log.info("reputacao.evento.ignorado service=reputacao eventId={} contratoId={} correlationId={} motivo=ja_processado",
                    eventId, envelope.contratoId(), correlationId);
            return;
        }

        if (!"ContratoConcluido".equals(envelope.eventType())) {
            log.info("reputacao.evento.sem_efeito service=reputacao eventId={} contratoId={} eventType={}",
                    eventId, envelope.contratoId(), envelope.eventType());
            return;
        }

        var freelancerId = UUID.fromString(envelope.payload().get("freelancerId").toString());
        var valor = new BigDecimal(envelope.payload().get("valor").toString());
        registrarContratoConcluido(eventId, envelope.contratoId(), freelancerId, valor, correlationId == null ? envelope.correlationId() : correlationId);
    }
}
