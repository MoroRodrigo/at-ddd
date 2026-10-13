package br.com.freela.reputacao;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class ReputacaoEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(ReputacaoEventConsumer.class);
    private final ObjectMapper objectMapper;
    private final ReputacaoService reputacaoService;

    public ReputacaoEventConsumer(ObjectMapper objectMapper, ReputacaoService reputacaoService) {
        this.objectMapper = objectMapper;
        this.reputacaoService = reputacaoService;
    }

    @KafkaListener(topics = "freela.contratos.eventos.v1", groupId = "reputacao-eventos-consumer")
    public void onMessage(ConsumerRecord<String, String> record) {
        String correlationId = readHeader(record, "X-Correlation-Id");
        try {
            ContratosEventEnvelope envelope = objectMapper.readValue(record.value(), ContratosEventEnvelope.class);
            log.info("reputacao.kafka.recebido eventId={} contratoId={} eventType={} correlationId={} topic={} partition={} offset={}",
                    envelope.eventId(), envelope.contratoId(), envelope.eventType(), correlationId, record.topic(), record.partition(), record.offset());
            reputacaoService.processarEvento(envelope, correlationId);
        } catch (Exception ex) {
            log.error("reputacao.kafka.erro eventType={} correlationId={} error={}", record.value(), correlationId, ex.getMessage(), ex);
            throw new IllegalStateException("Erro ao processar evento de reputação", ex);
        }
    }

    private String readHeader(ConsumerRecord<String, String> record, String headerName) {
        Header header = record.headers().lastHeader(headerName);
        if (header == null) {
            return "unknown";
        }
        return new String(header.value(), StandardCharsets.UTF_8);
    }
}
