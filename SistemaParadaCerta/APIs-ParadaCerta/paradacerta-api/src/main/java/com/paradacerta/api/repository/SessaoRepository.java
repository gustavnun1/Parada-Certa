package com.paradacerta.api.repository;

import com.paradacerta.api.model.SessaoEstacionamento;
import com.paradacerta.api.model.SessaoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SessaoRepository extends JpaRepository<SessaoEstacionamento, Long> {

    Optional<SessaoEstacionamento> findByClienteIdAndStatus(Long clienteId, SessaoStatus status);
    boolean existsByClienteIdAndStatus(Long clienteId, SessaoStatus status);
    boolean existsByClienteIdAndEstacionamentoIdAndReservadoTrueAndStatus(Long clienteId, Integer estacionamentoId, SessaoStatus status);
    Optional<SessaoEstacionamento> findByQrCode(String qrCode);

    // ── Sessão "viva" do motorista ─────────────────────────────────────────────
    // Cobre AGUARDANDO_CONFIRMACAO (reserva paga mas não confirmada),
    // EM_USO (reserva confirmada em curso) e ATIVA (entrada comum em curso).
    // Usado para a trava global "um motorista, uma sessão por vez".

    @Query("""
        SELECT s FROM SessaoEstacionamento s
        WHERE s.clienteId = :clienteId
          AND s.status IN (
              com.paradacerta.api.model.SessaoStatus.AGUARDANDO_CONFIRMACAO,
              com.paradacerta.api.model.SessaoStatus.EM_USO,
              com.paradacerta.api.model.SessaoStatus.ATIVA
          )
    """)
    Optional<SessaoEstacionamento> findSessaoVivaDoCliente(@Param("clienteId") Long clienteId);

    @Query("""
        SELECT COUNT(s) > 0 FROM SessaoEstacionamento s
        WHERE s.clienteId = :clienteId
          AND s.status IN (
              com.paradacerta.api.model.SessaoStatus.AGUARDANDO_CONFIRMACAO,
              com.paradacerta.api.model.SessaoStatus.EM_USO,
              com.paradacerta.api.model.SessaoStatus.ATIVA
          )
    """)
    boolean existsSessaoVivaDoCliente(@Param("clienteId") Long clienteId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SessaoEstacionamento s SET s.clienteId = null WHERE s.clienteId = :clienteId")
    int desassociarCliente(@Param("clienteId") Long clienteId);

    // ── Queries operacionais (admin web) ───────────────────────────────────────

    List<SessaoEstacionamento> findByEstacionamentoIdOrderByHoraEntradaDesc(Integer estacionamentoId);

    List<SessaoEstacionamento> findByEstacionamentoIdAndStatusOrderByHoraEntradaDesc(
            Integer estacionamentoId, SessaoStatus status);

    /**
     * Sessões com pelo menos uma data dentro do intervalo:
     * a) horaEntrada dentro do período, OU
     * b) horaPagamento dentro do período (para listar pagamentos de hoje).
     */
    @Query("""
        SELECT s FROM SessaoEstacionamento s
        WHERE s.estacionamentoId = :estId
          AND (
              (s.horaEntrada BETWEEN :inicio AND :fim)
              OR (s.horaPagamento IS NOT NULL AND s.horaPagamento BETWEEN :inicio AND :fim)
          )
        ORDER BY s.horaEntrada DESC
    """)
    List<SessaoEstacionamento> findByEstacionamentoIdAndPeriodo(
            @Param("estId") Integer estacionamentoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    @Query("""
        SELECT s FROM SessaoEstacionamento s
        WHERE s.estacionamentoId = :estId
          AND s.status = :status
          AND s.horaEntrada BETWEEN :inicio AND :fim
        ORDER BY s.horaEntrada DESC
    """)
    List<SessaoEstacionamento> findByEstacionamentoStatusPeriodo(
            @Param("estId") Integer estacionamentoId,
            @Param("status") SessaoStatus status,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    long countByEstacionamentoIdAndStatus(Integer estacionamentoId, SessaoStatus status);

    @Query("""
        SELECT COUNT(s) FROM SessaoEstacionamento s
        WHERE s.estacionamentoId = :estId
          AND s.status = :status
          AND s.horaPagamento BETWEEN :inicio AND :fim
    """)
    long countByEstacionamentoStatusAndPagamentoPeriodo(
            @Param("estId") Integer estacionamentoId,
            @Param("status") SessaoStatus status,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    /**
     * Pagamentos confirmados no período. Considera APENAS sessões ENCERRADAS
     * (pagamento efetivo do estacionamento). Sessões CANCELADAS — seja pelo
     * motorista (via app) ou manualmente pelo admin (via painel web) — ficam
     * de fora dos totais financeiros, mesmo que mantenham valorPago > 0 no
     * banco para fins de auditoria.
     */
    @Query("""
        SELECT s FROM SessaoEstacionamento s
        WHERE s.estacionamentoId = :estId
          AND s.status = com.paradacerta.api.model.SessaoStatus.ENCERRADA
          AND s.valorPago IS NOT NULL
          AND s.valorPago > 0
          AND s.horaPagamento BETWEEN :inicio AND :fim
        ORDER BY s.horaPagamento DESC
    """)
    List<SessaoEstacionamento> findPagamentosPagosPeriodo(
            @Param("estId") Integer estacionamentoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    /**
     * Lista pagamentos do estacionamento (sem filtro de período) — usada quando
     * filtro é "todos". Considera APENAS sessões ENCERRADAS com valorPago > 0.
     * Sessões CANCELADAS não somam receita (regra de negócio: cancelamento
     * manual pelo admin não gera faturamento).
     */
    @Query("""
        SELECT s FROM SessaoEstacionamento s
        WHERE s.estacionamentoId = :estId
          AND s.status = com.paradacerta.api.model.SessaoStatus.ENCERRADA
          AND s.valorPago IS NOT NULL
          AND s.valorPago > 0
        ORDER BY s.horaPagamento DESC
    """)
    List<SessaoEstacionamento> findPagamentosPagosTodos(@Param("estId") Integer estacionamentoId);

    /**
     * Sessões reservadas em aberto (com valorPago já cobrado antecipado) — para
     * "receita prevista" no painel admin. Cobre AGUARDANDO_CONFIRMACAO, EM_USO
     * e ATIVA (compat legado), excluindo reservas já encerradas/canceladas.
     */
    @Query("""
        SELECT s FROM SessaoEstacionamento s
        WHERE s.estacionamentoId = :estId
          AND s.reservado = true
          AND s.status IN (
              com.paradacerta.api.model.SessaoStatus.AGUARDANDO_CONFIRMACAO,
              com.paradacerta.api.model.SessaoStatus.EM_USO,
              com.paradacerta.api.model.SessaoStatus.ATIVA
          )
    """)
    List<SessaoEstacionamento> findReservasAtivas(@Param("estId") Integer estacionamentoId);

    /**
     * Reservas aguardando confirmação (pagas, motorista ainda não chegou).
     * Usada pelo painel admin para listar QRs de confirmação a imprimir.
     */
    @Query("""
        SELECT s FROM SessaoEstacionamento s
        WHERE s.estacionamentoId = :estId
          AND s.reservado = true
          AND s.status = com.paradacerta.api.model.SessaoStatus.AGUARDANDO_CONFIRMACAO
        ORDER BY s.inicioReservaPrevisto, s.horaEntrada
    """)
    List<SessaoEstacionamento> findReservasAguardandoConfirmacao(@Param("estId") Integer estacionamentoId);

    // ── Dashboard analítico ───────────────────────────────────────────────────

    /**
     * Conta entradas no período (todas as sessões com horaEntrada dentro do intervalo).
     * Quando inicio/fim forem null, conta tudo (period="todos").
     */
    @Query("""
        SELECT COUNT(s) FROM SessaoEstacionamento s
        WHERE s.estacionamentoId = :estId
          AND (:inicio IS NULL OR s.horaEntrada >= :inicio)
          AND (:fim    IS NULL OR s.horaEntrada <= :fim)
    """)
    long contarEntradasPeriodo(
            @Param("estId") Integer estacionamentoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    /** Conta sessões com um status específico no período. */
    @Query("""
        SELECT COUNT(s) FROM SessaoEstacionamento s
        WHERE s.estacionamentoId = :estId
          AND s.status = :status
          AND (:inicio IS NULL OR s.horaEntrada >= :inicio)
          AND (:fim    IS NULL OR s.horaEntrada <= :fim)
    """)
    long contarPorStatusPeriodo(
            @Param("estId") Integer estacionamentoId,
            @Param("status") SessaoStatus status,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    /** Conta sessões reservadas no período. */
    @Query("""
        SELECT COUNT(s) FROM SessaoEstacionamento s
        WHERE s.estacionamentoId = :estId
          AND s.reservado = true
          AND (:inicio IS NULL OR s.horaEntrada >= :inicio)
          AND (:fim    IS NULL OR s.horaEntrada <= :fim)
    """)
    long contarReservasPeriodo(
            @Param("estId") Integer estacionamentoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    /**
     * Pagamentos confirmados no período: lista bruta para agregação por dia em Java.
     * Evita depender do dialeto SQL Server para CAST(date) / HOUR().
     * Considera APENAS sessões ENCERRADAS — canceladas não entram em totais de receita.
     */
    @Query("""
        SELECT s FROM SessaoEstacionamento s
        WHERE s.estacionamentoId = :estId
          AND s.status = com.paradacerta.api.model.SessaoStatus.ENCERRADA
          AND s.valorPago IS NOT NULL
          AND s.valorPago > 0
          AND s.horaPagamento IS NOT NULL
          AND (:inicio IS NULL OR s.horaPagamento >= :inicio)
          AND (:fim    IS NULL OR s.horaPagamento <= :fim)
        ORDER BY s.horaPagamento
    """)
    List<SessaoEstacionamento> pagamentosNoPeriodo(
            @Param("estId") Integer estacionamentoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    /**
     * Entradas no período: lista bruta para agregação por hora em Java.
     */
    @Query("""
        SELECT s FROM SessaoEstacionamento s
        WHERE s.estacionamentoId = :estId
          AND (:inicio IS NULL OR s.horaEntrada >= :inicio)
          AND (:fim    IS NULL OR s.horaEntrada <= :fim)
        ORDER BY s.horaEntrada
    """)
    List<SessaoEstacionamento> entradasNoPeriodo(
            @Param("estId") Integer estacionamentoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    /**
     * Entradas agregadas por estacionamento para mapas/relatorios Premium.
     * Exclui sessoes canceladas para nao aquecer regioes por entradas estornadas.
     */
    @Query("""
        SELECT s.estacionamentoId, COUNT(s)
        FROM SessaoEstacionamento s
        WHERE s.estacionamentoId IN :estIds
          AND s.status <> com.paradacerta.api.model.SessaoStatus.CANCELADA
          AND (:inicio IS NULL OR s.horaEntrada >= :inicio)
          AND (:fim    IS NULL OR s.horaEntrada <= :fim)
        GROUP BY s.estacionamentoId
    """)
    List<Object[]> contarEntradasPorEstacionamentoPeriodo(
            @Param("estIds") List<Integer> estacionamentoIds,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );
}
