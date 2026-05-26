package com.paradacerta.api.model;

/**
 * Tipo de cobrança/recorrência do plano.
 *
 * <ul>
 *   <li>{@link #MENSAL} — ciclo de 30 dias.</li>
 *   <li>{@link #ANUAL}  — ciclo de 365 dias.</li>
 *   <li>{@link #TRIAL}  — período de avaliação (apenas {@link PlanoTipo#BASIC}).</li>
 * </ul>
 */
public enum PlanoCobranca {
    MENSAL,
    ANUAL,
    TRIAL
}
