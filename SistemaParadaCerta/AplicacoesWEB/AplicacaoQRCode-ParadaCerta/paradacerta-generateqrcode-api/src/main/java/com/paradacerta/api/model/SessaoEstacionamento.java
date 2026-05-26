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

    // Valores válidos para status — espelha o CHECK constraint do banco
    public static final String STATUS_ATIVA     = "ATIVA";
    public static final String STATUS_ENCERRADA = "ENCERRADA";
    public static final String STATUS_CANCELADA = "CANCELADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // BIGINT no banco → Long na JPA (consistente com a entidade da API principal)
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

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "qrCode", length = 64)
    private String qrCode;

    @Column(name = "reservado", nullable = false)
    private Boolean reservado;

    @Column(name = "placa", length = 7)
    private String placa;
}
