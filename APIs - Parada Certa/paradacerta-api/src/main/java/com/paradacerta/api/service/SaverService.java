package com.paradacerta.api.service;

import com.paradacerta.api.exception.ConflictException;
import com.paradacerta.api.exception.UsuarioNaoEncontradoException;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SaverService {

    private final ClienteRepository  clienteRepository;
    private final VeiculoRepository  veiculoRepository;
    private final EnderecoRepository enderecoRepository;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Transactional
    public ApiResponse salvar(CadastroRequest req) {

        Cliente cliente = clienteRepository.findById(req.getCpf())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Cliente não encontrado"));

        // 409 se e-mail mudou para um já usado por outro
        if (!cliente.getEmail().equals(req.getEmail()) &&
                clienteRepository.existsByEmail(req.getEmail())) {
            throw new ConflictException("E-mail já cadastrado por outro usuário");
        }

        LocalDate dataNascimento;
        try {
            dataNascimento = LocalDate.parse(req.getDataNascimento(), DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new RequisicaoInvalidaException("Data de nascimento inválida. Use o formato DD/MM/AAAA");
        }

        String placaUpper = req.getPlaca().toUpperCase();

        cliente.setNome(req.getNome());
        cliente.setEmail(req.getEmail());

        if (req.getSenha() != null && !req.getSenha().isEmpty()) {
            cliente.setSenha(BCrypt.hashpw(req.getSenha(), BCrypt.gensalt()));
        }

        cliente.setDataNascimento(dataNascimento);
        cliente.setNumeroCelular(req.getNumeroCelular() != null ? req.getNumeroCelular() : "");
        cliente.setPlaca(placaUpper);
        cliente.setVeiculo(req.getModeloVeiculo());
        cliente.setPremium(req.isPremium());
        clienteRepository.save(cliente);

        Optional<Veiculo> veiculoOpt = veiculoRepository.findById(cliente.getPlaca());
        Veiculo veiculo = veiculoOpt.orElseGet(() -> {
            Veiculo v = new Veiculo();
            v.setPlaca(placaUpper);
            v.setResponsavel(req.getCpf());
            return v;
        });
        veiculo.setNome(req.getModeloVeiculo());
        veiculo.setCor(req.getCorVeiculo());
        veiculoRepository.save(veiculo);

        Endereco endereco = enderecoRepository.findByCpfCliente(req.getCpf()).orElseGet(() -> {
            Endereco e = new Endereco();
            e.setCpfCliente(req.getCpf());
            return e;
        });
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
