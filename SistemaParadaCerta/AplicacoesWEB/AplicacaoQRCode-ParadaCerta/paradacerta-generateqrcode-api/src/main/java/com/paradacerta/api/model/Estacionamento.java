package com.paradacerta.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "Estacionamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Estacionamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "avaliacaoMedia", nullable = false, precision = 3, scale = 2)
    private BigDecimal avaliacaoMedia;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "endereco", nullable = false, length = 300)
    private String endereco;

    @Column(name = "cep", length = 8)
    private String cep;

    @Column(name = "logradouro", length = 200)
    private String logradouro;

    @Column(name = "numero", length = 10)
    private String numero;

    @Column(name = "complemento", length = 100)
    private String complemento;

    @Column(name = "bairro", length = 100)
    private String bairro;

    @Column(name = "cidade", length = 100)
    private String cidade;

    @Column(name = "uf", length = 2)
    private String uf;

    @Column(name = "precoHora", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoHora;

    @Column(name = "horarioAbertura")
    private LocalTime horarioAbertura;

    @Column(name = "horarioFechamento")
    private LocalTime horarioFechamento;

    @Column(name = "fotoPrincipal", length = 500)
    private String fotoPrincipal;

    @Column(name = "descricao", length = 1000)
    private String descricao;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo;

    @Column(name = "pixKey", length = 200)
    private String pixKey;

    @Column(name = "permiteReserva", nullable = false)
    private Boolean permiteReserva;

}
