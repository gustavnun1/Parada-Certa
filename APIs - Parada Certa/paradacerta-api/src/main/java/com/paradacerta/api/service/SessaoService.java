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
        try {
            SimpleJdbcCall call = new SimpleJdbcCall(dataSource)
                    .withoutProcedureColumnMetaDataAccess()
                    .withProcedureName("sp_RegistrarEntrada")
                    .declareParameters(
                            new SqlParameter("qrCode", Types.VARCHAR),
                            new SqlParameter("cpfUsuario", Types.VARCHAR),
                            new SqlParameter("estacionamentoId", Types.INTEGER),
                            new SqlOutParameter("sessaoId", Types.BIGINT),
                            new SqlOutParameter("horaEntrada", Types.TIMESTAMP)
                    );

            Map<String, Object> result = call.execute(
                    new MapSqlParameterSource()
                            .addValue("qrCode", req.getQrCode())
                            .addValue("cpfUsuario", req.getCpfUsuario())
                            .addValue("estacionamentoId", req.getEstacionamentoId())
            );

            Long sessaoId = (Long) result.get("sessaoId");
            LocalDateTime horaEntrada = ((Timestamp) result.get("horaEntrada")).toLocalDateTime();

            estacionamentoRepository.decrementarVaga(req.getEstacionamentoId());

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

            sessaoRepository.findByQrCode(req.getQrCode())
                    .ifPresent(s -> estacionamentoRepository.incrementarVaga(s.getEstacionamentoId()));

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
        // Verifica se já existe sessão ativa para evitar duplicata
        if (sessaoRepository.existsByCpfUsuarioAndStatus(req.getCpfUsuario(), SessaoStatus.ATIVA)) {
            SessaoEstacionamento sessaoExistente = sessaoRepository
                    .findByCpfUsuarioAndStatus(req.getCpfUsuario(), SessaoStatus.ATIVA)
                    .orElseThrow();
            return new EntradaResponse(true, "Sessão já ativa",
                    String.valueOf(sessaoExistente.getId()), sessaoExistente.getHoraEntrada());
        }

        LocalDateTime agora = LocalDateTime.now();
        String qrCode = java.util.UUID.randomUUID().toString();

        SessaoEstacionamento sessao = new SessaoEstacionamento();
        sessao.setCpfUsuario(req.getCpfUsuario());
        sessao.setEstacionamentoId(req.getEstacionamentoId());
        sessao.setHoraEntrada(agora);
        sessao.setStatus(SessaoStatus.ATIVA);
        sessao.setQrCode(qrCode);

        sessaoRepository.save(sessao);
        estacionamentoRepository.decrementarVaga(req.getEstacionamentoId());

        return new EntradaResponse(true, "Entrada registrada", String.valueOf(sessao.getId()), agora);
    }

    // ── Sessão ativa ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Optional<SessaoAtivaResponse> buscarSessaoAtiva(String cpf) {

        Optional<SessaoEstacionamento> sessaoOpt =
                sessaoRepository.findByCpfUsuarioAndStatus(cpf, SessaoStatus.ATIVA);

        if (sessaoOpt.isEmpty()) {
            return Optional.empty();
        }

        SessaoEstacionamento sessao = sessaoOpt.get();

        Estacionamento estacionamento = estacionamentoRepository
                .findById(sessao.getEstacionamentoId())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Estacionamento não encontrado"));

        Cliente cliente = clienteRepository.findByCpf(cpf)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado"));

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
                modeloVeiculo
        ));
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
