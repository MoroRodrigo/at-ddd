package br.com.freela.reputacao;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "reputacoes")
public class ReputacaoFreelancer {
    @Id
    private UUID freelancerId;

    @Column(nullable = false)
    private int contratosConcluidos;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    protected ReputacaoFreelancer() {
    }

    public ReputacaoFreelancer(UUID freelancerId) {
        this.freelancerId = freelancerId;
    }

    public void registrarContrato(BigDecimal valor) {
        contratosConcluidos++;
        valorTotal = valorTotal.add(valor);
    }

    public UUID getFreelancerId(){ return freelancerId; }
    public int getContratosConcluidos(){ return contratosConcluidos; }
    public BigDecimal getValorTotal(){ return valorTotal; }
}
