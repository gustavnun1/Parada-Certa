package com.paradacerta.api.controller;

import com.paradacerta.api.model.ApiResponse;
import com.paradacerta.api.model.EstacionamentoFotoResponse;
import com.paradacerta.api.service.FotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Upload, listagem e remoção de fotos do estacionamento.
 *
 * Endpoints:
 *  - {@code POST /api/estacionamentos/{estId}/fotos}      (multipart: campo file)
 *  - {@code GET  /api/estacionamentos/{estId}/fotos}
 *  - {@code DELETE /api/estacionamentos/{estId}/fotos/{fotoId}}
 */
@RestController
@RequestMapping("/api/estacionamentos/{estId}/fotos")
@RequiredArgsConstructor
public class EstacionamentoFotoController {

    private final FotoService fotoService;

    @GetMapping
    public ResponseEntity<List<EstacionamentoFotoResponse>> listar(@PathVariable Integer estId) {
        return ResponseEntity.ok(fotoService.listar(estId));
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<EstacionamentoFotoResponse> upload(
            @PathVariable Integer estId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "principal", required = false, defaultValue = "false") boolean principal
    ) {
        return ResponseEntity.ok(fotoService.upload(estId, file, principal));
    }

    @PutMapping("/{fotoId}/principal")
    public ResponseEntity<EstacionamentoFotoResponse> marcarPrincipal(
            @PathVariable Integer estId,
            @PathVariable Integer fotoId
    ) {
        return ResponseEntity.ok(fotoService.marcarPrincipal(estId, fotoId));
    }

    @DeleteMapping("/{fotoId}")
    public ResponseEntity<ApiResponse> remover(@PathVariable Integer estId, @PathVariable Integer fotoId) {
        fotoService.remover(estId, fotoId);
        return ResponseEntity.ok(ApiResponse.ok("Foto removida"));
    }
}
