package com.paradacerta.api.service;

import com.paradacerta.api.exception.RequisicaoInvalidaException;
import com.paradacerta.api.exception.UsuarioNaoEncontradoException;
import com.paradacerta.api.model.*;
import com.paradacerta.api.repository.AvaliacaoRepository;
import com.paradacerta.api.repository.ClienteRepository;
import com.paradacerta.api.repository.EstacionamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvaliacaoService {

    private final AvaliacaoRepository     avaliacaoRepository;
    private final EstacionamentoRepository estacionamentoRepository;
    private final ClienteRepository        clienteRepository;
    private final FiltroConteudoService    filtroConteudoService;  // camada 1: local, sempre ativa
    private final ModeracaoService         moderacaoService;       // camada 2: OpenAI API, opcional

    private static final String MSG_CONTEUDO_INADEQUADO =
        "Seu comentário contém conteúdo inadequado (linguagem ofensiva, violência ou " +
        "conteúdo sensível) e não pôde ser enviado. Por favor, revise e tente novamente.";

    @Transactional
    public ApiResponse avaliar(AvaliacaoRequest req) {
        Estacionamento estacionamento = estacionamentoRepository.findById(req.getEstacionamentoId())
                .orElseThrow(() -> new RequisicaoInvalidaException("Estacionamento não encontrado"));

        Cliente cliente = clienteRepository.findByCpf(req.getClienteCpf())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado"));

        // ── Moderação do comentário ───────────────────────────────────────────
        String comentario = req.getComentario();
        String comentarioFinal = null;

        if (comentario != null && !comentario.isBlank()) {
            // Camada 1: filtro local (palavrões em português) — sempre roda
            if (!filtroConteudoService.isConteudoApropriado(comentario)) {
                throw new RequisicaoInvalidaException(MSG_CONTEUDO_INADEQUADO);
            }

            // Camada 2: OpenAI Moderation API — roda se a chave estiver configurada
            if (!moderacaoService.isConteudoApropriado(comentario)) {
                throw new RequisicaoInvalidaException(MSG_CONTEUDO_INADEQUADO);
            }

            comentarioFinal = comentario.trim();
        }

        // ── Salvar avaliação ──────────────────────────────────────────────────
        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setEstacionamentoId(req.getEstacionamentoId());
        avaliacao.setClienteId(cliente.getId());
        avaliacao.setNota(req.getNota());
        avaliacao.setComentario(comentarioFinal);
        avaliacao.setDataAvaliacao(LocalDateTime.now());
        avaliacaoRepository.save(avaliacao);

        // Recalcula média do estacionamento
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
        return avaliacaoRepository
                .findByEstacionamentoIdOrderByDataAvaliacaoDesc(estacionamentoId)
                .stream()
                .map(a -> new AvaliacaoResponse(
                        a.getId(),
                        a.getNota(),
                        a.getComentario(),
                        a.getDataAvaliacao()
                ))
                .collect(Collectors.toList());
    }
}
