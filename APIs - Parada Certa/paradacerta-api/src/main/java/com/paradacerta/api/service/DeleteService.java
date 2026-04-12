package com.paradacerta.api.service;

import com.paradacerta.api.exception.UsuarioNaoEncontradoException;
import com.paradacerta.api.model.*;
import com.paradacerta.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeleteService {

    private final ClienteRepository  clienteRepository;
    private final VeiculoRepository  veiculoRepository;
    private final EnderecoRepository enderecoRepository;

    @Transactional
    public ApiResponse deletarConta(String cpf) {

        Cliente cliente = clienteRepository.findById(cpf)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Cliente não encontrado"));

        String placa = cliente.getPlaca();

        // Remove a FK do cliente para o veículo antes de deletar o veículo
        cliente.setPlaca(null);
        cliente.setVeiculo(null);
        clienteRepository.save(cliente);

        Optional<Endereco> enderecoOpt = enderecoRepository.findByCpfCliente(cpf);
        enderecoOpt.ifPresent(enderecoRepository::delete);

        if (placa != null && !placa.isEmpty()) {
            veiculoRepository.findById(placa).ifPresent(veiculoRepository::delete);
        }

        clienteRepository.delete(cliente);

        return ApiResponse.ok("Conta excluída com sucesso!");
    }
}
