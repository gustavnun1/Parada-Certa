package com.paradacerta.api.controller;

import com.paradacerta.api.model.*;
import com.paradacerta.api.service.OperadorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD de operadores do kiosk (separados do administrador web).
 * Consumido pelo painel web (usuarios.html) — o login do operador acontece
 * na API do kiosk (porta 8081).
 */
@RestController
@RequestMapping("/api/operador")
@RequiredArgsConstructor
public class OperadorController {

    private final OperadorService operadorService;

    /** GET /api/operador/estacionamento/{estId} — lista operadores do estacionamento */
    @GetMapping("/estacionamento/{estId}")
    public ResponseEntity<List<OperadorResponse>> listar(@PathVariable Integer estId) {
        return ResponseEntity.ok(operadorService.listarPorEstacionamento(estId));
    }

    /**
     * GET /api/operador/{id} — detalhe completo do operador (tela de edição).
     * Inclui CPF e endereço completos (LGPD: rota individual, não usada em listagens).
     */
    @GetMapping("/{id}")
    public ResponseEntity<OperadorDetailResponse> buscar(@PathVariable Integer id) {
        return ResponseEntity.ok(operadorService.detalhe(id));
    }

    /** POST /api/operador — cria operador (BCrypt na senha) */
    @PostMapping
    public ResponseEntity<OperadorResponse> criar(@Valid @RequestBody OperadorRequest req) {
        return ResponseEntity.ok(operadorService.criar(req));
    }

    /** PUT /api/operador/{id} — atualiza nome/usuário/ativo */
    @PutMapping("/{id}")
    public ResponseEntity<OperadorResponse> atualizar(
            @PathVariable Integer id,
            @Valid @RequestBody OperadorUpdateRequest req
    ) {
        return ResponseEntity.ok(operadorService.atualizar(id, req));
    }

    /** PUT /api/operador/{id}/senha — troca a senha (BCrypt) */
    @PutMapping("/{id}/senha")
    public ResponseEntity<ApiResponse> trocarSenha(
            @PathVariable Integer id,
            @Valid @RequestBody OperadorSenhaRequest req
    ) {
        operadorService.trocarSenha(id, req);
        return ResponseEntity.ok(ApiResponse.ok("Senha atualizada"));
    }

    /** DELETE /api/operador/{id} — soft delete (ativo=0) */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> desativar(@PathVariable Integer id) {
        operadorService.desativar(id);
        return ResponseEntity.ok(ApiResponse.ok("Operador desativado"));
    }
}
