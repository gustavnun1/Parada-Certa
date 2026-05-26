package com.paradacerta.api.controller;

import com.paradacerta.api.model.ApiResponse;
import com.paradacerta.api.model.CalculoExtraResponse;
import com.paradacerta.api.model.ReservaRequest;
import com.paradacerta.api.model.ReservaResponse;
import com.paradacerta.api.service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reserva")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    /**
     * POST /api/reserva
     * Cria uma reserva de vaga.
     * Cobra o valor integral da vaga antecipadamente.
     */
    @PostMapping
    public ResponseEntity<ReservaResponse> criarReserva(@Valid @RequestBody ReservaRequest request) {
        return ResponseEntity.ok(reservaService.criarReserva(request));
    }

    /**
     * DELETE /api/reserva/{sessaoId}
     * Cancela uma reserva ativa e processa reembolso de 15% (85% retidos pelo estacionamento).
     */
    @DeleteMapping("/{sessaoId}")
    public ResponseEntity<ApiResponse> cancelarReserva(@PathVariable Long sessaoId) {
        return ResponseEntity.ok(reservaService.cancelarReserva(sessaoId));
    }

    /**
     * POST /api/reserva/{sessaoId}/finalizar
     * Finaliza a reserva quando o usuário confirma entrada no estacionamento.
     * Cobra apenas se não houver tempo excedente (≤ 15 min além da 1ª hora).
     * Se houver tempo extra > 15 min, retorna 400 com o valor a pagar via
     * POST /api/sessao/encerrar/{sessaoId}?valorPago=X.XX
     */
    @PostMapping("/{sessaoId}/finalizar")
    public ResponseEntity<ApiResponse> finalizarReserva(@PathVariable Long sessaoId) {
        return ResponseEntity.ok(reservaService.finalizarReserva(sessaoId));
    }

    /**
     * GET /api/reserva/{sessaoId}/calculo-extra
     * Consulta o tempo excedente e o valor extra de uma reserva ativa.
     * Não altera o estado da sessão.
     * Retorna: { temCobrancaExtra, minutosExtra, valorExtra }
     */
    @GetMapping("/{sessaoId}/calculo-extra")
    public ResponseEntity<CalculoExtraResponse> calcularExtra(@PathVariable Long sessaoId) {
        return ResponseEntity.ok(reservaService.calcularExtra(sessaoId));
    }
}
