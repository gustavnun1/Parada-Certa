package com.example.paradacerta.models

data class SessaoAtiva(
    val estacionamentoId: Int,
    val estacionamentoNome: String,
    val modeloVeiculo: String,
    val placa: String,
    val precoHora: Double,
    val horaEntrada: Long = System.currentTimeMillis(),
    val inicioReservaPrevisto: Long? = null,
    val dataHoraConfirmacao: Long? = null,
    val sessaoId: String = "",
    val pixKey: String = "",
    val reservado: Boolean = false,
    val horarioReserva: String? = null,
    val status: SessaoStatus = SessaoStatus.ATIVA,
    val valorPagoAntecipado: Double = 0.0
) {
    /** Reserva paga, mas o motorista ainda não escaneou o QR no estacionamento. */
    val aguardandoConfirmacao: Boolean
        get() = reservado && status == SessaoStatus.AGUARDANDO_CONFIRMACAO

    /** Reserva confirmada presencialmente, vaga em uso. */
    val emUso: Boolean
        get() = reservado && (status == SessaoStatus.EM_USO ||
                // compat com reservas legado pré-migração
                (status == SessaoStatus.ATIVA && dataHoraConfirmacao != null))
}
