package com.paradacerta.api.service;

import com.paradacerta.api.exception.ConflictException;
import com.paradacerta.api.exception.RequisicaoInvalidaException;
import com.paradacerta.api.exception.UsuarioNaoEncontradoException;
import com.paradacerta.api.model.*;
import com.paradacerta.api.repository.VagasEstacionamentoRepository;
import com.paradacerta.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;

@Service
@RequiredArgsConstructor
public class SessaoService {
    private static final ZoneId ZONE_SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final Duration TOLERANCIA_RELOGIO_DISPOSITIVO = Duration.ofMinutes(10);

    private final SessaoRepository              sessaoRepository;
    private final EstacionamentoRepository      estacionamentoRepository;
    private final VagasEstacionamentoRepository vagasRepository;
    private final ClienteRepository             clienteRepository;
    private final VeiculoRepository             veiculoRepository;
    private final DataSource                    dataSource;

    // ── Entrada via QR Code (chama SP com validação ACID completa) ───────────

    @Transactional
    public EntradaResponse registrarEntrada(EntradaRequest req) {
        Cliente cliente = clienteRepository.findByCpf(req.getCpfUsuario())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado"));
        Long clienteId = cliente.getId();

        try {
            SimpleJdbcCall call = new SimpleJdbcCall(dataSource)
                    .withoutProcedureColumnMetaDataAccess()
                    .withProcedureName("sp_RegistrarEntrada")
                    .declareParameters(
                            new SqlParameter("qrCode", Types.VARCHAR),
                            new SqlParameter("clienteId", Types.BIGINT),
                            new SqlParameter("estacionamentoId", Types.INTEGER),
                            new SqlOutParameter("sessaoId", Types.BIGINT),
                            new SqlOutParameter("horaEntrada", Types.TIMESTAMP)
                    );

            Map<String, Object> result = call.execute(
                    new MapSqlParameterSource()
                            .addValue("qrCode", req.getQrCode())
                            .addValue("clienteId", clienteId)
                            .addValue("estacionamentoId", req.getEstacionamentoId())
            );

            Long sessaoId = (Long) result.get("sessaoId");
            LocalDateTime horaEntrada = ((Timestamp) result.get("horaEntrada")).toLocalDateTime();

            String pixKey = estacionamentoRepository.findById(req.getEstacionamentoId())
                    .map(Estacionamento::getPixKey)
                    .orElse(null);

            return new EntradaResponse(true, "Entrada registrada",
                    String.valueOf(sessaoId), horaEntrada, pixKey);

        } catch (DataAccessException e) {
            int code = extractSqlErrorCode(e);
            if (code == 50001) throw new ConflictException("Estacionamento lotado");
            if (code == 50002) throw new UsuarioNaoEncontradoException("Estacionamento ou cliente não encontrado");
            if (code == 50003) throw new RequisicaoInvalidaException("QR Code já utilizado ou inválido");
            if (code == 50004) throw new UsuarioNaoEncontradoException("QR Code não encontrado");
            if (code == 50005) throw new RequisicaoInvalidaException("QR Code expirado. Solicite um novo ao operador");
            throw e;
        }
    }

    // ── Pagamento via QR Code (chama SP com UPDATE atômico) ──────────────────

    @Transactional
    public PagamentoResponse registrarPagamento(PagamentoRequest req) {
        try {
            SimpleJdbcCall call = new SimpleJdbcCall(dataSource)
                    .withoutProcedureColumnMetaDataAccess()
                    .withProcedureName("sp_RegistrarPagamento")
                    .declareParameters(
                            new SqlParameter("qrCode", Types.VARCHAR),
                            new SqlParameter("valorPago", Types.DECIMAL),
                            new SqlOutParameter("horaSaida", Types.TIMESTAMP)
                    );

            Map<String, Object> result = call.execute(
                    new MapSqlParameterSource()
                            .addValue("qrCode", req.getQrCode())
                            .addValue("valorPago", req.getValorPago())
            );

            LocalDateTime horaSaida = ((Timestamp) result.get("horaSaida")).toLocalDateTime();

            return new PagamentoResponse(true, "Pagamento registrado",
                    req.getValorPago(), horaSaida);

        } catch (DataAccessException e) {
            int code = extractSqlErrorCode(e);
            if (code == 50002) throw new UsuarioNaoEncontradoException("QR Code inválido ou sessão já encerrada");
            throw e;
        }
    }

    // ── Entrada via app (demo — sem QR físico) ───────────────────────────────

    @Transactional
    public EntradaResponse registrarEntradaApp(EntradaAppRequest req) {
        Cliente cliente = clienteRepository.findByCpf(req.getCpfUsuario())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado"));
        Long clienteId = cliente.getId();

        // Idempotente para entrada comum (ATIVA já desse cliente): retorna a mesma.
        Optional<SessaoEstacionamento> sessaoAtivaComum = sessaoRepository
                .findByClienteIdAndStatus(clienteId, SessaoStatus.ATIVA);
        if (sessaoAtivaComum.isPresent()) {
            SessaoEstacionamento sessaoExistente = sessaoAtivaComum.get();
            String pixKeyExistente = estacionamentoRepository
                    .findById(sessaoExistente.getEstacionamentoId())
                    .map(Estacionamento::getPixKey)
                    .orElse(null);
            return new EntradaResponse(true, "Sessão já ativa",
                    String.valueOf(sessaoExistente.getId()),
                    sessaoExistente.getHoraEntrada(),
                    pixKeyExistente);
        }

        // Bloqueia se houver reserva AGUARDANDO_CONFIRMACAO ou EM_USO
        if (sessaoRepository.existsSessaoVivaDoCliente(clienteId)) {
            throw new ConflictException(
                "Você já possui uma reserva em andamento. Finalize ou cancele antes de iniciar uma nova entrada."
            );
        }

        // Verifica se o estacionamento existe, está ativo e tem vagas
        Estacionamento est = estacionamentoRepository.findById(req.getEstacionamentoId())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Estacionamento não encontrado"));

        if (!Boolean.TRUE.equals(est.getAtivo())) {
            throw new RequisicaoInvalidaException("Estacionamento inativo");
        }

        int vagasDisponiveis = vagasRepository.findByEstacionamentoId(req.getEstacionamentoId())
                .map(VagasEstacionamento::getQtdVagasDisponiveis)
                .orElse(0);
        if (vagasDisponiveis <= 0) {
            throw new ConflictException("Estacionamento sem vagas disponíveis");
        }

        LocalDateTime agora = horaEntradaApp(req.getHoraEntradaDispositivoMs());
        String qrCode = java.util.UUID.randomUUID().toString();

        SessaoEstacionamento sessao = new SessaoEstacionamento();
        sessao.setClienteId(clienteId);
        sessao.setEstacionamentoId(req.getEstacionamentoId());
        sessao.setHoraEntrada(agora);
        sessao.setStatus(SessaoStatus.ATIVA);
        sessao.setQrCode(qrCode);
        sessao.setPlaca(req.getPlaca() != null ? req.getPlaca().toUpperCase() : null);

        sessaoRepository.save(sessao);

        return new EntradaResponse(true, "Entrada registrada",
                String.valueOf(sessao.getId()), agora, est.getPixKey());
    }

    @Transactional
    public EntradaResponse vincularEntradaKiosk(EntradaKioskRequest req) {
        Cliente cliente = clienteRepository.findByCpf(req.getCpfUsuario())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuario nao encontrado"));
        Long clienteId = cliente.getId();

        String token = req.getToken() != null ? req.getToken().trim() : "";
        SessaoEstacionamento sessao = sessaoRepository.findByQrCode(token)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Sessao do QR Code nao encontrada"));

        if (sessao.getStatus() != SessaoStatus.ATIVA) {
            throw new RequisicaoInvalidaException("Sessao do QR Code nao esta ativa");
        }

        if (sessao.getClienteId() != null && !sessao.getClienteId().equals(clienteId)) {
            throw new ConflictException("QR Code ja vinculado a outro motorista");
        }

        Optional<SessaoEstacionamento> vivaDoCliente =
                sessaoRepository.findSessaoVivaDoCliente(clienteId);
        if (vivaDoCliente.isPresent() && !vivaDoCliente.get().getId().equals(sessao.getId())) {
            throw new ConflictException("Usuario ja possui uma sessao ou reserva em andamento");
        }

        sessao.setClienteId(clienteId);
        sessao.setPlaca(req.getPlaca() != null ? req.getPlaca().toUpperCase() : null);
        sessao.setReservado(Boolean.TRUE.equals(sessao.getReservado()));
        sessaoRepository.save(sessao);

        String pixKey = estacionamentoRepository.findById(sessao.getEstacionamentoId())
                .map(Estacionamento::getPixKey)
                .orElse(null);

        return new EntradaResponse(true, "Entrada do kiosk vinculada",
                String.valueOf(sessao.getId()), sessao.getHoraEntrada(), pixKey);
    }

    // ── Sessão ativa ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Optional<SessaoAtivaResponse> buscarSessaoAtiva(String cpf) {
        Cliente cliente = clienteRepository.findByCpf(cpf).orElse(null);
        if (cliente == null) return Optional.empty();

        // Retorna sessão "viva": AGUARDANDO_CONFIRMACAO, EM_USO ou ATIVA.
        Optional<SessaoEstacionamento> sessaoOpt =
                sessaoRepository.findSessaoVivaDoCliente(cliente.getId());

        if (sessaoOpt.isEmpty()) {
            return Optional.empty();
        }

        SessaoEstacionamento sessao = sessaoOpt.get();

        Estacionamento estacionamento = estacionamentoRepository
                .findById(sessao.getEstacionamentoId())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Estacionamento não encontrado"));

        String placaSessao = sessao.getPlaca();
        String modeloVeiculo = null;
        if (placaSessao != null) {
            modeloVeiculo = veiculoRepository.findById(placaSessao)
                    .map(Veiculo::getNome)
                    .orElse(null);
        }

        long horaEntradaMs = toEpochMillisSaoPaulo(sessao.getHoraEntrada());
        Long inicioReservaPrevistoMs = sessao.getInicioReservaPrevisto() != null
                ? toEpochMillisSaoPaulo(sessao.getInicioReservaPrevisto())
                : null;
        Long dataHoraConfirmacaoMs = sessao.getDataHoraConfirmacao() != null
                ? toEpochMillisSaoPaulo(sessao.getDataHoraConfirmacao())
                : null;

        return Optional.of(new SessaoAtivaResponse(
                String.valueOf(sessao.getId()),
                estacionamento.getId(),
                estacionamento.getNome(),
                estacionamento.getPixKey(),
                horaEntradaMs,
                inicioReservaPrevistoMs,
                dataHoraConfirmacaoMs,
                estacionamento.getPrecoHora(),
                placaSessao,
                modeloVeiculo,
                Boolean.TRUE.equals(sessao.getReservado()),
                sessao.getStatus(),
                sessao.getValorPago()
        ));
    }

    // ── Encerrar sessão via app (pagamento confirmado manualmente) ────────────
    //
    // Para sessões NÃO-reservadas, o backend é a fonte da verdade do valor:
    // recalcula a cobrança aplicando as regras (mínimo 1h, blocos de 30min) e
    // ignora o valorPago enviado pelo cliente. O parâmetro `valorPago` ainda é
    // aceito para compatibilidade — apenas usado em reservas para acumular o
    // valor extra à 1ª hora já cobrada.

    public void encerrarSessao(String sessaoId, BigDecimal valorPago) {
        encerrarSessao(sessaoId, valorPago, null);
    }

    @Transactional
    public void encerrarSessao(String sessaoId, BigDecimal valorPago, String cpf) {
        Long id;
        try {
            id = Long.parseLong(sessaoId);
        } catch (NumberFormatException e) {
            throw new UsuarioNaoEncontradoException("sessaoId inválido: " + sessaoId);
        }

        SessaoEstacionamento sessao = sessaoRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Sessão não encontrada: " + sessaoId));

        if (cpf != null && !cpf.isBlank()) {
            Cliente cliente = clienteRepository.findByCpf(cpf)
                    .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuario nao encontrado"));
            if (sessao.getClienteId() == null || !sessao.getClienteId().equals(cliente.getId())) {
                throw new ConflictException("Esta sessao pertence a outro motorista");
            }
        }

        if (sessao.getStatus() != SessaoStatus.ATIVA) {
            return; // idempotente
        }

        LocalDateTime agora = nowSaoPaulo();
        sessao.setStatus(SessaoStatus.ENCERRADA);
        sessao.setHoraSaida(agora);
        sessao.setHoraPagamento(agora);

        if (Boolean.TRUE.equals(sessao.getReservado())) {
            // Reserva: a 1ª hora já está em valorPago; soma cobrança extra se houver
            if (valorPago != null && valorPago.compareTo(BigDecimal.ZERO) > 0
                    && sessao.getValorPago() != null) {
                sessao.setValorPago(sessao.getValorPago().add(valorPago));
            } else if (valorPago != null && valorPago.compareTo(BigDecimal.ZERO) > 0) {
                sessao.setValorPago(valorPago);
            }
        } else {
            // Sessão comum: recalcula valor a partir da hora de entrada
            Estacionamento est = estacionamentoRepository.findById(sessao.getEstacionamentoId())
                    .orElseThrow(() -> new UsuarioNaoEncontradoException("Estacionamento não encontrado"));
            BigDecimal valorCalculado = calcularValorEstadia(
                    precoHoraParaCobranca(sessao, est), sessao.getHoraEntrada(), agora);
            sessao.setValorPago(valorCalculado);
        }

        sessaoRepository.save(sessao);
    }

    // ── Cálculo de cobrança (consulta) ────────────────────────────────────────

    @Transactional(readOnly = true)
    public CobrancaEstadiaResponse calcularCobrancaEstadia(String sessaoId) {
        Long id;
        try {
            id = Long.parseLong(sessaoId);
        } catch (NumberFormatException e) {
            throw new UsuarioNaoEncontradoException("sessaoId inválido: " + sessaoId);
        }

        SessaoEstacionamento sessao = sessaoRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Sessão não encontrada"));

        if (Boolean.TRUE.equals(sessao.getReservado())) {
            throw new RequisicaoInvalidaException(
                    "Esta sessão é uma reserva — use /api/reserva/{id}/calculo-extra");
        }

        Estacionamento est = estacionamentoRepository.findById(sessao.getEstacionamentoId())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Estacionamento não encontrado"));

        LocalDateTime fim = sessao.getStatus() == SessaoStatus.ATIVA
                ? nowSaoPaulo()
                : (sessao.getHoraSaida() != null ? sessao.getHoraSaida() : nowSaoPaulo());

        long minutosPermanencia = Math.max(
                0L, Duration.between(sessao.getHoraEntrada(), fim).toMinutes());
        long minutosCobrados   = arredondarMinutosCobrados(minutosPermanencia);
        BigDecimal precoHora   = precoHoraParaCobranca(sessao, est);
        BigDecimal valor       = calcularValorPorMinutos(precoHora, minutosCobrados);

        return new CobrancaEstadiaResponse(
                precoHora, minutosPermanencia, minutosCobrados, valor);
    }

    // ── Regras de cobrança (fonte da verdade) ─────────────────────────────────

    /**
     * Calcula o valor a cobrar pela estadia entre duas datas.
     * Regras:
     *  - tempo ≤ 60 minutos cobra 60 minutos (1 hora cheia);
     *  - acima de 60 minutos, arredonda para cima em blocos de 30 minutos.
     */
    private BigDecimal calcularValorEstadia(
            BigDecimal precoHora, LocalDateTime entrada, LocalDateTime saida) {
        long minutos = Math.max(0L, Duration.between(entrada, saida).toMinutes());
        long cobrados = arredondarMinutosCobrados(minutos);
        return calcularValorPorMinutos(precoHora, cobrados);
    }

    private long arredondarMinutosCobrados(long minutos) {
        if (minutos <= 60) return 60;
        long blocos = (minutos + 29) / 30; // ceil(minutos / 30)
        return blocos * 30;
    }

    private BigDecimal calcularValorPorMinutos(BigDecimal precoHora, long minutosCobrados) {
        if (precoHora == null) return BigDecimal.ZERO;
        return precoHora
                .multiply(BigDecimal.valueOf(minutosCobrados))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private BigDecimal precoHoraParaCobranca(SessaoEstacionamento sessao, Estacionamento est) {
        if (sessao.getStatus() == SessaoStatus.ATIVA
                && !Boolean.TRUE.equals(sessao.getReservado())
                && sessao.getValorPago() != null
                && sessao.getValorPago().compareTo(BigDecimal.ZERO) > 0) {
            return sessao.getValorPago();
        }
        return est.getPrecoHora();
    }

    private LocalDateTime horaEntradaApp(Long horaEntradaDispositivoMs) {
        LocalDateTime agora = nowSaoPaulo();
        if (horaEntradaDispositivoMs == null || horaEntradaDispositivoMs <= 0) {
            return agora;
        }

        Instant instanteDispositivo = Instant.ofEpochMilli(horaEntradaDispositivoMs);
        Instant instanteServidor = agora.atZone(ZONE_SAO_PAULO).toInstant();
        Duration diferenca = Duration.between(instanteDispositivo, instanteServidor).abs();
        if (diferenca.compareTo(TOLERANCIA_RELOGIO_DISPOSITIVO) > 0) {
            return agora;
        }
        return LocalDateTime.ofInstant(instanteDispositivo, ZONE_SAO_PAULO);
    }

    private LocalDateTime nowSaoPaulo() {
        return LocalDateTime.now(ZONE_SAO_PAULO);
    }

    private long toEpochMillisSaoPaulo(LocalDateTime dataHora) {
        return dataHora.atZone(ZONE_SAO_PAULO).toInstant().toEpochMilli();
    }

    private int extractSqlErrorCode(DataAccessException e) {
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof SQLException) {
                return ((SQLException) cause).getErrorCode();
            }
            cause = cause.getCause();
        }
        return -1;
    }
}
