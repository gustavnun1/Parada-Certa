package com.paradacerta.api.service;

import com.paradacerta.api.exception.ConflictException;
import com.paradacerta.api.exception.UsuarioNaoEncontradoException;
import com.paradacerta.api.model.ApiResponse;
import com.paradacerta.api.model.Cliente;
import com.paradacerta.api.model.ClienteUpdateRequest;
import com.paradacerta.api.model.Endereco;
import com.paradacerta.api.repository.ClienteRepository;
import com.paradacerta.api.repository.EnderecoRepository;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SaverService {

    private final ClienteRepository clienteRepository;
    private final EnderecoRepository enderecoRepository;

    @Transactional
    public ApiResponse salvar(ClienteUpdateRequest req) {
        String cpf = DocumentoValidator.somenteDigitos(req.getCpf());
        DocumentoValidator.validarCpf(cpf);
        String nome = UserFieldValidator.normalizarNome(req.getNome());
        String email = UserFieldValidator.normalizarEmail(req.getEmail());
        UserFieldValidator.validarSenha(req.getSenha(), false);
        String numeroCelular = UserFieldValidator.normalizarTelefone(req.getNumeroCelular());
        LocalDate dataNascimento = UserFieldValidator.parseDataNascimento(req.getDataNascimento());
        String cep = UserFieldValidator.normalizarCep(req.getCep());
        String logradouro = UserFieldValidator.validarTextoObrigatorio(req.getLogradouro(), "Logradouro é obrigatório.", 120);
        String numero = UserFieldValidator.normalizarNumeroEndereco(req.getNumero());
        String bairro = UserFieldValidator.validarTextoObrigatorio(req.getBairro(), "Bairro é obrigatório.", 80);
        String cidade = UserFieldValidator.validarTextoObrigatorio(req.getCidade(), "Cidade é obrigatória.", 80);
        String estado = UserFieldValidator.normalizarUf(req.getEstado());

        Cliente cliente = clienteRepository.findByCpf(cpf)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Cliente não encontrado"));

        Long clienteId = cliente.getId();

        if (!cliente.getEmail().equals(email) && clienteRepository.existsByEmail(email)) {
            throw new ConflictException("E-mail já cadastrado por outro usuário");
        }

        cliente.setNome(nome);
        cliente.setEmail(email);

        if (req.getSenha() != null && !req.getSenha().isBlank()) {
            cliente.setSenha(BCrypt.hashpw(req.getSenha(), BCrypt.gensalt()));
        }

        cliente.setDataNascimento(dataNascimento);
        cliente.setNumeroCelular(numeroCelular);
        clienteRepository.save(cliente);

        Endereco endereco = enderecoRepository.findByClienteId(clienteId).orElseGet(() -> {
            Endereco e = new Endereco();
            e.setClienteId(clienteId);
            return e;
        });
        endereco.setCep(cep);
        endereco.setLogradouro(logradouro);
        endereco.setNumero(numero);
        endereco.setComplemento(req.getComplemento() != null ? req.getComplemento().trim() : "");
        endereco.setBairro(bairro);
        endereco.setCidade(cidade);
        endereco.setEstado(estado);
        enderecoRepository.save(endereco);

        return ApiResponse.ok("Dados atualizados com sucesso!");
    }
}
