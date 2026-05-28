package com.paradacerta.api.service;

import com.paradacerta.api.exception.RequisicaoInvalidaException;
import com.paradacerta.api.exception.UsuarioNaoEncontradoException;
import com.paradacerta.api.model.ApiResponse;
import com.paradacerta.api.model.Avaliacao;
import com.paradacerta.api.model.AvaliacaoRequest;
import com.paradacerta.api.model.AvaliacaoResponse;
import com.paradacerta.api.model.Cliente;
import com.paradacerta.api.model.Estacionamento;
import com.paradacerta.api.repository.AvaliacaoRepository;
import com.paradacerta.api.repository.ClienteRepository;
import com.paradacerta.api.repository.EstacionamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AvaliacaoService {
    private static final ZoneId ZONE_SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final String MSG_CONTEUDO_INADEQUADO =
            "Seu comentário contém conteúdo inadequado (linguagem ofensiva, violência ou " +
            "conteúdo sensível) e não pôde ser enviado. Por favor, revise e tente novamente.";

    private final AvaliacaoRepository avaliacaoRepository;
    private final EstacionamentoRepository estacionamentoRepository;
    private final ClienteRepository clienteRepository;
    private final FiltroConteudoService filtroConteudoService;
    private final ModeracaoService moderacaoService;

    @Transactional
    public ApiResponse avaliar(AvaliacaoRequest req) {
        Estacionamento estacionamento = estacionamentoRepository.findById(req.getEstacionamentoId())
                .orElseThrow(() -> new RequisicaoInvalidaException("Estacionamento não encontrado"));

        Cliente cliente = clienteRepository.findByCpf(req.getClienteCpf())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado"));

        String comentarioFinal = validarComentario(req.getComentario());

        Avaliacao avaliacao = avaliacaoRepository
                .findByEstacionamentoIdAndClienteId(req.getEstacionamentoId(), cliente.getId())
                .orElseGet(Avaliacao::new);
        avaliacao.setEstacionamentoId(req.getEstacionamentoId());
        avaliacao.setClienteId(cliente.getId());
        avaliacao.setNota(req.getNota());
        avaliacao.setComentario(comentarioFinal);
        avaliacao.setDataAvaliacao(LocalDateTime.now(ZONE_SAO_PAULO));
        avaliacaoRepository.save(avaliacao);

        Double media = avaliacaoRepository.calcularMedia(req.getEstacionamentoId());
        if (media != null) {
            estacionamento.setAvaliacaoMedia(
                    BigDecimal.valueOf(media).setScale(2, RoundingMode.HALF_UP)
            );
            estacionamentoRepository.save(estacionamento);
        }

        return ApiResponse.ok("Avaliação registrada com sucesso. Obrigado pelo seu feedback!");
    }

    @Transactional(readOnly = true)
    public List<AvaliacaoResponse> listar(Integer estacionamentoId) {
        estacionamentoRepository.findById(estacionamentoId)
                .orElseThrow(() -> new RequisicaoInvalidaException("Estacionamento não encontrado"));

        return avaliacaoRepository
                .findByEstacionamentoIdOrderByDataAvaliacaoDesc(estacionamentoId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private String validarComentario(String comentario) {
        if (comentario == null || comentario.isBlank()) {
            return null;
        }

        if (!filtroConteudoService.isConteudoApropriado(comentario)
                || !moderacaoService.isConteudoApropriado(comentario)) {
            throw new RequisicaoInvalidaException(MSG_CONTEUDO_INADEQUADO);
        }

        return comentario.trim();
    }

    private AvaliacaoResponse toResponse(Avaliacao avaliacao) {
        String clienteNome = clienteRepository.findById(avaliacao.getClienteId())
                .map(Cliente::getNome)
                .orElse("Cliente");

        return new AvaliacaoResponse(
                avaliacao.getId(),
                avaliacao.getEstacionamentoId(),
                avaliacao.getClienteId(),
                clienteNome,
                avaliacao.getNota(),
                avaliacao.getComentario(),
                avaliacao.getDataAvaliacao()
        );
    }
}
