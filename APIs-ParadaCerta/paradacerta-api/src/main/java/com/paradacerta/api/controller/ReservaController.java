package com.paradacerta.api.controller;

import com.paradacerta.api.model.ApiResponse;
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
     * Cria uma reserva de vaga para um usuário Premium.
     * Cobra o valor integral da vaga antecipadamente.
     */
    @PostMapping
    public ResponseEntity<ReservaResponse> criarReserva(@Valid @RequestBody ReservaRequest request) {
        return ResponseEntity.ok(reservaService.criarReserva(request));
    }

    /**
     * DELETE /api/reserva/{sessaoId}
     * Cancela uma reserva ativa e processa reembolso de 90%.
     */
    @DeleteMapping("/{sessaoId}")
    public ResponseEntity<ApiResponse> cancelarReserva(@PathVariable Long sessaoId) {
        return ResponseEntity.ok(reservaService.cancelarReserva(sessaoId));
    }

    /**
     * POST /api/reserva/{sessaoId}/finalizar
     * Finaliza a reserva quando o usuário confirma entrada no estacionamento.
     * Não cobra nada — pagamento já foi feito na reserva.
     */
    @PostMapping("/{sessaoId}/finalizar")
    public ResponseEntity<ApiResponse> finalizarReserva(@PathVariable Long sessaoId) {
        return ResponseEntity.ok(reservaService.finalizarReserva(sessaoId));
    }
}
