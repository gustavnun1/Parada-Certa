package com.paradacerta.api.service;

import com.paradacerta.api.model.Avaliacao;
import com.paradacerta.api.model.AvaliacaoRequest;
import com.paradacerta.api.model.AvaliacaoResponse;
import com.paradacerta.api.model.Cliente;
import com.paradacerta.api.model.Estacionamento;
import com.paradacerta.api.repository.AvaliacaoRepository;
import com.paradacerta.api.repository.ClienteRepository;
import com.paradacerta.api.repository.EstacionamentoRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AvaliacaoServiceTest {

    private final AvaliacaoRepository avaliacaoRepository = mock(AvaliacaoRepository.class);
    private final EstacionamentoRepository estacionamentoRepository = mock(EstacionamentoRepository.class);
    private final ClienteRepository clienteRepository = mock(ClienteRepository.class);
    private final FiltroConteudoService filtroConteudoService = mock(FiltroConteudoService.class);
    private final ModeracaoService moderacaoService = mock(ModeracaoService.class);

    private final AvaliacaoService service = new AvaliacaoService(
            avaliacaoRepository,
            estacionamentoRepository,
            clienteRepository,
            filtroConteudoService,
            moderacaoService
    );

    @Test
    void salvarAvaliacaoVinculaEstacionamentoEClienteCorretosERecalculaMedia() {
        Estacionamento estacionamento = new Estacionamento();
        estacionamento.setId(10);
        Cliente cliente = cliente(7L, "12345678909", "Maria Silva");

        when(estacionamentoRepository.findById(10)).thenReturn(Optional.of(estacionamento));
        when(clienteRepository.findByCpf("12345678909")).thenReturn(Optional.of(cliente));
        when(avaliacaoRepository.findByEstacionamentoIdAndClienteId(10, 7L)).thenReturn(Optional.empty());
        when(avaliacaoRepository.calcularMedia(10)).thenReturn(4.5);
        when(filtroConteudoService.isConteudoApropriado("Bom atendimento")).thenReturn(true);
        when(moderacaoService.isConteudoApropriado("Bom atendimento")).thenReturn(true);

        AvaliacaoRequest request = new AvaliacaoRequest();
        request.setEstacionamentoId(10);
        request.setClienteCpf("12345678909");
        request.setNota(5);
        request.setComentario("Bom atendimento");

        service.avaliar(request);

        verify(avaliacaoRepository).save(any(Avaliacao.class));
        assertThat(estacionamento.getAvaliacaoMedia()).isEqualByComparingTo(new BigDecimal("4.50"));
        verify(estacionamentoRepository).save(estacionamento);
    }

    @Test
    void listarRetornaSomenteAvaliacoesDoEstacionamentoComDadosDoCliente() {
        Estacionamento estacionamento = new Estacionamento();
        estacionamento.setId(10);
        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setId(1);
        avaliacao.setEstacionamentoId(10);
        avaliacao.setClienteId(7L);
        avaliacao.setNota(4);
        avaliacao.setComentario("Seguro e organizado");
        avaliacao.setDataAvaliacao(java.time.LocalDateTime.of(2026, 5, 27, 18, 30));

        when(estacionamentoRepository.findById(10)).thenReturn(Optional.of(estacionamento));
        when(avaliacaoRepository.findByEstacionamentoIdOrderByDataAvaliacaoDesc(10))
                .thenReturn(List.of(avaliacao));
        when(clienteRepository.findById(7L)).thenReturn(Optional.of(cliente(7L, "12345678909", "Maria Silva")));

        List<AvaliacaoResponse> respostas = service.listar(10);

        assertThat(respostas).hasSize(1);
        AvaliacaoResponse resposta = respostas.get(0);
        assertThat(resposta.getEstacionamentoId()).isEqualTo(10);
        assertThat(resposta.getClienteId()).isEqualTo(7L);
        assertThat(resposta.getClienteNome()).isEqualTo("Maria Silva");
        assertThat(resposta.getNota()).isEqualTo(4);
    }

    private Cliente cliente(Long id, String cpf, String nome) {
        Cliente cliente = new Cliente();
        cliente.setId(id);
        cliente.setCpf(cpf);
        cliente.setNome(nome);
        return cliente;
    }
}
