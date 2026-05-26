package com.paradacerta.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** DTO de retorno para listagem de fotos do estacionamento. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EstacionamentoFotoResponse {
    private Integer id;
    private Integer estacionamentoId;
    /** URL relativa para o front buscar a imagem ({@code /uploads/...}). */
    private String url;
    private String nomeOriginal;
    private String tipoMime;
    private Long tamanhoBytes;
    private Boolean principal;
    private Integer ordem;
    private LocalDateTime criadoEm;
}
