package com.paradacerta.api.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "VagasEstacionamento")
public class VagasEstacionamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private Integer estacionamentoId;

    @Column(nullable = false)
    private Integer qtdVagasTotais;

    @Column(nullable = false)
    private Integer qtdVagasDisponiveis;

    @Column(nullable = false)
    private Integer qtdVagasReservaveis = 0;

    @Column(nullable = false)
    private Integer qtdVagasReservadas = 0;
}
