package com.paradacerta.api.controller;

import com.paradacerta.api.dto.AdmDTO;
import com.paradacerta.api.service.AdmEstacionamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/adm")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdmEstacionamentoController {

    private final AdmEstacionamentoService admService;

    @PostMapping("/login")
    public ResponseEntity<AdmDTO.LoginResponse> login(@RequestBody AdmDTO.LoginRequest request) {
        AdmDTO.LoginResponse response = admService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/qrcode/gerar")
    public ResponseEntity<AdmDTO.GerarQrCodeResponse> gerarQrCode(
            @RequestBody AdmDTO.GerarQrCodeRequest request) {
        AdmDTO.GerarQrCodeResponse response = admService.gerarQrCode(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/estacionamento/{id}/status")
    public ResponseEntity<AdmDTO.StatusResponse> getStatus(@PathVariable("id") Integer id) {
        AdmDTO.StatusResponse response = admService.getStatus(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/conta")
    public ResponseEntity<AdmDTO.ExclusaoContaResponse> excluirPropriaConta(
            @RequestParam("adminId") Integer adminId) {
        return ResponseEntity.ok(admService.excluirPropriaConta(adminId));
    }
}
