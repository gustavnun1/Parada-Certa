package com.paradacerta.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AvaliacaoResponse {
    private Integer id;
    private Integer nota;
    private String comentario;
    private LocalDateTime dataAvaliacao;
}
