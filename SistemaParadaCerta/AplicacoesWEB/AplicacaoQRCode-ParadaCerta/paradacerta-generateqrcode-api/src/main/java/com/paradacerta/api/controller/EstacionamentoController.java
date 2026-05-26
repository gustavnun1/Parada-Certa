package com.paradacerta.api.controller;

import com.paradacerta.api.dto.AdmDTO;
import com.paradacerta.api.service.AdmEstacionamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estacionamentos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EstacionamentoController {

    private final AdmEstacionamentoService admService;

    @GetMapping
    public ResponseEntity<List<AdmDTO.EstacionamentoResponse>> listar(
            @RequestParam("adminId") Integer adminId) {
        return ResponseEntity.ok(admService.listarEstacionamentosDoAdmin(adminId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdmDTO.EstacionamentoResponse> buscar(
            @PathVariable("id") Integer id,
            @RequestParam(value = "adminId", required = false) Integer adminId) {
        return ResponseEntity.ok(admService.buscarEstacionamento(id, adminId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdmDTO.EstacionamentoResponse> atualizar(
            @PathVariable("id") Integer id,
            @RequestBody AdmDTO.AtualizarEstacionamentoRequest request) {
        return ResponseEntity.ok(admService.atualizarEstacionamento(id, request));
    }

    @PutMapping("/{id}/vagas")
    public ResponseEntity<Void> atualizarVagas(
            @PathVariable("id") Integer id,
            @RequestBody AdmDTO.AtualizarVagasRequest request) {
        admService.atualizarVagas(id, request);
        return ResponseEntity.noContent().build();
    }
}
