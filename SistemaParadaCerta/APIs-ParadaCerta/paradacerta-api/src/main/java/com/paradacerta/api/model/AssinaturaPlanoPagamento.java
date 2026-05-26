package com.paradacerta.api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registro de pagamento/assinatura de plano para um estacionamento.
 * Não armazena número completo do cartão nem CVV — apenas últimos 4 dígitos
 * e bandeira identificada (LGPD/PCI-DSS).
 */
@Data
@Entity
@Table(name = "AssinaturaPlanoPagamento")
public class AssinaturaPlanoPagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "estacionamentoId", nullable = false)
    private Integer estacionamentoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plano", nullable = false, length = 10)
    private PlanoTipo plano;

    @Enumerated(EnumType.STRING)
    @Column(name = "cobranca", nullable = false, length = 10)
    private PlanoCobranca cobranca;

    @Column(name = "valor", nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "status", nullable = false, length = 15)
    private String status;

    @Column(name = "dataPagamento", nullable = false)
    private LocalDateTime dataPagamento;

    @Column(name = "ultimos4", length = 4)
    private String ultimos4;

    @Column(name = "bandeira", length = 50)
    private String bandeira;

    @Column(name = "nomeCartao", length = 100)
    private String nomeCartao;
}
