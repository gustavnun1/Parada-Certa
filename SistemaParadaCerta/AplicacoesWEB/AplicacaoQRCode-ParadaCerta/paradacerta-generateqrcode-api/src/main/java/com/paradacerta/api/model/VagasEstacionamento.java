package com.paradacerta.api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "VagasEstacionamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VagasEstacionamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "estacionamentoId", nullable = false, unique = true)
    private Integer estacionamentoId;

    @Column(name = "qtdVagasTotais", nullable = false)
    private Integer qtdVagasTotais;

    @Column(name = "qtdVagasDisponiveis", nullable = false)
    private Integer qtdVagasDisponiveis;

    @Column(name = "qtdVagasReservaveis", nullable = false)
    private Integer qtdVagasReservaveis;

    @Column(name = "qtdVagasReservadas", nullable = false)
    private Integer qtdVagasReservadas;
}
