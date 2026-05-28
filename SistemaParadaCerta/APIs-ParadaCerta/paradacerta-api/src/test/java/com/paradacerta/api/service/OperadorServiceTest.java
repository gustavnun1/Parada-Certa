package com.paradacerta.api.service;

import com.paradacerta.api.exception.ConflictException;
import com.paradacerta.api.model.OperadorEstacionamento;
import com.paradacerta.api.model.OperadorRequest;
import com.paradacerta.api.model.OperadorUpdateRequest;
import com.paradacerta.api.repository.EstacionamentoRepository;
import com.paradacerta.api.repository.OperadorEstacionamentoRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperadorServiceTest {

    private final OperadorEstacionamentoRepository operadorRepository = mock(OperadorEstacionamentoRepository.class);
    private final EstacionamentoRepository estacionamentoRepository = mock(EstacionamentoRepository.class);
    private final OperadorService service = new OperadorService(operadorRepository, estacionamentoRepository);

    @Test
    void criarValidaUsuarioDuplicadoDentroDoMesmoEstacionamento() {
        OperadorRequest req = request(7, "operador");

        when(estacionamentoRepository.existsById(7)).thenReturn(true);
        when(operadorRepository.existsByEstacionamentoIdAndUsuario(7, "operador")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(req))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Já existe um operador com este nome de usuário");
    }

    @Test
    void criarPermiteMesmoUsuarioEmOutroEstacionamento() {
        OperadorRequest req = request(8, "operador");
        ArgumentCaptor<OperadorEstacionamento> captor = ArgumentCaptor.forClass(OperadorEstacionamento.class);

        when(estacionamentoRepository.existsById(8)).thenReturn(true);
        when(operadorRepository.existsByEstacionamentoIdAndUsuario(8, "operador")).thenReturn(false);
        when(operadorRepository.existsByCpf("12345678909")).thenReturn(false);
        when(operadorRepository.save(any(OperadorEstacionamento.class))).thenAnswer(invocation -> {
            OperadorEstacionamento op = invocation.getArgument(0);
            op.setId(33);
            return op;
        });

        service.criar(req);

        verify(operadorRepository).save(captor.capture());
        assertThat(captor.getValue().getEstacionamentoId()).isEqualTo(8);
        assertThat(captor.getValue().getUsuario()).isEqualTo("operador");
    }

    @Test
    void atualizarValidaNovoUsuarioNoEstacionamentoDoOperador() {
        OperadorEstacionamento op = operador(10, 5, "atual");
        OperadorUpdateRequest req = new OperadorUpdateRequest();
        req.setUsuario("novo");

        when(operadorRepository.findById(10)).thenReturn(Optional.of(op));
        when(operadorRepository.existsByEstacionamentoIdAndUsuario(5, "novo")).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(10, req))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Já existe um operador com este nome de usuário");
    }

    private static OperadorRequest request(Integer estacionamentoId, String usuario) {
        OperadorRequest req = new OperadorRequest();
        req.setEstacionamentoId(estacionamentoId);
        req.setNome("Operador Teste");
        req.setUsuario(usuario);
        req.setSenha("123456");
        req.setCpf("12345678909");
        req.setEmail("operador@teste.com");
        req.setTelefone("11999999999");
        req.setCep("01001000");
        req.setLogradouro("Praca da Se");
        req.setNumero("1");
        req.setBairro("Se");
        req.setCidade("Sao Paulo");
        req.setUf("SP");
        return req;
    }

    private static OperadorEstacionamento operador(Integer id, Integer estacionamentoId, String usuario) {
        OperadorEstacionamento op = new OperadorEstacionamento();
        op.setId(id);
        op.setEstacionamentoId(estacionamentoId);
        op.setNome("Operador Teste");
        op.setUsuario(usuario);
        op.setSenhaHash("hash");
        op.setAtivo(true);
        op.setCriadoEm(LocalDateTime.now());
        return op;
    }
}
