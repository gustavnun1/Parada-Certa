package com.paradacerta.api.controller;

import com.paradacerta.api.exception.UsuarioNaoEncontradoException;
import com.paradacerta.api.model.ApiResponse;
import com.paradacerta.api.model.FormaPagamento;
import com.paradacerta.api.repository.FormaPagamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pagamento")
@RequiredArgsConstructor
public class PagamentoController {

    private final FormaPagamentoRepository formaPagamentoRepository;

    /**
     * GET /api/pagamento/{cpf}
     * Lista todas as formas de pagamento do cliente
     */
    @GetMapping("/{cpf}")
    public ResponseEntity<List<FormaPagamento>> listar(@PathVariable String cpf) {
        return ResponseEntity.ok(formaPagamentoRepository.findByClienteCPF(cpf));
    }

    /**
     * POST /api/pagamento
     * Salva uma nova forma de pagamento
     */
    @PostMapping
    public ResponseEntity<ApiResponse> salvar(@RequestBody FormaPagamento formaPagamento) {
        formaPagamentoRepository.save(formaPagamento);
        return ResponseEntity.ok(ApiResponse.ok("Forma de pagamento salva"));
    }

    /**
     * DELETE /api/pagamento/{id}
     * Remove uma forma de pagamento pelo ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deletar(@PathVariable Integer id) {
        if (!formaPagamentoRepository.existsById(id)) {
            throw new UsuarioNaoEncontradoException("Forma de pagamento não encontrada");
        }
        formaPagamentoRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.ok("Forma de pagamento removida"));
    }
}
