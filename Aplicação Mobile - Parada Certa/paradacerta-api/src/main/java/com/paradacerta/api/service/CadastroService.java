package com.paradacerta.api.service;

import com.paradacerta.api.model.*;
import com.paradacerta.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.mindrot.jbcrypt.BCrypt;

@Service
@RequiredArgsConstructor
public class CadastroService {

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
    public ApiResponse cadastrar(CadastroRequest req) {

        // 1. Valida duplicidades
        if (clienteRepository.existsByCpf(req.getCpf())) {
            return ApiResponse.erro("CPF já cadastrado");
        }
        if (clienteRepository.existsByEmail(req.getEmail())) {
            return ApiResponse.erro("E-mail já cadastrado");
        }
        if (veiculoRepository.existsByPlaca(req.getPlaca().toUpperCase())) {
            return ApiResponse.erro("Placa já cadastrada");
        }

        // 2. Converte a data de nascimento
        LocalDate dataNascimento;
        try {
            dataNascimento = LocalDate.parse(req.getDataNascimento(), DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return ApiResponse.erro("Data de nascimento inválida. Use o formato DD/MM/AAAA");
        }

        String placaUpper = req.getPlaca().toUpperCase();

        // 3. Salva o Cliente
        Cliente cliente = new Cliente();
        cliente.setCpf(req.getCpf());
        cliente.setNome(req.getNome());
        cliente.setEmail(req.getEmail());
        cliente.setSenha(BCrypt.hashpw(req.getSenha(), BCrypt.gensalt()));
        cliente.setDataNascimento(dataNascimento);
        cliente.setNumeroCelular(req.getNumeroCelular() != null ? req.getNumeroCelular() : "");
        cliente.setPlaca(placaUpper);
        cliente.setVeiculo(req.getModeloVeiculo());
        clienteRepository.save(cliente);

        // 4. Salva o Veículo primeiro (Cliente tem FK para Veículo)
        Veiculo veiculo = new Veiculo();
        veiculo.setPlaca(placaUpper);
        veiculo.setNome(req.getModeloVeiculo());
        veiculo.setCor(req.getCorVeiculo());
        veiculo.setResponsavel(req.getCpf());
        veiculoRepository.save(veiculo);

        // 5. Salva o Endereço
        Endereco endereco = new Endereco();
        endereco.setCep(req.getCep());
        endereco.setLogradouro(req.getLogradouro());
        endereco.setNumero(req.getNumero());
        endereco.setComplemento(req.getComplemento() != null ? req.getComplemento() : "");
        endereco.setBairro(req.getBairro());
        endereco.setCidade(req.getCidade());
        endereco.setEstado(req.getEstado().toUpperCase());
        endereco.setCpfCliente(req.getCpf());
        enderecoRepository.save(endereco);

        return ApiResponse.ok("Cadastro realizado com sucesso!");
    }
}
