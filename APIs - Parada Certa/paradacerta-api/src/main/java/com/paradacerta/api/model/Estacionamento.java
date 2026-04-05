package com.paradacerta.api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "Estacionamento")
public class Estacionamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private Integer qtdVagasTotais;

    @Column(nullable = false)
    private Integer qtdVagasDisponiveis;

    @Column(nullable = false)
    private Double avaliacaoMedia;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private String endereco;

    @Column(nullable = false)
    private Double precoHora;

    private LocalTime horarioAbertura;

    private LocalTime horarioFechamento;

    @Column(length = 500)
    private String fotoPrincipal; // URL ou base64 da foto

    @Column(length = 1000)
    private String descricao;

    @Column(nullable = false)
    private Boolean ativo = true;
}