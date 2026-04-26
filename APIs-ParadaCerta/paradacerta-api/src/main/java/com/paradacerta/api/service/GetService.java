package com.paradacerta.api.service;

import com.paradacerta.api.model.*;
import com.paradacerta.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.web.bind.annotation.PathVariable;

@Service
@RequiredArgsConstructor
public class GetService {

    private final ClienteRepository  clienteRepository;
    private final VeiculoRepository  veiculoRepository;
    private final EnderecoRepository enderecoRepository;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Realiza o cadastro completo: valida duplicidades,
     * depois salva Cliente → Veículo → Endereço em transação única.
     */
    @Transactional
    public ApiResponse select(String email) {

        Optional<Cliente> clienteOpt = clienteRepository.findByEmail(email);

        if (clienteOpt.isEmpty()) {
            return ApiResponse.erro("Cliente não encontrado");
        }

        Cliente cliente = clienteOpt.get();

        Veiculo veiculo = veiculoRepository.findByPlaca(cliente.getPlaca());

        Endereco endereco = enderecoRepository
                .findByClienteId(cliente.getId())
                .orElse(null);

        Map<String, Object> response = new HashMap<>();
        response.put("cliente", cliente);
        response.put("veiculo", veiculo);
        response.put("endereco", endereco);

        return ApiResponse.ok(response.toString());
    }
}
