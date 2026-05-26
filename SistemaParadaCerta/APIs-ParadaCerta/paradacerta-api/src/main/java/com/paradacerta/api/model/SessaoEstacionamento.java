package com.paradacerta.api.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "SessaoEstacionamento")
public class SessaoEstacionamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clienteId")
    private Long clienteId;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private SessaoStatus status;

    @Column(name = "qrCode", length = 64)
    private String qrCode;

    @Column(name = "reservado", nullable = false)
    private Boolean reservado = false;

    @Column(name = "placa", length = 7)
    private String placa;
}
