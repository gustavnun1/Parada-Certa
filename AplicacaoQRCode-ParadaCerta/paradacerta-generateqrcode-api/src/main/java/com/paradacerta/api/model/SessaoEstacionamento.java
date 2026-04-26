package com.paradacerta.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "SessaoEstacionamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessaoEstacionamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clienteId", nullable = false)
    private Integer clienteId;

    @Column(name = "estacionamentoId", nullable = false)
    private Integer estacionamentoId;

    @Column(name = "horaEntrada", nullable = false)
    private LocalDateTime horaEntrada;

    @Column(name = "horaSaida")
    private LocalDateTime horaSaida;

    @Column(name = "horaPagamento")
    private LocalDateTime horaPagamento;

    @Column(name = "valorPago", precision = 10, scale = 2)
    private BigDecimal valorPago;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "qrCode", length = 64)
    private String qrCode;

    @Column(name = "reservado", nullable = false)
    private Boolean reservado;
}
