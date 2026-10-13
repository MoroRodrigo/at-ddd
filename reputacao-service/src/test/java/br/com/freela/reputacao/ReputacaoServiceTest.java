package br.com.freela.reputacao;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReputacaoServiceTest {

    @Test
    void deveIgnorarEventoDuplicado() {
        ReputacaoRepository repository = Mockito.mock(ReputacaoRepository.class);
        ReputacaoEventoProcessadoRepository eventoProcessadoRepository = Mockito.mock(ReputacaoEventoProcessadoRepository.class);
        Mockito.when(eventoProcessadoRepository.existsByEventId(Mockito.any(UUID.class))).thenReturn(true);

        ReputacaoService service = new ReputacaoService(repository, eventoProcessadoRepository);
        var eventId = UUID.randomUUID();
        var contratoId = UUID.randomUUID();
        var freelancerId = UUID.randomUUID();

        service.processarEvento(new ContratosEventEnvelope(eventId, "ContratoConcluido", contratoId, Instant.now(), "corr-1", 2L, "v1",
                Map.of("freelancerId", freelancerId.toString(), "valor", new BigDecimal("2000.00").toPlainString())), "corr-1");

        Mockito.verify(repository, Mockito.never()).save(Mockito.any());
    }
}
