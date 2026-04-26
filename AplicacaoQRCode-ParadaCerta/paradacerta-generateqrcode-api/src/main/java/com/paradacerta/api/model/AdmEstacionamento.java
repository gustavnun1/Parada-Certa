package com.paradacerta.api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "AdmEstacionamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdmEstacionamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "estacionamentoId", nullable = false)
    private Integer estacionamentoId;

    @Column(name = "usuario", nullable = false, length = 50)
    private String usuario;

    @Column(name = "senhaHash", nullable = false, length = 255)
    private String senhaHash;

    @Column(name = "nomeCompleto", nullable = false, length = 100)
    private String nomeCompleto;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estacionamentoId", insertable = false, updatable = false)
    private Estacionamento estacionamento;
}
