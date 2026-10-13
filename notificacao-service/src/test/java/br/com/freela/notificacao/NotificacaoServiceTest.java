package br.com.freela.notificacao;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NotificacaoServiceTest {

    @Test
    void deveIgnorarEventoDuplicado() {
        NotificacaoRepository repository = Mockito.mock(NotificacaoRepository.class);
        Mockito.when(repository.existsByEventId(Mockito.any(UUID.class))).thenReturn(true);

        NotificacaoService service = new NotificacaoService(repository);
        var eventId = UUID.randomUUID();
        var contratoId = UUID.randomUUID();
        var freelancerId = UUID.randomUUID();

        service.processarEvento(new ContratosEventEnvelope(eventId, "ContratoCriado", contratoId,
                Instant.now(), "corr-1", 1L, "v1", Map.of("freelancerId", freelancerId.toString(), "valor", "1000.0")), "corr-1");

        Mockito.verify(repository, Mockito.never()).save(Mockito.any());
    }
}
