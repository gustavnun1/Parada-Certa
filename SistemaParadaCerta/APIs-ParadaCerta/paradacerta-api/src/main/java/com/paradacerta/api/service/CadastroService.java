package com.paradacerta.api.service;

import com.paradacerta.api.exception.ConflictException;
import com.paradacerta.api.exception.RequisicaoInvalidaException;
import com.paradacerta.api.model.*;
import com.paradacerta.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
@RequiredArgsConstructor
public class CadastroService {

    private final ClienteRepository  clienteRepository;
    private final VeiculoRepository  veiculoRepository;
    private final EnderecoRepository enderecoRepository;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Transactional
    public ApiResponse cadastrar(CadastroRequest req) {

        // 409 para duplicidades
        if (clienteRepository.existsByCpf(req.getCpf())) {
            throw new ConflictException("CPF já cadastrado");
        }
        if (clienteRepository.existsByEmail(req.getEmail())) {
            throw new ConflictException("E-mail já cadastrado");
        }
        if (veiculoRepository.existsByPlaca(req.getPlaca().toUpperCase())) {
            throw new ConflictException("Placa já cadastrada");
        }

        LocalDate dataNascimento;
        try {
            dataNascimento = LocalDate.parse(req.getDataNascimento(), DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new RequisicaoInvalidaException("Data de nascimento inválida. Use o formato DD/MM/AAAA");
        }

        String placaUpper = req.getPlaca().toUpperCase();

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

        Long clienteId = cliente.getId();

        Veiculo veiculo = new Veiculo();
        veiculo.setPlaca(placaUpper);
        veiculo.setNome(req.getModeloVeiculo());
        veiculo.setCor(req.getCorVeiculo());
        veiculo.setClienteId(clienteId);
        veiculoRepository.save(veiculo);

        Endereco endereco = new Endereco();
        endereco.setCep(req.getCep());
        endereco.setLogradouro(req.getLogradouro());
        endereco.setNumero(req.getNumero());
        endereco.setComplemento(req.getComplemento() != null ? req.getComplemento() : "");
        endereco.setBairro(req.getBairro());
        endereco.setCidade(req.getCidade());
        endereco.setEstado(req.getEstado().toUpperCase());
        endereco.setClienteId(clienteId);
        enderecoRepository.save(endereco);

        return ApiResponse.ok("Cadastro realizado com sucesso!");
    }
}
