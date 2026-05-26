package com.paradacerta.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    @Column(nullable = false, unique = true, length = 14)
    private String cnpj;

    @Column(nullable = false, length = 200)
    private String razaoSocial;

    @Column(length = 200)
    private String nomeFantasia;

    @Transient
    private Integer qtdVagasTotais;

    @Transient
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

    @Column(length = 200)
    private String pixKey;

    @Column(nullable = false)
    private Boolean permiteReserva = false;

    @Transient
    private Integer qtdVagasReservaveis = 0;

    @Transient
    private Integer qtdVagasReservadas = 0;

    // ── Endereço detalhado (Script 04-ALTER) ────────────────────────────────
    // Mantém-se a coluna [endereco] (texto livre) como espelho derivado dos
    // campos abaixo para compatibilidade com o app mobile e listagens existentes.
    @Column(length = 8)
    private String cep;

    @Column(length = 200)
    private String logradouro;

    @Column(length = 10)
    private String numero;

    @Column(length = 100)
    private String complemento;

    @Column(length = 100)
    private String bairro;

    @Column(length = 100)
    private String cidade;

    @Column(length = 2)
    private String uf;

    // ── Plano de assinatura (Script 07-ALTER) ────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "plano", length = 10)
    private PlanoTipo plano;

    @Column(name = "planoInicio")
    private LocalDateTime planoInicio;

    @Column(name = "planoFim")
    private LocalDateTime planoFim;

    @Enumerated(EnumType.STRING)
    @Column(name = "planoCobranca", length = 10)
    private PlanoCobranca planoCobranca;

    @JsonProperty("isPremium")
    public boolean getIsPremium() {
        return plano == PlanoTipo.PREMIUM
                && (planoFim == null || planoFim.isAfter(LocalDateTime.now()));
    }
}
