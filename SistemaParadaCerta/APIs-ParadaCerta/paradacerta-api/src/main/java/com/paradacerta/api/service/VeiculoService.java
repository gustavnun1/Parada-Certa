package com.paradacerta.api.service;

import com.paradacerta.api.exception.ConflictException;
import com.paradacerta.api.exception.RequisicaoInvalidaException;
import com.paradacerta.api.exception.UsuarioNaoEncontradoException;
import com.paradacerta.api.model.*;
import com.paradacerta.api.repository.ClienteRepository;
import com.paradacerta.api.repository.SessaoRepository;
import com.paradacerta.api.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VeiculoService {

    private static final int LIMITE_VEICULOS = 5;

    private final VeiculoRepository  veiculoRepository;
    private final ClienteRepository  clienteRepository;
    private final SessaoRepository   sessaoRepository;

    @Transactional(readOnly = true)
    public List<Veiculo> listar(String cpf) {
        Cliente cliente = clienteRepository.findByCpf(cpf)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado"));
        return veiculoRepository.findAllByClienteId(cliente.getId());
    }

    @Transactional
    public ApiResponse adicionar(VeiculoInput input) {
        Cliente cliente = clienteRepository.findByCpf(input.getCpf())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado"));

        if (veiculoRepository.countByClienteId(cliente.getId()) >= LIMITE_VEICULOS) {
            throw new RequisicaoInvalidaException("Limite de " + LIMITE_VEICULOS + " veículos atingido");
        }

        String placa = PlacaValidator.normalizar(input.getPlaca());
        if (!PlacaValidator.isValida(placa)) {
            throw new RequisicaoInvalidaException(PlacaValidator.MSG_FORMATO_INVALIDO);
        }

        if (veiculoRepository.existsByPlaca(placa)) {
            throw new ConflictException("Placa " + placa + " já cadastrada no sistema");
        }

        Veiculo veiculo = new Veiculo();
        veiculo.setPlaca(placa);
        veiculo.setNome(input.getModeloVeiculo());
        veiculo.setCor(input.getCorVeiculo());
        veiculo.setClienteId(cliente.getId());
        veiculoRepository.save(veiculo);

        return ApiResponse.ok("Veículo cadastrado com sucesso");
    }

    @Transactional
    public ApiResponse atualizar(String placa, VeiculoUpdateRequest request) {
        String placaUpper = placa.toUpperCase();

        Veiculo veiculo = veiculoRepository.findById(placaUpper)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Veículo não encontrado"));

        Cliente cliente = clienteRepository.findByCpf(request.getCpf())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado"));

        if (!veiculo.getClienteId().equals(cliente.getId())) {
            throw new RequisicaoInvalidaException("Este veículo não pertence ao usuário informado");
        }

        veiculo.setNome(request.getModeloVeiculo());
        veiculo.setCor(request.getCorVeiculo());
        veiculoRepository.save(veiculo);

        return ApiResponse.ok("Veículo atualizado com sucesso");
    }

    @Transactional
    public ApiResponse remover(String placa, String cpf) {
        String placaUpper = placa.toUpperCase();

        Veiculo veiculo = veiculoRepository.findById(placaUpper)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Veículo não encontrado"));

        Cliente cliente = clienteRepository.findByCpf(cpf)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado"));

        if (!veiculo.getClienteId().equals(cliente.getId())) {
            throw new RequisicaoInvalidaException("Este veículo não pertence ao usuário informado");
        }

        if (veiculoRepository.countByClienteId(cliente.getId()) <= 1) {
            throw new RequisicaoInvalidaException("É necessário manter pelo menos um veículo cadastrado");
        }

        if (sessaoRepository.existsByClienteIdAndStatus(cliente.getId(), SessaoStatus.ATIVA)) {
            SessaoEstacionamento sessaoAtiva = sessaoRepository
                    .findByClienteIdAndStatus(cliente.getId(), SessaoStatus.ATIVA)
                    .orElse(null);
            if (sessaoAtiva != null && placaUpper.equals(sessaoAtiva.getPlaca())) {
                throw new ConflictException("Este veículo está em uso numa sessão ativa. Encerre a sessão antes de remover o veículo");
            }
        }

        veiculoRepository.deleteById(placaUpper);

        return ApiResponse.ok("Veículo removido com sucesso");
    }
}
