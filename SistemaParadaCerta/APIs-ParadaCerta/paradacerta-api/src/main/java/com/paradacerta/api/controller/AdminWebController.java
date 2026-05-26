package com.paradacerta.api.controller;

import com.paradacerta.api.model.*;
import com.paradacerta.api.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Endpoints exclusivos do painel web administrativo.
 * Login por e-mail + senha (BCrypt). Sem token de sessão (TCC).
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminWebController {

    private final AdminService adminService;

    /** POST /api/admin/login — autentica admin/operador por email + senha */
    @PostMapping("/login")
    public ResponseEntity<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest req) {
        return ResponseEntity.ok(adminService.login(req));
    }

    /**
     * POST /api/admin/cadastro
     * Cria Estacionamento + linha de VagasEstacionamento + AdmEstacionamento (responsável) em uma transação.
     */
    @PostMapping(value = "/cadastro", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AdminCadastroResponse> cadastrar(@Valid @RequestBody AdminCadastroRequest req) {
        return ResponseEntity.ok(adminService.cadastrar(req));
    }

    /**
     * POST /api/admin/cadastro (multipart)
     * Mesmo cadastro inicial, mas com fotos no mesmo fluxo transacional. Se uma
     * foto falhar na validacao/upload, estacionamento/admin/operador nao ficam gravados.
     */
    @PostMapping(value = "/cadastro", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AdminCadastroResponse> cadastrarComFotos(
            @Valid @RequestPart("dados") AdminCadastroRequest req,
            @RequestPart(name = "fotos", required = false) List<MultipartFile> fotos
    ) {
        return ResponseEntity.ok(adminService.cadastrarComFotos(req, fotos));
    }

    /** GET /api/admin/{id} — detalhe de um administrador */
    @GetMapping("/{id}")
    public ResponseEntity<AdminResponse> buscar(@PathVariable Integer id) {
        return ResponseEntity.ok(adminService.buscarPorId(id));
    }

    /** GET /api/admin/estacionamento/{estacionamentoId} — todos os operadores do estacionamento */
    @GetMapping("/estacionamento/{estacionamentoId}")
    public ResponseEntity<List<AdminResponse>> listarPorEstacionamento(@PathVariable Integer estacionamentoId) {
        return ResponseEntity.ok(adminService.listarPorEstacionamento(estacionamentoId));
    }

    /** POST /api/admin/operador — cria operador adicional dentro do mesmo estacionamento */
    @PostMapping("/operador")
    public ResponseEntity<AdminResponse> cadastrarOperador(@Valid @RequestBody OperadorCadastroRequest req) {
        return ResponseEntity.ok(adminService.cadastrarOperador(req));
    }

    /** PUT /api/admin/{id} — atualiza dados do operador */
    @PutMapping("/{id}")
    public ResponseEntity<AdminResponse> atualizar(
            @PathVariable Integer id,
            @Valid @RequestBody AdminUpdateRequest req
    ) {
        return ResponseEntity.ok(adminService.atualizar(id, req));
    }

    /** DELETE /api/admin/{id} — soft delete (ativo=0) */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> desativar(@PathVariable Integer id) {
        adminService.desativar(id);
        return ResponseEntity.ok(ApiResponse.ok("Administrador desativado"));
    }
}
