package com.paradacerta.api.controller;

import com.paradacerta.api.model.ApiResponse;
import com.paradacerta.api.model.CalculoExtraResponse;
import com.paradacerta.api.model.ConfirmacaoReservaResponse;
import com.paradacerta.api.model.ConfirmarReservaRequest;
import com.paradacerta.api.model.FinalizacaoUsoResponse;
import com.paradacerta.api.model.FinalizarUsoRequest;
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
     * Cria uma reserva de vaga em status AGUARDANDO_CONFIRMACAO.
     * Cobra o valor da 1ª hora antecipadamente. O motorista precisa escanear
     * o QR Code de confirmação no estacionamento para ativar a reserva.
     */
    @PostMapping
    public ResponseEntity<ReservaResponse> criarReserva(@Valid @RequestBody ReservaRequest request) {
        return ResponseEntity.ok(reservaService.criarReserva(request));
    }

    /**
     * DELETE /api/reserva/{sessaoId}
     * Cancela uma reserva AGUARDANDO_CONFIRMACAO e processa reembolso de 15%.
     * Após confirmar (EM_USO), o cancelamento não é mais permitido pelo motorista.
     */
    @DeleteMapping("/{sessaoId}")
    public ResponseEntity<ApiResponse> cancelarReserva(@PathVariable Long sessaoId) {
        return ResponseEntity.ok(reservaService.cancelarReserva(sessaoId));
    }

    /**
     * POST /api/reserva/confirmar
     * Motorista escaneia o QR Code de confirmação fornecido pelo estacionamento.
     * Marca dataHoraConfirmacao e muda status para EM_USO. A partir daqui o
     * cronômetro do uso começa a contar e o cancelamento é bloqueado.
     */
    @PostMapping("/confirmar")
    public ResponseEntity<ConfirmacaoReservaResponse> confirmarReserva(
            @Valid @RequestBody ConfirmarReservaRequest request) {
        return ResponseEntity.ok(
                reservaService.confirmarReservaPorQrCode(request.getQrCode(), request.getCpf())
        );
    }

    /**
     * GET /api/reserva/{sessaoId}/finalizacao
     * Calcula a finalização do uso sem efetivar (preview): tempo de uso,
     * valor final, valor restante a cobrar. Usado pelo mobile para mostrar
     * o resumo antes de confirmar.
     */
    @GetMapping("/{sessaoId}/finalizacao")
    public ResponseEntity<FinalizacaoUsoResponse> calcularFinalizacao(
            @PathVariable Long sessaoId,
            @RequestParam String cpf) {
        return ResponseEntity.ok(reservaService.calcularFinalizacaoUso(sessaoId, cpf));
    }

    /**
     * POST /api/reserva/{sessaoId}/finalizar-uso
     * Encerra efetivamente a reserva EM_USO. Recalcula o valor com base no
     * tempo desde dataHoraConfirmacao. Se houver excedente além do valor
     * antecipado, exige valorPagoAdicional no body.
     */
    @PostMapping("/{sessaoId}/finalizar-uso")
    public ResponseEntity<FinalizacaoUsoResponse> finalizarUso(
            @PathVariable Long sessaoId,
            @Valid @RequestBody FinalizarUsoRequest request) {
        return ResponseEntity.ok(
                reservaService.finalizarUso(sessaoId, request.getCpf(), request.getValorPagoAdicional())
        );
    }

    /**
     * POST /api/reserva/{sessaoId}/finalizar
     * Compat: chamado por versões antigas do app. Encerra a reserva sem
     * cobrança adicional. Builds novos devem usar /finalizar-uso.
     */
    @PostMapping("/{sessaoId}/finalizar")
    public ResponseEntity<ApiResponse> finalizarReserva(@PathVariable Long sessaoId) {
        return ResponseEntity.ok(reservaService.finalizarReserva(sessaoId));
    }

    /**
     * GET /api/reserva/{sessaoId}/calculo-extra
     * Mantido por compat: consulta o tempo excedente e o valor extra de uma
     * reserva em curso. Para o fluxo novo, prefira /finalizacao.
     */
    @GetMapping("/{sessaoId}/calculo-extra")
    public ResponseEntity<CalculoExtraResponse> calcularExtra(@PathVariable Long sessaoId) {
        return ResponseEntity.ok(reservaService.calcularExtra(sessaoId));
    }
}
