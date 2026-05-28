package com.paradacerta.api.service;

import com.paradacerta.api.exception.ConflictException;
import com.paradacerta.api.exception.RequisicaoInvalidaException;
import com.paradacerta.api.model.ApiResponse;
import com.paradacerta.api.model.CadastroRequest;
import com.paradacerta.api.model.Cliente;
import com.paradacerta.api.model.Endereco;
import com.paradacerta.api.model.Veiculo;
import com.paradacerta.api.repository.ClienteRepository;
import com.paradacerta.api.repository.EnderecoRepository;
import com.paradacerta.api.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CadastroService {

    private final ClienteRepository clienteRepository;
    private final VeiculoRepository veiculoRepository;
    private final EnderecoRepository enderecoRepository;

    @Transactional
    public ApiResponse cadastrar(CadastroRequest req) {
        String cpf = DocumentoValidator.somenteDigitos(req.getCpf());
        DocumentoValidator.validarCpf(cpf);
        String nome = UserFieldValidator.normalizarNome(req.getNome());
        String email = UserFieldValidator.normalizarEmail(req.getEmail());
        UserFieldValidator.validarSenha(req.getSenha(), true);
        String numeroCelular = UserFieldValidator.normalizarTelefone(req.getNumeroCelular());
        LocalDate dataNascimento = UserFieldValidator.parseDataNascimento(req.getDataNascimento());
        String cep = UserFieldValidator.normalizarCep(req.getCep());
        String logradouro = UserFieldValidator.validarTextoObrigatorio(req.getLogradouro(), "Logradouro é obrigatório.", 120);
        String numero = UserFieldValidator.normalizarNumeroEndereco(req.getNumero());
        String bairro = UserFieldValidator.validarTextoObrigatorio(req.getBairro(), "Bairro é obrigatório.", 80);
        String cidade = UserFieldValidator.validarTextoObrigatorio(req.getCidade(), "Cidade é obrigatória.", 80);
        String estado = UserFieldValidator.normalizarUf(req.getEstado());

        if (clienteRepository.existsByCpf(cpf)) {
            throw new ConflictException("CPF já cadastrado");
        }
        if (clienteRepository.existsByEmail(email)) {
            throw new ConflictException("E-mail já cadastrado");
        }

        String placaUpper = PlacaValidator.normalizar(req.getPlaca());
        if (!PlacaValidator.isValida(placaUpper)) {
            throw new RequisicaoInvalidaException(PlacaValidator.MSG_FORMATO_INVALIDO);
        }
        if (veiculoRepository.existsByPlaca(placaUpper)) {
            throw new ConflictException("Placa já cadastrada");
        }

        Cliente cliente = new Cliente();
        cliente.setCpf(cpf);
        cliente.setNome(nome);
        cliente.setEmail(email);
        cliente.setSenha(BCrypt.hashpw(req.getSenha(), BCrypt.gensalt()));
        cliente.setDataNascimento(dataNascimento);
        cliente.setNumeroCelular(numeroCelular);
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
        endereco.setCep(cep);
        endereco.setLogradouro(logradouro);
        endereco.setNumero(numero);
        endereco.setComplemento(req.getComplemento() != null ? req.getComplemento().trim() : "");
        endereco.setBairro(bairro);
        endereco.setCidade(cidade);
        endereco.setEstado(estado);
        endereco.setClienteId(clienteId);
        enderecoRepository.save(endereco);

        return ApiResponse.ok("Cadastro realizado com sucesso!");
    }
}
