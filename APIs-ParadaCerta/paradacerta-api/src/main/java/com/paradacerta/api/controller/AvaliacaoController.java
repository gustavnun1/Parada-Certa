package com.paradacerta.api.controller;

import com.paradacerta.api.model.ApiResponse;
import com.paradacerta.api.model.AvaliacaoRequest;
import com.paradacerta.api.model.AvaliacaoResponse;
import com.paradacerta.api.service.AvaliacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/avaliacao")
@RequiredArgsConstructor
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    /** POST /api/avaliacao — registra avaliação do motorista após encerrar sessão */
    @PostMapping
    public ResponseEntity<ApiResponse> avaliar(@Valid @RequestBody AvaliacaoRequest request) {
        return ResponseEntity.ok(avaliacaoService.avaliar(request));
    }

    /** GET /api/avaliacao/estacionamento/{id} — lista avaliações de um estacionamento */
    @GetMapping("/estacionamento/{id}")
    public ResponseEntity<List<AvaliacaoResponse>> listar(@PathVariable Integer id) {
        return ResponseEntity.ok(avaliacaoService.listar(id));
    }
}
