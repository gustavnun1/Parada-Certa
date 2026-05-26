package com.paradacerta.api.controller;

import com.paradacerta.api.exception.RequisicaoInvalidaException;
import com.paradacerta.api.exception.UsuarioNaoEncontradoException;
import com.paradacerta.api.model.ApiResponse;
import com.paradacerta.api.model.Cliente;
import com.paradacerta.api.model.FormaPagamento;
import com.paradacerta.api.model.FormaPagamentoInput;
import com.paradacerta.api.repository.ClienteRepository;
import com.paradacerta.api.repository.FormaPagamentoRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/pagamento")
@RequiredArgsConstructor
public class PagamentoController {

    private final FormaPagamentoRepository formaPagamentoRepository;
    private final ClienteRepository        clienteRepository;

    /**
     * GET /api/pagamento/{cpf}
     * Lista todas as formas de pagamento do cliente.
     */
    @GetMapping("/{cpf}")
    public ResponseEntity<List<FormaPagamento>> listar(@PathVariable String cpf) {
        Cliente cliente = clienteRepository.findByCpf(cpf)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Cliente não encontrado"));
        return ResponseEntity.ok(formaPagamentoRepository.findByClienteId(cliente.getId()));
    }

    /**
     * POST /api/pagamento
     * Salva uma nova forma de pagamento.
     *
     * Segurança:
     * - Valida o número do cartão com algoritmo de Luhn
     * - Verifica se a validade não está expirada
     * - Armazena APENAS os últimos 4 dígitos — nunca o número completo
     * - CVV não é recebido nem armazenado
     */
    @PostMapping
    public ResponseEntity<ApiResponse> salvar(@Valid @RequestBody FormaPagamentoInput input) {
        Cliente cliente = clienteRepository.findByCpf(input.getClienteCpf())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Cliente não encontrado"));

        boolean isCartao = "CARTAO_CREDITO".equals(input.getTipoPagamento())
                        || "DEBITO".equals(input.getTipoPagamento());

        if (isCartao) {
            if (input.getNumeroCartao() == null || input.getNumeroCartao().isBlank()) {
                throw new RequisicaoInvalidaException("Número do cartão é obrigatório");
            }
            if (!luhnValido(input.getNumeroCartao())) {
                throw new RequisicaoInvalidaException("Número do cartão inválido");
            }
            if (input.getValidade() == null || !validadeNaoExpirada(input.getValidade())) {
                throw new RequisicaoInvalidaException("Cartão expirado ou validade inválida");
            }
            if (input.getNomeCartao() == null || input.getNomeCartao().isBlank()) {
                throw new RequisicaoInvalidaException("Nome no cartão é obrigatório");
            }
        }

        FormaPagamento fp = new FormaPagamento();
        fp.setClienteId(cliente.getId());
        fp.setTipoPagamento(input.getTipoPagamento());

        // Armazena APENAS os últimos 4 dígitos do cartão
        if (input.getNumeroCartao() != null) {
            String digits = input.getNumeroCartao().replaceAll("\\D", "");
            fp.setNumeroCartao(digits.substring(Math.max(0, digits.length() - 4)));
        }

        fp.setNomeCartao(input.getNomeCartao() != null ? input.getNomeCartao().trim() : null);
        fp.setValidade(input.getValidade());
        fp.setBandeira(input.getBandeira());

        formaPagamentoRepository.save(fp);
        return ResponseEntity.ok(ApiResponse.ok("Forma de pagamento salva"));
    }

    /**
     * DELETE /api/pagamento/{id}?cpf=...
     * Remove uma forma de pagamento pelo ID.
     * Verifica que o cartão pertence ao CPF informado antes de excluir.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deletar(
            @PathVariable Integer id,
            @RequestParam String cpf
    ) {
        FormaPagamento fp = formaPagamentoRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Forma de pagamento não encontrada"));

        Cliente cliente = clienteRepository.findByCpf(cpf)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Cliente não encontrado"));

        // Garante que o cartão pertence ao cliente que está pedindo a exclusão
        if (!fp.getClienteId().equals(cliente.getId())) {
            throw new RequisicaoInvalidaException("Operação não permitida");
        }

        formaPagamentoRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.ok("Forma de pagamento removida"));
    }

    // ── Helpers de validação ──────────────────────────────────────────────────

    /**
     * Algoritmo de Luhn — verifica se um número de cartão é matematicamente válido.
     */
    private static boolean luhnValido(String numero) {
        String digits = numero.replaceAll("\\D", "");
        if (digits.length() < 13 || digits.length() > 19) return false;
        int soma = 0;
        boolean dobrar = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = Character.getNumericValue(digits.charAt(i));
            if (dobrar) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            soma += d;
            dobrar = !dobrar;
        }
        return soma % 10 == 0;
    }

    /**
     * Verifica se a validade no formato MM/AA não está no passado.
     */
    private static boolean validadeNaoExpirada(String validade) {
        try {
            if (validade == null || !validade.matches("\\d{2}/\\d{2}")) return false;
            String[] parts = validade.split("/");
            int mes = Integer.parseInt(parts[0]);
            int ano = Integer.parseInt(parts[1]);
            if (mes < 1 || mes > 12) return false;
            if (ano > 99) return false; // sanity: rejeita ano de 4 dígitos enviado por engano
            YearMonth cartao = YearMonth.of(2000 + ano, mes);
            return !cartao.isBefore(YearMonth.now());
        } catch (Exception e) {
            return false;
        }
    }
}
