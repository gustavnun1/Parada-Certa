package com.paradacerta.api.controller;

import com.paradacerta.api.model.ApiResponse;
import com.paradacerta.api.model.CancelarSessaoRequest;
import com.paradacerta.api.model.DashboardAnaliticoResponse;
import com.paradacerta.api.model.MapaCalorOperacaoResponse;
import com.paradacerta.api.model.OperacaoResumoResponse;
import com.paradacerta.api.model.RelatorioRegionalResponse;
import com.paradacerta.api.model.SessaoAdminResponse;
import com.paradacerta.api.service.OperacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Visão operacional do administrador web (aba Reservas / Operação). */
@RestController
@RequestMapping("/api/operacao")
@RequiredArgsConstructor
public class OperacaoController {

    private final OperacaoService operacaoService;

    /**
     * GET /api/operacao/{estacionamentoId}/sessoes
     * Filtros opcionais:
     *  - status     = ATIVA | ENCERRADA | CANCELADA | todos
     *  - dataInicio = yyyy-MM-dd
     *  - dataFim    = yyyy-MM-dd
     */
    @GetMapping("/{estacionamentoId}/sessoes")
    public ResponseEntity<List<SessaoAdminResponse>> listarSessoes(
            @PathVariable Integer estacionamentoId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim
    ) {
        return ResponseEntity.ok(
                operacaoService.listarSessoes(estacionamentoId, status, dataInicio, dataFim)
        );
    }

    /** GET /api/operacao/{estacionamentoId}/resumo — cards do dashboard operacional. */
    @GetMapping("/{estacionamentoId}/resumo")
    public ResponseEntity<OperacaoResumoResponse> resumo(@PathVariable Integer estacionamentoId) {
        return ResponseEntity.ok(operacaoService.resumo(estacionamentoId));
    }

    /**
     * GET /api/operacao/{estacionamentoId}/analitico?periodo=hoje|semana|mes|ano|todos
     * Indicadores agregados do dashboard analítico (ocupação, receita por dia,
     * ticket médio, ocupação por hora, top horários, totais por status).
     */
    @GetMapping("/{estacionamentoId}/analitico")
    public ResponseEntity<DashboardAnaliticoResponse> analitico(
            @PathVariable Integer estacionamentoId,
            @RequestParam(name = "periodo", defaultValue = "hoje") String periodo
    ) {
        return ResponseEntity.ok(operacaoService.analitico(estacionamentoId, periodo));
    }

    /**
     * GET /api/operacao/{estacionamentoId}/regional?periodo=...
     * Relatório regional (dados agregados/anonimizados). Disponível APENAS para PREMIUM ativo.
     * Retorna 403 com mensagem clara caso o estacionamento não tenha o plano correto.
     */
    @GetMapping("/{estacionamentoId}/regional")
    public ResponseEntity<RelatorioRegionalResponse> regional(
            @PathVariable Integer estacionamentoId,
            @RequestParam(name = "periodo", defaultValue = "mes") String periodo
    ) {
        return ResponseEntity.ok(operacaoService.relatorioRegional(estacionamentoId, periodo));
    }

    /**
     * GET /api/operacao/{estacionamentoId}/mapa-calor?periodo=...
     * Mapa de calor Premium com dados agregados por regiao/coordenada aproximada.
     */
    @GetMapping("/{estacionamentoId}/mapa-calor")
    public ResponseEntity<MapaCalorOperacaoResponse> mapaCalor(
            @PathVariable Integer estacionamentoId,
            @RequestParam(name = "periodo", defaultValue = "mes") String periodo
    ) {
        return ResponseEntity.ok(operacaoService.mapaCalor(estacionamentoId, periodo));
    }

    /**
     * POST /api/operacao/{sessaoId}/finalizar?valorPago=0.00
     * Encerra a sessão e registra valorPago (opcional).
     */
    @PostMapping("/{sessaoId}/finalizar")
    public ResponseEntity<ApiResponse> finalizarSessao(
            @PathVariable Long sessaoId,
            @RequestParam(required = false) BigDecimal valorPago
    ) {
        return ResponseEntity.ok(operacaoService.encerrarSessao(sessaoId, valorPago));
    }

    /** POST /api/operacao/{sessaoId}/cancelar — cancela sessão ATIVA com senha do admin. */
    @PostMapping("/{sessaoId}/cancelar")
    public ResponseEntity<ApiResponse> cancelarSessao(
            @PathVariable Long sessaoId,
            @Valid @RequestBody CancelarSessaoRequest req
    ) {
        return ResponseEntity.ok(operacaoService.cancelarSessao(sessaoId, req));
    }

    /** DELETE /api/operacao/{sessaoId} — cancela reserva ATIVA. */
    @DeleteMapping("/{sessaoId}")
    public ResponseEntity<ApiResponse> cancelarReserva(@PathVariable Long sessaoId) {
        return ResponseEntity.ok(operacaoService.cancelarReserva(sessaoId));
    }
}
