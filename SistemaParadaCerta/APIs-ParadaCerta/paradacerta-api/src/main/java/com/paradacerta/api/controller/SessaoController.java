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

    @PostMapping("/entrada/kiosk")
    public ResponseEntity<EntradaResponse> vincularEntradaKiosk(@Valid @RequestBody EntradaKioskRequest request) {
        return ResponseEntity.ok(sessaoService.vincularEntradaKiosk(request));
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
     * Para sessões NÃO-reservadas o backend recalcula o valor (mínimo 1h,
     * arredondamento por blocos de 30 minutos) — o parâmetro valorPago é
     * mantido por compatibilidade, mas o valor gravado é sempre o calculado.
     * O trigger TR_Sessao_AtualizaVagas recalcula qtdVagasDisponiveis automaticamente.
     */
    @PostMapping("/encerrar/{sessaoId}")
    public ResponseEntity<ApiResponse> encerrarSessao(
            @PathVariable String sessaoId,
            @RequestParam(defaultValue = "0") double valorPago,
            @RequestParam(required = false) String cpf) {
        sessaoService.encerrarSessao(sessaoId, BigDecimal.valueOf(valorPago), cpf);
        return ResponseEntity.ok(ApiResponse.ok("Sessão encerrada com sucesso"));
    }

    /**
     * GET /api/sessao/{sessaoId}/calculo-cobranca
     * Calcula a cobrança da estadia (sessão não-reservada) aplicando as regras:
     *  - cobrança mínima de 1 hora;
     *  - tempo acima de 1 hora arredondado para o próximo bloco de 30 minutos.
     * Não modifica o estado da sessão.
     */
    @GetMapping("/{sessaoId}/calculo-cobranca")
    public ResponseEntity<CobrancaEstadiaResponse> calcularCobranca(@PathVariable String sessaoId) {
        return ResponseEntity.ok(sessaoService.calcularCobrancaEstadia(sessaoId));
    }
}
