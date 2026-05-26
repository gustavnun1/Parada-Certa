package com.paradacerta.api.model;

/**
 * Tipos de plano de assinatura de um estacionamento.
 *
 * <ul>
 *   <li>{@link #BASIC}    — trial de 30 dias com as funcionalidades do Standard.</li>
 *   <li>{@link #STANDARD} — pago. Fotos até 3, avaliações, dashboard completo.</li>
 *   <li>{@link #PREMIUM}  — pago. Tudo do Standard + 5 fotos, destaque, relatórios regionais.</li>
 * </ul>
 */
public enum PlanoTipo {
    BASIC,
    STANDARD,
    PREMIUM
}
