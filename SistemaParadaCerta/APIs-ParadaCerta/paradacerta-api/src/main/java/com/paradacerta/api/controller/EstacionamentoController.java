package com.paradacerta.api.controller;

import com.paradacerta.api.model.*;
import com.paradacerta.api.service.EstacionamentoService;
import com.paradacerta.api.service.PlanoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/estacionamentos")
@RequiredArgsConstructor
public class EstacionamentoController {

    private final EstacionamentoService estacionamentoService;
    private final PlanoService planoService;

    /**
     * GET /api/estacionamentos
     * - Sem parâmetro: lista todos os estacionamentos ativos (usado pelo app mobile).
     * - Com ?adminId={id}: lista apenas os estacionamentos vinculados ao administrador
     *   informado (usado pelo painel web para que cada admin veja só os seus).
     */
    @GetMapping
    public ResponseEntity<List<Estacionamento>> listarTodos(
            @RequestParam(name = "adminId", required = false) Integer adminId
    ) {
        if (adminId != null) {
            return ResponseEntity.ok(estacionamentoService.buscarPorAdmin(adminId));
        }
        return ResponseEntity.ok(estacionamentoService.buscarTodos());
    }

    /** GET /api/estacionamentos/proximos?lat=&lng=&raio= */
    @GetMapping("/proximos")
    public ResponseEntity<List<Estacionamento>> buscarProximos(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(defaultValue = "5.0") Double raio
    ) {
        return ResponseEntity.ok(estacionamentoService.buscarProximos(lat, lng, raio));
    }

    /** GET /api/estacionamentos/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<Estacionamento> buscarPorId(@PathVariable Integer id) {
        return estacionamentoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** GET /api/estacionamentos/{id}/qr-entrada */
    @GetMapping("/{id}/qr-entrada")
    public ResponseEntity<QrResponse> gerarQrEntrada(@PathVariable Integer id) {
        return ResponseEntity.ok(estacionamentoService.gerarQrEntrada(id));
    }

    /**
     * PUT /api/estacionamentos/{id}/entrada
     * Decrementa uma vaga disponível (chamado pelo app ao registrar entrada).
     * Mantido como fallback explícito — o trigger TR_Sessao_AtualizaVagas faz isso automaticamente.
     */
    @PutMapping("/{id}/entrada")
    public ResponseEntity<ApiResponse> registrarEntrada(@PathVariable Integer id) {
        estacionamentoService.decrementarVaga(id);
        return ResponseEntity.ok(ApiResponse.ok("Vaga decrementada"));
    }

    /**
     * PUT /api/estacionamentos/{id}/saida
     * Incrementa uma vaga disponível (chamado pelo app ao confirmar pagamento).
     * Mantido como fallback explícito — o trigger TR_Sessao_AtualizaVagas faz isso automaticamente.
     */
    @PutMapping("/{id}/saida")
    public ResponseEntity<ApiResponse> registrarSaida(@PathVariable Integer id) {
        estacionamentoService.incrementarVaga(id);
        return ResponseEntity.ok(ApiResponse.ok("Vaga incrementada"));
    }

    // ── CRUD admin web ───────────────────────────────────────────────────────

    /** POST /api/estacionamentos — cria estacionamento + linha de VagasEstacionamento */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Estacionamento> criar(@Valid @RequestBody EstacionamentoRequest req) {
        return ResponseEntity.ok(estacionamentoService.criar(req));
    }

    /** POST /api/estacionamentos multipart - cria estacionamento e fotos iniciais. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Estacionamento> criarComFotos(
            @Valid @RequestPart("dados") EstacionamentoRequest req,
            @RequestPart(name = "fotos", required = false) List<MultipartFile> fotos
    ) {
        return ResponseEntity.ok(estacionamentoService.criarComFotos(req, fotos));
    }

    /** PUT /api/estacionamentos/{id} — atualiza dados cadastrais */
    @PutMapping("/{id}")
    public ResponseEntity<Estacionamento> atualizar(
            @PathVariable Integer id,
            @Valid @RequestBody EstacionamentoUpdateRequest req
    ) {
        return ResponseEntity.ok(estacionamentoService.atualizar(id, req));
    }

    /** PUT /api/estacionamentos/{id}/vagas — atualiza qtdVagasTotais e qtdVagasReservaveis */
    @PutMapping("/{id}/vagas")
    public ResponseEntity<Estacionamento> atualizarVagas(
            @PathVariable Integer id,
            @Valid @RequestBody VagasUpdateRequest req
    ) {
        return ResponseEntity.ok(estacionamentoService.atualizarVagas(id, req));
    }

    /** DELETE /api/estacionamentos/{id} — soft delete (ativo=0) */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> desativar(@PathVariable Integer id) {
        estacionamentoService.desativar(id);
        return ResponseEntity.ok(ApiResponse.ok("Estacionamento desativado"));
    }

    // ── Plano de assinatura (Script 07-ALTER) ────────────────────────────────

    /** GET /api/estacionamentos/{id}/plano — retorna o plano vigente + flags de gating. */
    @GetMapping("/{id}/plano")
    public ResponseEntity<PlanoResponse> consultarPlano(@PathVariable Integer id) {
        return ResponseEntity.ok(planoService.buscarPlano(id));
    }

    /** PUT /api/estacionamentos/{id}/plano — assina/renova plano. */
    @PutMapping("/{id}/plano")
    public ResponseEntity<PlanoResponse> mudarPlano(
            @PathVariable Integer id,
            @Valid @RequestBody PlanoUpdateRequest req
    ) {
        return ResponseEntity.ok(planoService.mudarPlano(id, req));
    }

    /**
     * POST /api/estacionamentos/{id}/plano/pagamento
     * Recebe os dados do cartão, valida (Luhn + validade + CVV), registra o recibo
     * (somente últimos 4 dígitos + bandeira) e ativa o plano em uma única transação.
     */
    @PostMapping("/{id}/plano/pagamento")
    public ResponseEntity<PagamentoPlanoResponse> pagarPlano(
            @PathVariable Integer id,
            @Valid @RequestBody PagamentoPlanoRequest req
    ) {
        return ResponseEntity.ok(planoService.processarPagamentoEAtualizarPlano(id, req));
    }
}
