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

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SaverService {

    private final ClienteRepository  clienteRepository;
    private final VeiculoRepository  veiculoRepository;
    private final EnderecoRepository enderecoRepository;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Atualiza os dados de um cliente existente
     */
    @Transactional
    public ApiResponse salvar(CadastroRequest req) {

        // 1. Verifica se o cliente existe (pelo CPF)
        Optional<Cliente> clienteOpt = clienteRepository.findById(req.getCpf());

        if (clienteOpt.isEmpty()) {
            return ApiResponse.erro("Cliente não encontrado");
        }

        Cliente clienteExistente = clienteOpt.get();

        // 2. Verifica se está tentando mudar o e-mail para um que já existe
        if (!clienteExistente.getEmail().equals(req.getEmail()) &&
                clienteRepository.existsByEmail(req.getEmail())) {
            return ApiResponse.erro("E-mail já cadastrado por outro usuário");
        }

        // 3. Converte a data de nascimento
        LocalDate dataNascimento;
        try {
            dataNascimento = LocalDate.parse(req.getDataNascimento(), DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return ApiResponse.erro("Data de nascimento inválida. Use o formato DD/MM/AAAA");
        }

        String placaUpper = req.getPlaca().toUpperCase();

        // 4. Atualiza o Cliente
        clienteExistente.setNome(req.getNome());
        clienteExistente.setEmail(req.getEmail());

        // Só atualiza a senha se vier preenchida (diferente de vazio)
        if (req.getSenha() != null && !req.getSenha().isEmpty()) {
            clienteExistente.setSenha(BCrypt.hashpw(req.getSenha(), BCrypt.gensalt()));
        }

        clienteExistente.setDataNascimento(dataNascimento);
        clienteExistente.setNumeroCelular(req.getNumeroCelular() != null ? req.getNumeroCelular() : "");
        clienteExistente.setPlaca(placaUpper);
        clienteExistente.setVeiculo(placaUpper);
        clienteRepository.save(clienteExistente);

        // 5. Atualiza ou cria o Veículo
        Optional<Veiculo> veiculoOpt = veiculoRepository.findById(clienteExistente.getPlaca());

        Veiculo veiculo;
        if (veiculoOpt.isPresent()) {
            veiculo = veiculoOpt.get();
        } else {
            veiculo = new Veiculo();
            veiculo.setPlaca(placaUpper);
            veiculo.setResponsavel(req.getCpf());
        }

        veiculo.setNome(req.getModeloVeiculo());
        veiculo.setCor(req.getCorVeiculo());
        veiculoRepository.save(veiculo);

        // 6. Atualiza ou cria o Endereço
        Optional<Endereco> enderecoOpt = enderecoRepository.findByCpfCliente(req.getCpf());

        Endereco endereco;
        if (enderecoOpt.isPresent()) {
            endereco = enderecoOpt.get();
        } else {
            endereco = new Endereco();
            endereco.setCpfCliente(req.getCpf());
        }

        endereco.setCep(req.getCep());
        endereco.setLogradouro(req.getLogradouro());
        endereco.setNumero(req.getNumero());
        endereco.setComplemento(req.getComplemento() != null ? req.getComplemento() : "");
        endereco.setBairro(req.getBairro());
        endereco.setCidade(req.getCidade());
        endereco.setEstado(req.getEstado().toUpperCase());
        enderecoRepository.save(endereco);

        return ApiResponse.ok("Dados atualizados com sucesso!");
    }
}