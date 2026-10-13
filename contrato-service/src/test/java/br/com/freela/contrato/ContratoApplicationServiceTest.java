package br.com.freela.contrato;

import br.com.freela.contrato.application.ContratoApplicationService;
import br.com.freela.contrato.application.CriarContratoCommand;
import br.com.freela.contrato.domain.repository.ContratoRepository;
import br.com.freela.contrato.infrastructure.outbox.OutboxEventEntity;
import br.com.freela.contrato.infrastructure.outbox.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ContratoApplicationServiceTest {

    @Test
    void devePersistirContratoERegistrarOutbox() {
        ContratoRepository repository = Mockito.mock(ContratoRepository.class);
        OutboxEventRepository outboxEventRepository = Mockito.mock(OutboxEventRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();

        var clienteId = UUID.randomUUID();
        var freelancerId = UUID.randomUUID();
        var contrato = br.com.freela.contrato.domain.model.Contrato.criar(clienteId, freelancerId, "API", new BigDecimal("2500"));
        Mockito.when(repository.salvar(Mockito.any())).thenReturn(contrato);
        Mockito.when(outboxEventRepository.save(Mockito.any(OutboxEventEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ContratoApplicationService service = new ContratoApplicationService(repository, outboxEventRepository, objectMapper);
        var salvo = service.criar(new CriarContratoCommand(clienteId, freelancerId, "API", new BigDecimal("2500")), "corr-1");

        assertNotNull(salvo);
        assertEquals("API", salvo.titulo());
        Mockito.verify(outboxEventRepository, Mockito.atLeastOnce()).save(
            Mockito.argThat(event ->
                event.getAggregateId().equals(salvo.id()) &&
                "ContratoCriado".equals(event.getEventType()) &&
                event.getPayload().contains("\"eventType\":\"ContratoCriado\"")
            )
        );
    }
}
