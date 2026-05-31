package com.paradacerta.api.service;

import com.paradacerta.api.exception.ConflictException;
import com.paradacerta.api.exception.UsuarioNaoEncontradoException;
import com.paradacerta.api.model.ApiResponse;
import com.paradacerta.api.model.Cliente;
import com.paradacerta.api.model.Endereco;
import com.paradacerta.api.repository.AvaliacaoRepository;
import com.paradacerta.api.repository.ClienteRepository;
import com.paradacerta.api.repository.EnderecoRepository;
import com.paradacerta.api.repository.FormaPagamentoRepository;
import com.paradacerta.api.repository.SessaoRepository;
import com.paradacerta.api.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeleteService {

    private final ClienteRepository clienteRepository;
    private final VeiculoRepository veiculoRepository;
    private final EnderecoRepository enderecoRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final FormaPagamentoRepository formaPagamentoRepository;
    private final SessaoRepository sessaoRepository;

    @Transactional
    public ApiResponse deletarConta(String cpf) {
        Cliente cliente = clienteRepository.findByCpf(cpf)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Cliente nao encontrado"));

        Long clienteId = cliente.getId();

        if (sessaoRepository.existsSessaoVivaDoCliente(clienteId)) {
            throw new ConflictException("Encerre ou cancele sua sessao/reserva ativa antes de excluir a conta.");
        }

        avaliacaoRepository.deleteByClienteId(clienteId);
        sessaoRepository.desassociarCliente(clienteId);

        Optional<Endereco> enderecoOpt = enderecoRepository.findByClienteId(clienteId);
        enderecoOpt.ifPresent(enderecoRepository::delete);

        formaPagamentoRepository.deleteByClienteId(clienteId);
        veiculoRepository.deleteByClienteId(clienteId);
        clienteRepository.delete(cliente);

        return ApiResponse.ok("Conta excluida com sucesso!");
    }
}
