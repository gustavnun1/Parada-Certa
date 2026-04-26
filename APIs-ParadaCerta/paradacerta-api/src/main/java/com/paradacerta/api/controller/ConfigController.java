package com.paradacerta.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints de configuração pública do app.
 * Expõe dados que o Android precisa mas que não devem estar hardcoded no APK.
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @Value("${empresa.pix-key}")
    private String pixKey;

    /**
     * GET /api/config/pix-key
     * Retorna a chave PIX da empresa para pagamentos de assinatura Premium.
     * A chave é lida de uma variável de ambiente (EMPRESA_PIX_KEY) no servidor.
     */
    @GetMapping("/pix-key")
    public ResponseEntity<Map<String, String>> obterPixKey() {
        return ResponseEntity.ok(Map.of("pixKey", pixKey));
    }
}
