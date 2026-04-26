package com.paradacerta.api.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "Vaga")
public class Vaga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer estacionamentoId;

    @Column(nullable = false, length = 10)
    private String numero;

    @Column(nullable = false, length = 50)
    private String tipo;

    @Column(nullable = false)
    private Boolean ocupada = false;
}