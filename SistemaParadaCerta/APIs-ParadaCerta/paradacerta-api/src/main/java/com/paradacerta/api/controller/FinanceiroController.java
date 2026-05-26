package com.paradacerta.api.controller;

import com.paradacerta.api.model.FinanceiroPagamentoResponse;
import com.paradacerta.api.model.FinanceiroResumoResponse;
import com.paradacerta.api.service.FinanceiroService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Visão financeira do administrador web (aba Financeiro). */
@RestController
@RequestMapping("/api/financeiro")
@RequiredArgsConstructor
public class FinanceiroController {

    private final FinanceiroService financeiroService;

    /**
     * GET /api/financeiro/{estacionamentoId}/pagamentos?periodo=hoje|semana|mes|ano|todos
     * Lista pagamentos confirmados (sessões ENCERRADAS com valorPago > 0).
     */
    @GetMapping("/{estacionamentoId}/pagamentos")
    public ResponseEntity<List<FinanceiroPagamentoResponse>> listarPagamentos(
            @PathVariable Integer estacionamentoId,
            @RequestParam(defaultValue = "todos") String periodo
    ) {
        return ResponseEntity.ok(financeiroService.listarPagamentos(estacionamentoId, periodo));
    }

    /** GET /api/financeiro/{estacionamentoId}/resumo?periodo=... — cards do financeiro. */
    @GetMapping("/{estacionamentoId}/resumo")
    public ResponseEntity<FinanceiroResumoResponse> resumo(
            @PathVariable Integer estacionamentoId,
            @RequestParam(defaultValue = "todos") String periodo
    ) {
        return ResponseEntity.ok(financeiroService.resumo(estacionamentoId, periodo));
    }
}
