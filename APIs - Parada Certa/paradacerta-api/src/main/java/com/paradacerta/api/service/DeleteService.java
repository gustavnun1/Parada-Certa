package com.paradacerta.api.service;

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

    /**
     * Deleta a conta do cliente e todos os dados relacionados
     */
    @Transactional
    public ApiResponse deletarConta(String cpf) {

        // 1. Verifica se o cliente existe
        Optional<Cliente> clienteOpt = clienteRepository.findById(cpf);

        if (clienteOpt.isEmpty()) {
            return ApiResponse.erro("Cliente não encontrado");
        }

        Cliente cliente = clienteOpt.get();
        String placa = cliente.getPlaca();

        // 2. Remove a FK do cliente para o veículo (seta como null)
        cliente.setPlaca(null);
        cliente.setVeiculo(null);
        clienteRepository.save(cliente);

        // 3. Deleta o Endereço
        Optional<Endereco> enderecoOpt = enderecoRepository.findByCpfCliente(cpf);
        enderecoOpt.ifPresent(enderecoRepository::delete);

        // 4. Deleta o Veículo
        if (placa != null && !placa.isEmpty()) {
            Optional<Veiculo> veiculoOpt = veiculoRepository.findById(placa);
            veiculoOpt.ifPresent(veiculoRepository::delete);
        }

        // 5. Deleta o Cliente por último
        clienteRepository.delete(cliente);

        return ApiResponse.ok("Conta excluída com sucesso!");
    }
}