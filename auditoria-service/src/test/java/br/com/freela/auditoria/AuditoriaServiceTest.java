package br.com.freela.auditoria;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuditoriaServiceTest {

    @Test
    void deveIgnorarEventoDuplicado() {
        EventoAuditoriaRepository repository = Mockito.mock(EventoAuditoriaRepository.class);
        Mockito.when(repository.existsByEventId(Mockito.any(UUID.class))).thenReturn(true);

        AuditoriaService service = new AuditoriaService(repository, new ObjectMapper());
        var eventId = UUID.randomUUID();
        var contratoId = UUID.randomUUID();

        service.registrarEvento(new ContratosEventEnvelope(eventId, "ContratoCriado", contratoId, Instant.now(), "corr-1", 1L, "v1",
                Map.of("freelancerId", UUID.randomUUID().toString(), "valor", "1500.00")), "corr-1");

        Mockito.verify(repository, Mockito.never()).save(Mockito.any());
    }
}
