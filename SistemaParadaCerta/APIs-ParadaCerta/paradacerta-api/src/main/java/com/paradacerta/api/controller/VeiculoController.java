package com.paradacerta.api.controller;

import com.paradacerta.api.model.ApiResponse;
import com.paradacerta.api.model.Veiculo;
import com.paradacerta.api.model.VeiculoInput;
import com.paradacerta.api.model.VeiculoUpdateRequest;
import com.paradacerta.api.service.VeiculoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/veiculo")
@RequiredArgsConstructor
public class VeiculoController {

    private final VeiculoService veiculoService;

    /** GET /api/veiculo/cliente/{cpf} — lista todos os veículos do cliente */
    @GetMapping("/cliente/{cpf}")
    public ResponseEntity<List<Veiculo>> listar(@PathVariable String cpf) {
        return ResponseEntity.ok(veiculoService.listar(cpf));
    }

    /** POST /api/veiculo — adiciona veículo ao cliente (máx. 5) */
    @PostMapping
    public ResponseEntity<ApiResponse> adicionar(@Valid @RequestBody VeiculoInput input) {
        return ResponseEntity.ok(veiculoService.adicionar(input));
    }

    /** PUT /api/veiculo/{placa} — atualiza modelo e cor de um veículo */
    @PutMapping("/{placa}")
    public ResponseEntity<ApiResponse> atualizar(
            @PathVariable String placa,
            @Valid @RequestBody VeiculoUpdateRequest request
    ) {
        return ResponseEntity.ok(veiculoService.atualizar(placa, request));
    }

    /** DELETE /api/veiculo/{placa}?cpf={cpf} — remove veículo do cliente */
    @DeleteMapping("/{placa}")
    public ResponseEntity<ApiResponse> remover(
            @PathVariable String placa,
            @RequestParam String cpf
    ) {
        return ResponseEntity.ok(veiculoService.remover(placa, cpf));
    }
}
