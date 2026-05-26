package com.paradacerta.api.controller;

import com.paradacerta.api.model.ApiResponse;
import com.paradacerta.api.model.ConfirmarRecuperacaoRequest;
import com.paradacerta.api.model.LoginRequest;
import com.paradacerta.api.model.SolicitarRecuperacaoRequest;
import com.paradacerta.api.service.LoginService;
import com.paradacerta.api.service.RecuperacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;
    private final RecuperacaoService recuperacaoService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(loginService.login(request));
    }

    /**
     * POST /api/recuperar-senha/solicitar
     * Gera código de 6 dígitos e envia por e-mail.
     */
    @PostMapping("/recuperar-senha/solicitar")
    public ResponseEntity<ApiResponse> solicitarCodigo(@Valid @RequestBody SolicitarRecuperacaoRequest request) {
        return ResponseEntity.ok(recuperacaoService.solicitarCodigo(request));
    }

    /**
     * POST /api/recuperar-senha/confirmar
     * Valida o código recebido e atualiza a senha.
     */
    @PostMapping("/recuperar-senha/confirmar")
    public ResponseEntity<ApiResponse> confirmarCodigo(@Valid @RequestBody ConfirmarRecuperacaoRequest request) {
        return ResponseEntity.ok(recuperacaoService.confirmarCodigo(request));
    }
}
