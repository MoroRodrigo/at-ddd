package br.com.freela.reputacao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReputacaoEventoProcessadoRepository extends JpaRepository<ReputacaoEventoProcessado, UUID> {
    boolean existsByEventId(UUID eventId);
}
