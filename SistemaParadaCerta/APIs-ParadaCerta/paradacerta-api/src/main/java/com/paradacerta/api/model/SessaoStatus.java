package com.paradacerta.api.model;

public enum SessaoStatus {
    AGUARDANDO_CONFIRMACAO,
    EM_USO,
    ATIVA,
    ENCERRADA,
    CANCELADA;

    public boolean isAtivoOuAguardando() {
        return this == AGUARDANDO_CONFIRMACAO || this == EM_USO || this == ATIVA;
    }
}
