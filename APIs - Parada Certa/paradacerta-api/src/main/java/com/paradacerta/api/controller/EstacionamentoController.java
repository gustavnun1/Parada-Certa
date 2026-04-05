package com.paradacerta.api.controller;

import com.paradacerta.api.model.Estacionamento;
import com.paradacerta.api.service.EstacionamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estacionamentos")
@RequiredArgsConstructor
public class EstacionamentoController {

    private final EstacionamentoService estacionamentoService;

    /**
     * GET /api/estacionamentos
     * Retorna todos os estacionamentos ativos
     */
    @GetMapping
    public ResponseEntity<List<Estacionamento>> listarTodos() {
        return ResponseEntity.ok(estacionamentoService.buscarTodos());
    }

    /**
     * GET /api/estacionamentos/proximos?lat=-23.550520&lng=-46.633308&raio=5
     * Retorna estacionamentos próximos a uma coordenada
     */
    @GetMapping("/proximos")
    public ResponseEntity<List<Estacionamento>> buscarProximos(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(defaultValue = "5.0") Double raio
    ) {
        return ResponseEntity.ok(estacionamentoService.buscarProximos(lat, lng, raio));
    }

    /**
     * GET /api/estacionamentos/{id}
     * Retorna detalhes de um estacionamento específico
     */
    @GetMapping("/{id}")
    public ResponseEntity<Estacionamento> buscarPorId(@PathVariable Integer id) {
        return estacionamentoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}