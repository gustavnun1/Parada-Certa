package com.paradacerta.api.controller;

import com.paradacerta.api.model.ApiResponse;
import com.paradacerta.api.model.CadastroRequest;
import com.paradacerta.api.repository.ClienteRepository;
import com.paradacerta.api.repository.VeiculoRepository;
import com.paradacerta.api.service.CadastroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CadastroController {

    private final CadastroService    cadastroService;
    private final ClienteRepository  clienteRepository;
    private final VeiculoRepository  veiculoRepository;

    /**
     * POST /api/cadastro
     * Recebe todos os dados e salva no banco em transação única
     */
    @PostMapping("/cadastro")
    public ResponseEntity<ApiResponse> cadastrar(@Valid @RequestBody CadastroRequest request) {
        ApiResponse response = cadastroService.cadastrar(request);
        if (response.isSucesso()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * GET /api/verificar/cpf/{cpf}
     * Verifica se o CPF já está cadastrado (usado pelo app antes de finalizar)
     */
    @GetMapping("/verificar/cpf/{cpf}")
    public ResponseEntity<ApiResponse> verificarCpf(@PathVariable String cpf) {
        boolean existe = clienteRepository.existsByCpf(cpf);
        if (existe) {
            return ResponseEntity.badRequest().body(ApiResponse.erro("CPF já cadastrado"));
        }
        return ResponseEntity.ok(ApiResponse.ok("CPF disponível"));
    }

    /**
     * GET /api/verificar/placa/{placa}
     * Verifica se a placa já está cadastrada
     */
    @GetMapping("/verificar/placa/{placa}")
    public ResponseEntity<ApiResponse> verificarPlaca(@PathVariable String placa) {
        boolean existe = veiculoRepository.existsByPlaca(placa.toUpperCase());
        if (existe) {
            return ResponseEntity.badRequest().body(ApiResponse.erro("Placa já cadastrada"));
        }
        return ResponseEntity.ok(ApiResponse.ok("Placa disponível"));
    }

    /**
     * GET /api/health
     * Verifica se a API está no ar (útil para teste inicial)
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse> health() {
        return ResponseEntity.ok(ApiResponse.ok("API Parada Certa está no ar!"));
    }
}
