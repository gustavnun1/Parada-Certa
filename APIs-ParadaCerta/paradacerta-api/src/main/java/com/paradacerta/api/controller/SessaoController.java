package com.paradacerta.api.controller;

import com.paradacerta.api.model.*;
import com.paradacerta.api.service.SessaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/sessao")
@RequiredArgsConstructor
public class SessaoController {

    private final SessaoService sessaoService;

    /**
     * POST /api/sessao/entrada
     * Registra entrada via QR Code chamando sp_RegistrarEntrada
     */
    @PostMapping("/entrada")
    public ResponseEntity<EntradaResponse> registrarEntrada(@Valid @RequestBody EntradaRequest request) {
        return ResponseEntity.ok(sessaoService.registrarEntrada(request));
    }

    /**
     * POST /api/sessao/entrada/app
     * Registra entrada iniciada pelo app (demo ou QR scan),
     * criando a sessão no banco sem depender do QR físico.
     */
    @PostMapping("/entrada/app")
    public ResponseEntity<EntradaResponse> registrarEntradaApp(@Valid @RequestBody EntradaAppRequest request) {
        return ResponseEntity.ok(sessaoService.registrarEntradaApp(request));
    }

    /**
     * POST /api/sessao/pagamento
     * Registra pagamento via QR Code chamando sp_RegistrarPagamento
     */
    @PostMapping("/pagamento")
    public ResponseEntity<PagamentoResponse> registrarPagamento(@Valid @RequestBody PagamentoRequest request) {
        return ResponseEntity.ok(sessaoService.registrarPagamento(request));
    }

    /**
     * GET /api/sessao/ativa/{cpf}
     * Retorna a sessão ativa do usuário.
     * 200 + body quando encontrada; 204 sem body quando não há sessão ativa.
     */
    @GetMapping("/ativa/{cpf}")
    public ResponseEntity<SessaoAtivaResponse> buscarSessaoAtiva(@PathVariable String cpf) {
        return sessaoService.buscarSessaoAtiva(cpf)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * POST /api/sessao/encerrar/{sessaoId}?valorPago=25.50
     * Encerra uma sessão ativa registrando o valor pago.
     * Chamado pelo app quando o usuário confirma o pagamento manualmente.
     * O trigger TR_Sessao_AtualizaVagas recalcula qtdVagasDisponiveis automaticamente.
     */
    @PostMapping("/encerrar/{sessaoId}")
    public ResponseEntity<ApiResponse> encerrarSessao(
            @PathVariable String sessaoId,
            @RequestParam(defaultValue = "0") double valorPago) {
        sessaoService.encerrarSessao(sessaoId, BigDecimal.valueOf(valorPago));
        return ResponseEntity.ok(ApiResponse.ok("Sessão encerrada com sucesso"));
    }
}
