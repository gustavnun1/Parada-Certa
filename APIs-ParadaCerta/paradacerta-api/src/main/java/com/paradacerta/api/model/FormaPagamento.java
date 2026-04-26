package com.paradacerta.api.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@Entity
@Table(name = "FormaPagamento")
public class FormaPagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "clienteId", nullable = false)
    private Long clienteId;

    @Column(name = "tipoPagamento", nullable = false, length = 20)
    private String tipoPagamento;

    /**
     * Armazena APENAS os últimos 4 dígitos do cartão.
     * O número completo NUNCA é persistido no banco.
     */
    @ToString.Exclude
    @Column(name = "numeroCartao", length = 4)
    private String numeroCartao;

    @ToString.Exclude
    @Column(name = "nomeCartao", length = 100)
    private String nomeCartao;

    @Column(name = "validade", length = 5)
    private String validade;

    @Column(name = "bandeira", length = 30)
    private String bandeira;
}
