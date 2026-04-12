package com.paradacerta.api.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "FormaPagamento")
public class FormaPagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "clienteCPF", nullable = false, length = 11)
    private String clienteCPF;

    @Column(name = "tipoPagamento", nullable = false, length = 20)
    private String tipoPagamento;

    @Column(name = "numeroCartao", length = 19)
    private String numeroCartao;

    @Column(name = "nomeCartao", length = 100)
    private String nomeCartao;

    @Column(name = "validade", length = 5)
    private String validade;

    @Column(name = "bandeira", length = 30)
    private String bandeira;
}
