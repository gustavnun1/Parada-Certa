package com.paradacerta.api.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Atualização de capacidade de vagas (PUT /api/estacionamentos/{id}/vagas).
 * Disponíveis e Reservadas continuam sendo controladas pelo trigger TR_Sessao_AtualizaVagas.
 */
@Data
public class VagasUpdateRequest {

    @NotNull
    @Min(value = 0)
    private Integer qtdVagasTotais;

    @NotNull
    @Min(value = 0)
    private Integer qtdVagasReservaveis;
}
