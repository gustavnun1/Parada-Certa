package com.paradacerta.api.controller;

import com.paradacerta.api.exception.UsuarioNaoEncontradoException;
import com.paradacerta.api.model.*;
import com.paradacerta.api.repository.ClienteRepository;
import com.paradacerta.api.repository.EnderecoRepository;
import com.paradacerta.api.repository.VeiculoRepository;
import com.paradacerta.api.service.CadastroService;
import com.paradacerta.api.service.DeleteService;
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
    private final EnderecoRepository enderecoRepository;
    private final SaverService       saverService;
    private final DeleteService      deleteService;

    /** POST /api/cadastro — cria Cliente + Veículo + Endereço */
    @PostMapping("/cadastro")
    public ResponseEntity<ApiResponse> cadastrar(@Valid @RequestBody CadastroRequest request) {
        return ResponseEntity.ok(cadastroService.cadastrar(request));
    }

    /** GET /api/usuario/{email} — retorna UserCompleteData por e-mail */
    @GetMapping("/usuario/{email}")
    public ResponseEntity<UserCompleteData> getUserByEmail(@PathVariable String email) {
        Cliente cliente = clienteRepository.findByEmail(email)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado"));
        return ResponseEntity.ok(buildUserData(cliente));
    }

    /** GET /api/usuario/cpf/{cpf} — retorna UserCompleteData por CPF */
    @GetMapping("/usuario/cpf/{cpf}")
    public ResponseEntity<UserCompleteData> getUserByCpf(@PathVariable String cpf) {
        Cliente cliente = clienteRepository.findByCpf(cpf)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado"));
        return ResponseEntity.ok(buildUserData(cliente));
    }

    /** PUT /api/salvar — atualiza dados do cliente */
    @PutMapping("/salvar")
    public ResponseEntity<ApiResponse> salvar(@Valid @RequestBody CadastroRequest request) {
        return ResponseEntity.ok(saverService.salvar(request));
    }

    /** DELETE /api/conta/{cpf} — exclui conta do cliente */
    @DeleteMapping("/conta/{cpf}")
    public ResponseEntity<ApiResponse> deletarConta(@PathVariable String cpf) {
        return ResponseEntity.ok(deleteService.deletarConta(cpf));
    }

    /** PUT /api/premium/{cpf} — ativa plano premium */
    @PutMapping("/premium/{cpf}")
    public ResponseEntity<ApiResponse> atualizarPremium(@PathVariable String cpf) {
        Cliente cliente = clienteRepository.findByCpf(cpf)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Cliente não encontrado"));
        cliente.setPremium(true);
        clienteRepository.save(cliente);
        return ResponseEntity.ok(ApiResponse.ok("Plano Premium ativado com sucesso"));
    }

    /** PUT /api/premium/cancelar/{cpf} — cancela plano premium */
    @PutMapping("/premium/cancelar/{cpf}")
    public ResponseEntity<ApiResponse> cancelarPremium(@PathVariable String cpf) {
        Cliente cliente = clienteRepository.findByCpf(cpf)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Cliente não encontrado"));
        cliente.setPremium(false);
        clienteRepository.save(cliente);
        return ResponseEntity.ok(ApiResponse.ok("Plano Premium cancelado com sucesso"));
    }

    /** GET /api/verificar/cpf/{cpf} */
    @GetMapping("/verificar/cpf/{cpf}")
    public ResponseEntity<ApiResponse> verificarCpf(@PathVariable String cpf) {
        if (clienteRepository.existsByCpf(cpf)) {
            return ResponseEntity.badRequest().body(ApiResponse.erro("CPF já cadastrado"));
        }
        return ResponseEntity.ok(ApiResponse.ok("CPF disponível"));
    }

    /** GET /api/verificar/placa/{placa} */
    @GetMapping("/verificar/placa/{placa}")
    public ResponseEntity<ApiResponse> verificarPlaca(@PathVariable String placa) {
        if (veiculoRepository.existsByPlaca(placa.toUpperCase())) {
            return ResponseEntity.badRequest().body(ApiResponse.erro("Placa já cadastrada"));
        }
        return ResponseEntity.ok(ApiResponse.ok("Placa disponível"));
    }

    /** GET /api/health */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse> health() {
        return ResponseEntity.ok(ApiResponse.ok("API Parada Certa está no ar!"));
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private UserCompleteData buildUserData(Cliente cliente) {
        Optional<Veiculo> veiculo = Optional.ofNullable(cliente.getPlaca())
                .flatMap(veiculoRepository::findById);
        Optional<Endereco> endereco = enderecoRepository.findByClienteId(cliente.getId());
        return new UserCompleteData(cliente, veiculo.orElse(null), endereco.orElse(null));
    }
}
