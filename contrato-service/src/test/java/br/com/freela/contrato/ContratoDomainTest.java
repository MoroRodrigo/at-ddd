package br.com.freela.contrato;

import br.com.freela.contrato.domain.model.Contrato;
import br.com.freela.contrato.domain.model.StatusContrato;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ContratoDomainTest {

    @Test
    void deveCriarContratoComStatusAtivoEEvento() {
        UUID clienteId = UUID.randomUUID();
        UUID freelancerId = UUID.randomUUID();

        Contrato contrato = Contrato.criar(clienteId, freelancerId, "Desenvolvimento de API", new BigDecimal("1500.50"));

        assertEquals(StatusContrato.ATIVO, contrato.status());
        assertEquals(1, contrato.domainEvents().size());
        assertEquals("ContratoCriado", contrato.domainEvents().getFirst().eventType());
    }

    @Test
    void deveRegistrarEntregaEConclusaoEmSequencia() {
        Contrato contrato = Contrato.criar(UUID.randomUUID(), UUID.randomUUID(), "Portal", new BigDecimal("2000"));

        contrato.registrarEntrega("corr-1");
        assertEquals(StatusContrato.ENTREGA_REGISTRADA, contrato.status());

        contrato.concluir("corr-2");
        assertEquals(StatusContrato.CONCLUIDO, contrato.status());
        assertEquals(3, contrato.domainEvents().size());
    }

    @Test
    void naoDeveCancelarContratoConcluido() {
        Contrato contrato = Contrato.criar(UUID.randomUUID(), UUID.randomUUID(), "Portal", new BigDecimal("2000"));
        contrato.registrarEntrega("corr-1");
        contrato.concluir("corr-2");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> contrato.cancelar("corr-3"));
        assertTrue(ex.getMessage().contains("não pode ser cancelado"));
    }
}
