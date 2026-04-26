package com.paradacerta.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "QrCodeEntrada")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrCodeEntrada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", nullable = false, length = 64)
    private String token;

    @Column(name = "estacionamentoId", nullable = false)
    private Integer estacionamentoId;

    @Column(name = "geradoPor", nullable = false)
    private Integer geradoPor;

    @Column(name = "geradoEm", nullable = false)
    private LocalDateTime geradoEm;

    @Column(name = "expiradoEm", nullable = false)
    private LocalDateTime expiradoEm;

    @Column(name = "status", nullable = false, length = 15)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estacionamentoId", insertable = false, updatable = false)
    private Estacionamento estacionamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "geradoPor", insertable = false, updatable = false)
    private AdmEstacionamento admin;
}
