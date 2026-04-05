package com.paradacerta.api.controller;

import com.paradacerta.api.model.*;
import com.paradacerta.api.repository.ClienteRepository;
import com.paradacerta.api.repository.EnderecoRepository;
import com.paradacerta.api.repository.VeiculoRepository;
import com.paradacerta.api.service.CadastroService;
import com.paradacerta.api.service.DeleteService;
import com.paradacerta.api.service.GetService;
import com.paradacerta.api.service.SaverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ClientController {

    private final CadastroService    cadastroService;
    private final ClienteRepository  clienteRepository;
    private final VeiculoRepository  veiculoRepository;
    private final EnderecoRepository  enderecoRepository;
    private final SaverService saverService;
    private final DeleteService deleteService;

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
     * GET /api/usuario/{email}
     * Retorna os dados completos do usuário (Cliente + Veículo + Endereço)
     */
    @GetMapping("/usuario/{email}")
    public ResponseEntity<UserCompleteData> getUserByEmail(@PathVariable String email) {
        Optional<Cliente> clienteOpt = clienteRepository.findByEmail(email);

        if (clienteOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Cliente cliente = clienteOpt.get();

        // Busca o veículo
        Optional<Veiculo> veiculo = veiculoRepository.findById(cliente.getPlaca());

        // Busca o endereço
        Optional<Endereco> endereco = enderecoRepository.findByCpfCliente(cliente.getCpf());

        UserCompleteData userData = new UserCompleteData(
                cliente,
                veiculo.orElse(null),
                endereco.orElse(null)
        );

        return ResponseEntity.ok(userData);
    }

    /**
     * PUT /api/atualizar
     * Atualiza os dados de um cliente existente
     */
    @PutMapping("/salvar")
    public ResponseEntity<ApiResponse> atualizar(@Valid @RequestBody CadastroRequest request) {
        ApiResponse response = saverService.salvar(request);
        if (response.isSucesso()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * DELETE /api/conta/{cpf}
     * Deleta a conta do cliente
     */
    @DeleteMapping("/conta/{cpf}")
    public ResponseEntity<ApiResponse> deletarConta(@PathVariable String cpf) {
        ApiResponse response = deleteService.deletarConta(cpf);
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
