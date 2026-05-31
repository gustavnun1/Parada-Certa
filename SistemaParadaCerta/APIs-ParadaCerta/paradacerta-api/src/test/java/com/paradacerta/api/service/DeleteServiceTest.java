package com.paradacerta.api.service;

import com.paradacerta.api.exception.ConflictException;
import com.paradacerta.api.model.Cliente;
import com.paradacerta.api.model.Endereco;
import com.paradacerta.api.repository.AvaliacaoRepository;
import com.paradacerta.api.repository.ClienteRepository;
import com.paradacerta.api.repository.EnderecoRepository;
import com.paradacerta.api.repository.FormaPagamentoRepository;
import com.paradacerta.api.repository.SessaoRepository;
import com.paradacerta.api.repository.VeiculoRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeleteServiceTest {

    private final ClienteRepository clienteRepository = mock(ClienteRepository.class);
    private final VeiculoRepository veiculoRepository = mock(VeiculoRepository.class);
    private final EnderecoRepository enderecoRepository = mock(EnderecoRepository.class);
    private final AvaliacaoRepository avaliacaoRepository = mock(AvaliacaoRepository.class);
    private final FormaPagamentoRepository formaPagamentoRepository = mock(FormaPagamentoRepository.class);
    private final SessaoRepository sessaoRepository = mock(SessaoRepository.class);

    private final DeleteService service = new DeleteService(
            clienteRepository,
            veiculoRepository,
            enderecoRepository,
            avaliacaoRepository,
            formaPagamentoRepository,
            sessaoRepository
    );

    @Test
    void bloqueiaExclusaoQuandoClienteTemSessaoViva() {
        Cliente cliente = cliente();
        when(clienteRepository.findByCpf("12345678909")).thenReturn(Optional.of(cliente));
        when(sessaoRepository.existsSessaoVivaDoCliente(7L)).thenReturn(true);

        assertThatThrownBy(() -> service.deletarConta("12345678909"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Encerre ou cancele sua sessao/reserva ativa antes de excluir a conta.");

        verify(avaliacaoRepository, never()).deleteByClienteId(7L);
        verify(sessaoRepository, never()).desassociarCliente(7L);
        verify(clienteRepository, never()).delete(cliente);
    }

    @Test
    void removeDependenciasEAnonimizaHistoricoAntesDeExcluirCliente() {
        Cliente cliente = cliente();
        Endereco endereco = new Endereco();

        when(clienteRepository.findByCpf("12345678909")).thenReturn(Optional.of(cliente));
        when(sessaoRepository.existsSessaoVivaDoCliente(7L)).thenReturn(false);
        when(enderecoRepository.findByClienteId(7L)).thenReturn(Optional.of(endereco));

        service.deletarConta("12345678909");

        InOrder ordem = inOrder(
                avaliacaoRepository,
                sessaoRepository,
                enderecoRepository,
                formaPagamentoRepository,
                veiculoRepository,
                clienteRepository
        );
        ordem.verify(avaliacaoRepository).deleteByClienteId(7L);
        ordem.verify(sessaoRepository).desassociarCliente(7L);
        ordem.verify(enderecoRepository).delete(endereco);
        ordem.verify(formaPagamentoRepository).deleteByClienteId(7L);
        ordem.verify(veiculoRepository).deleteByClienteId(7L);
        ordem.verify(clienteRepository).delete(cliente);
    }

    private static Cliente cliente() {
        Cliente cliente = new Cliente();
        cliente.setId(7L);
        cliente.setCpf("12345678909");
        cliente.setNome("Maria Silva");
        cliente.setEmail("maria@teste.com");
        return cliente;
    }
}
