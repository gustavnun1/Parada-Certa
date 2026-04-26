package com.paradacerta.api.service;

import com.paradacerta.api.exception.ConflictException;
import com.paradacerta.api.exception.UsuarioNaoEncontradoException;
import com.paradacerta.api.model.*;
import com.paradacerta.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;

@Service
@RequiredArgsConstructor
public class SessaoService {

    private final SessaoRepository        sessaoRepository;
    private final EstacionamentoRepository estacionamentoRepository;
    private final ClienteRepository        clienteRepository;
    private final VeiculoRepository        veiculoRepository;
    private final DataSource               dataSource;

    // ── Entrada ──────────────────────────────────────────────────────────────

    @Transactional
    public EntradaResponse registrarEntrada(EntradaRequest req) {
        // Resolve CPF → clienteId
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

            return new EntradaResponse(true, "Entrada registrada",
                    String.valueOf(sessaoId), horaEntrada);

        } catch (DataAccessException e) {
            int code = extractSqlErrorCode(e);
            if (code == 50001) throw new ConflictException("Estacionamento lotado");
            if (code == 50002) throw new UsuarioNaoEncontradoException("Estacionamento ou cliente não encontrado");
            throw e;
        }
    }

    // ── Pagamento ─────────────────────────────────────────────────────────────

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

    // ── Entrada via app (demo ou QR) ─────────────────────────────────────────

    @Transactional
    public EntradaResponse registrarEntradaApp(EntradaAppRequest req) {
        // Resolve CPF → clienteId
        Cliente cliente = clienteRepository.findByCpf(req.getCpfUsuario())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado"));
        Long clienteId = cliente.getId();

        // Verifica se já existe sessão ativa para evitar duplicata
        if (sessaoRepository.existsByClienteIdAndStatus(clienteId, SessaoStatus.ATIVA)) {
            SessaoEstacionamento sessaoExistente = sessaoRepository
                    .findByClienteIdAndStatus(clienteId, SessaoStatus.ATIVA)
                    .orElseThrow();
            return new EntradaResponse(true, "Sessão já ativa",
                    String.valueOf(sessaoExistente.getId()), sessaoExistente.getHoraEntrada());
        }

        LocalDateTime agora = LocalDateTime.now();
        String qrCode = java.util.UUID.randomUUID().toString();

        SessaoEstacionamento sessao = new SessaoEstacionamento();
        sessao.setClienteId(clienteId);
        sessao.setEstacionamentoId(req.getEstacionamentoId());
        sessao.setHoraEntrada(agora);
        sessao.setStatus(SessaoStatus.ATIVA);
        sessao.setQrCode(qrCode);

        sessaoRepository.save(sessao);

        return new EntradaResponse(true, "Entrada registrada", String.valueOf(sessao.getId()), agora);
    }

    // ── Sessão ativa ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Optional<SessaoAtivaResponse> buscarSessaoAtiva(String cpf) {
        Cliente cliente = clienteRepository.findByCpf(cpf).orElse(null);
        if (cliente == null) return Optional.empty();

        Long clienteId = cliente.getId();

        Optional<SessaoEstacionamento> sessaoOpt =
                sessaoRepository.findByClienteIdAndStatus(clienteId, SessaoStatus.ATIVA);

        if (sessaoOpt.isEmpty()) {
            return Optional.empty();
        }

        SessaoEstacionamento sessao = sessaoOpt.get();

        Estacionamento estacionamento = estacionamentoRepository
                .findById(sessao.getEstacionamentoId())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Estacionamento não encontrado"));

        String modeloVeiculo = null;
        if (cliente.getPlaca() != null) {
            modeloVeiculo = veiculoRepository.findById(cliente.getPlaca())
                    .map(Veiculo::getNome)
                    .orElse(null);
        }

        long horaEntradaMs = sessao.getHoraEntrada()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        return Optional.of(new SessaoAtivaResponse(
                String.valueOf(sessao.getId()),
                estacionamento.getId(),
                estacionamento.getNome(),
                estacionamento.getPixKey(),
                horaEntradaMs,
                estacionamento.getPrecoHora(),
                cliente.getPlaca(),
                modeloVeiculo,
                Boolean.TRUE.equals(sessao.getReservado())
        ));
    }

    // ── Encerrar sessão via app (pagamento confirmado no app) ─────────────────

    @Transactional
    public void encerrarSessao(String sessaoId, BigDecimal valorPago) {
        Long id;
        try {
            id = Long.parseLong(sessaoId);
        } catch (NumberFormatException e) {
            throw new UsuarioNaoEncontradoException("sessaoId inválido: " + sessaoId);
        }

        SessaoEstacionamento sessao = sessaoRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Sessão não encontrada: " + sessaoId));

        if (sessao.getStatus() != SessaoStatus.ATIVA) {
            // Sessão já encerrada ou cancelada — idempotente
            return;
        }

        LocalDateTime agora = LocalDateTime.now();
        int estacionamentoId = sessao.getEstacionamentoId();
        sessao.setStatus(SessaoStatus.ENCERRADA);
        sessao.setHoraSaida(agora);
        sessao.setHoraPagamento(agora);
        if (valorPago != null && valorPago.compareTo(BigDecimal.ZERO) > 0) {
            sessao.setValorPago(valorPago);
        }
        sessaoRepository.save(sessao);
    }

    // ── helper ────────────────────────────────────────────────────────────────

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
