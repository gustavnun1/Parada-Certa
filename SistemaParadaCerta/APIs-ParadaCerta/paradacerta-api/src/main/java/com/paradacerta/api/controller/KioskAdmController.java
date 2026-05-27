package com.paradacerta.api.controller;

import com.paradacerta.api.dto.KioskAdmDTO;
import com.paradacerta.api.service.KioskAdmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/adm")
@RequiredArgsConstructor
public class KioskAdmController {

    private final KioskAdmService kioskAdmService;

    @PostMapping("/login")
    public ResponseEntity<KioskAdmDTO.LoginResponse> login(@RequestBody KioskAdmDTO.LoginRequest request) {
        return ResponseEntity.ok(kioskAdmService.login(request));
    }

    @PostMapping("/qrcode/gerar")
    public ResponseEntity<KioskAdmDTO.GerarQrCodeResponse> gerarQrCode(
            @RequestBody KioskAdmDTO.GerarQrCodeRequest request
    ) {
        return ResponseEntity.ok(kioskAdmService.gerarQrCode(request));
    }

    @GetMapping("/estacionamento/{id}/status")
    public ResponseEntity<KioskAdmDTO.StatusResponse> buscarStatus(@PathVariable Integer id) {
        return ResponseEntity.ok(kioskAdmService.buscarStatus(id));
    }
}
