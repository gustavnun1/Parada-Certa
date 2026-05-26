package com.paradacerta.api.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Foto de um estacionamento.
 *
 * O caminho aponta para o arquivo dentro de {@code paradacerta.uploads.dir}
 * (servido como recurso estático em {@code /uploads/**}). A linha só é gravada
 * após o arquivo passar pelas validações de magic bytes e moderação Google Vision.
 *
 * Script SQL: 08-CREATE-EstacionamentoFoto.sql
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "EstacionamentoFoto")
public class EstacionamentoFoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "estacionamentoId", nullable = false)
    private Integer estacionamentoId;

    /** Caminho relativo a partir de {@code paradacerta.uploads.dir}. Exemplo: {@code estacionamento/12/uuid.jpg}. */
    @Column(name = "caminho", nullable = false, length = 500)
    private String caminho;

    @Column(name = "nomeOriginal", length = 255)
    private String nomeOriginal;

    @Column(name = "tipoMime", nullable = false, length = 50)
    private String tipoMime;

    @Column(name = "tamanhoBytes", nullable = false)
    private Long tamanhoBytes;

    @Column(name = "principal", nullable = false)
    private Boolean principal = false;

    @Column(name = "ordem", nullable = false)
    private Integer ordem = 0;

    @Column(name = "criadoEm", nullable = false)
    private LocalDateTime criadoEm;
}
