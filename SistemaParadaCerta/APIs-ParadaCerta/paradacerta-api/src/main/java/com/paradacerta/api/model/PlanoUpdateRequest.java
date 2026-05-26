package com.paradacerta.api.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Mudança de plano (PUT /api/estacionamentos/{id}/plano).
 *
 * Regras de combinação:
 *  - BASIC          + TRIAL  -> reinicia trial de 30 dias.
 *  - STANDARD       + MENSAL -> 30 dias.
 *  - STANDARD       + ANUAL  -> 365 dias.
 *  - PREMIUM        + MENSAL -> 30 dias.
 *  - PREMIUM        + ANUAL  -> 365 dias.
 *  - Outras combinações são rejeitadas com 400.
 */
@Data
public class PlanoUpdateRequest {

    @NotNull(message = "Tipo do plano é obrigatório")
    private PlanoTipo tipo;

    @NotNull(message = "Cobrança é obrigatória")
    private PlanoCobranca cobranca;
}
