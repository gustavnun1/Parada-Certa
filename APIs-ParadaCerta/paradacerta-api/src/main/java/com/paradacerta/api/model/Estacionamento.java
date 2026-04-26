package com.paradacerta.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "Estacionamento")
public class Estacionamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false)
    private Integer qtdVagasTotais;

    @Column(nullable = false)
    private Integer qtdVagasDisponiveis;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal avaliacaoMedia;

    @Column(nullable = false, precision = 10, scale = 8)
    private BigDecimal  latitude;

    @Column(nullable = false, precision = 11, scale = 8)
    private BigDecimal  longitude;

    @Column(nullable = false, length = 300)
    private String endereco;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal  precoHora;

    private LocalTime horarioAbertura;

    private LocalTime horarioFechamento;

    @Column(length = 500)
    private String fotoPrincipal;

    @Column(length = 1000)
    private String descricao;

    @Column(nullable = false)
    private Boolean ativo = true;

    @JsonIgnore
    @Column(length = 200)
    private String pixKey;

    @Column(nullable = false)
    private Boolean permiteReserva = false;

    @Column(nullable = false)
    private Integer qtdVagasReservaveis = 0;
}