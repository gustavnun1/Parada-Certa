package com.paradacerta.api.service;

import com.paradacerta.api.exception.ConteudoInvalidoException;
import com.paradacerta.api.exception.UsuarioNaoEncontradoException;
import com.paradacerta.api.model.Estacionamento;
import com.paradacerta.api.model.EstacionamentoFoto;
import com.paradacerta.api.model.EstacionamentoFotoResponse;
import com.paradacerta.api.repository.EstacionamentoFotoRepository;
import com.paradacerta.api.repository.EstacionamentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orquestra o upload de fotos do estacionamento.
 *
 * <ol>
 *   <li>Verifica plano (limite de fotos via {@link PlanoService}).</li>
 *   <li>Validação local: extensão, MIME, tamanho e magic bytes ({@link FotoValidatorService}).</li>
 *   <li>Moderação Google Vision SafeSearch ({@link ModeracaoImagemService}).</li>
 *   <li>Grava em disco em {@code paradacerta.uploads.dir/estacionamento/{id}/{uuid}.{ext}}.</li>
 *   <li>Persiste linha em {@link EstacionamentoFoto}; se {@code principal=true},
 *       atualiza também {@link Estacionamento#getFotoPrincipal()} para compatibilidade
 *       com o app mobile.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FotoService {

    private final EstacionamentoRepository estacionamentoRepository;
    private final EstacionamentoFotoRepository fotoRepository;
    private final FotoValidatorService fotoValidatorService;
    private final ModeracaoImagemService moderacaoImagemService;
    private final PlanoService planoService;

    @Value("${paradacerta.uploads.dir:./uploads}")
    private String uploadsDir;

    // ── Consultas ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EstacionamentoFotoResponse> listar(Integer estacionamentoId) {
        garantirEstacionamento(estacionamentoId);
        return fotoRepository.findByEstacionamentoIdOrderByOrdemAscIdAsc(estacionamentoId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Upload ───────────────────────────────────────────────────────────────

    @Transactional
    public EstacionamentoFotoResponse upload(Integer estacionamentoId, MultipartFile arquivo, boolean principal) {
        Estacionamento est = garantirEstacionamento(estacionamentoId);

        // 1) Limite do plano (Item 2)
        int limite = planoService.getLimiteFotos(est);
        if (limite <= 0) {
            throw new ConteudoInvalidoException(
                "Seu plano atual não permite envio de fotos. Faça upgrade para continuar."
            );
        }
        long atuais = fotoRepository.countByEstacionamentoId(estacionamentoId);
        if (atuais >= limite) {
            throw new ConteudoInvalidoException(
                "Limite de fotos do plano atingido (" + limite + "). Remova uma foto para enviar outra ou faça upgrade do plano."
            );
        }

        // 2) Validação local
        FotoValidatorService.ResultadoValidacao validacao = fotoValidatorService.validar(arquivo);

        // 3) Moderação (Google Vision SafeSearch). Fail-closed em caso de erro de rede.
        moderacaoImagemService.moderar(validacao.bytes);

        // 4) Persistência em disco
        Path destino;
        String caminhoRelativo;
        try {
            Path base = Paths.get(uploadsDir).toAbsolutePath().normalize();
            Path pastaEst = base.resolve("estacionamento").resolve(String.valueOf(estacionamentoId));
            Files.createDirectories(pastaEst);

            String nomeArquivo = UUID.randomUUID() + "." + validacao.extensao;
            destino = pastaEst.resolve(nomeArquivo);

            Files.write(destino, validacao.bytes,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);

            // Caminho relativo (usado para servir via /uploads/**)
            caminhoRelativo = "estacionamento/" + estacionamentoId + "/" + nomeArquivo;
        } catch (IOException e) {
            log.error("Falha ao gravar imagem em disco para estId={}", estacionamentoId, e);
            throw new ConteudoInvalidoException("Não foi possível salvar a imagem agora. Tente novamente em alguns instantes.");
        }

        // 5) Linha em EstacionamentoFoto
        if (principal) {
            fotoRepository.zerarPrincipal(estacionamentoId);
        }
        EstacionamentoFoto foto = new EstacionamentoFoto();
        foto.setEstacionamentoId(estacionamentoId);
        foto.setCaminho(caminhoRelativo);
        foto.setNomeOriginal(arquivo.getOriginalFilename());
        foto.setTipoMime(validacao.mime);
        foto.setTamanhoBytes(arquivo.getSize());
        foto.setPrincipal(principal);
        foto.setOrdem((int) atuais);
        foto.setCriadoEm(LocalDateTime.now());
        foto = fotoRepository.save(foto);

        // Mantém Estacionamento.fotoPrincipal consistente (app mobile usa)
        if (principal) {
            est.setFotoPrincipal("/uploads/" + caminhoRelativo);
            estacionamentoRepository.save(est);
        } else if (est.getFotoPrincipal() == null && atuais == 0) {
            // primeira foto torna-se a principal por padrão
            foto.setPrincipal(true);
            fotoRepository.save(foto);
            est.setFotoPrincipal("/uploads/" + caminhoRelativo);
            estacionamentoRepository.save(est);
        }

        log.info("Foto enviada para estId={}, fotoId={}, principal={}, tamanho={}b",
                estacionamentoId, foto.getId(), foto.getPrincipal(), foto.getTamanhoBytes());
        return toResponse(foto);
    }

    // ── Marcar principal ─────────────────────────────────────────────────────

    @Transactional
    public EstacionamentoFotoResponse marcarPrincipal(Integer estacionamentoId, Integer fotoId) {
        Estacionamento est = garantirEstacionamento(estacionamentoId);
        EstacionamentoFoto foto = fotoRepository.findById(fotoId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Foto não encontrada"));
        if (!foto.getEstacionamentoId().equals(estacionamentoId)) {
            throw new UsuarioNaoEncontradoException("Foto não encontrada para este estacionamento");
        }
        fotoRepository.zerarPrincipal(estacionamentoId);
        foto.setPrincipal(true);
        fotoRepository.save(foto);

        est.setFotoPrincipal("/uploads/" + foto.getCaminho());
        estacionamentoRepository.save(est);
        log.info("Foto marcada como principal: estId={}, fotoId={}", estacionamentoId, fotoId);
        return toResponse(foto);
    }

    // ── Remoção ──────────────────────────────────────────────────────────────

    @Transactional
    public void remover(Integer estacionamentoId, Integer fotoId) {
        Estacionamento est = garantirEstacionamento(estacionamentoId);
        EstacionamentoFoto foto = fotoRepository.findById(fotoId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Foto não encontrada"));

        if (!foto.getEstacionamentoId().equals(estacionamentoId)) {
            throw new UsuarioNaoEncontradoException("Foto não encontrada para este estacionamento");
        }

        // Apaga o arquivo do disco (não-fatal — log apenas).
        try {
            Path arquivo = Paths.get(uploadsDir).toAbsolutePath().normalize().resolve(foto.getCaminho());
            Files.deleteIfExists(arquivo);
        } catch (IOException e) {
            log.warn("Falha ao apagar arquivo físico {} (continuando com a remoção do registro): {}",
                    foto.getCaminho(), e.getMessage());
        }

        boolean eraPrincipal = Boolean.TRUE.equals(foto.getPrincipal());
        fotoRepository.delete(foto);

        if (eraPrincipal) {
            // Promove a primeira foto restante a principal (se houver).
            List<EstacionamentoFoto> restantes = fotoRepository.findByEstacionamentoIdOrderByOrdemAscIdAsc(estacionamentoId);
            if (!restantes.isEmpty()) {
                EstacionamentoFoto nova = restantes.get(0);
                nova.setPrincipal(true);
                fotoRepository.save(nova);
                est.setFotoPrincipal("/uploads/" + nova.getCaminho());
            } else {
                est.setFotoPrincipal(null);
            }
            estacionamentoRepository.save(est);
        }
        log.info("Foto removida: estId={}, fotoId={}", estacionamentoId, fotoId);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Estacionamento garantirEstacionamento(Integer estacionamentoId) {
        return estacionamentoRepository.findById(estacionamentoId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Estacionamento não encontrado"));
    }

    private EstacionamentoFotoResponse toResponse(EstacionamentoFoto f) {
        return new EstacionamentoFotoResponse(
                f.getId(),
                f.getEstacionamentoId(),
                "/uploads/" + f.getCaminho(),
                f.getNomeOriginal(),
                f.getTipoMime(),
                f.getTamanhoBytes(),
                f.getPrincipal(),
                f.getOrdem(),
                f.getCriadoEm()
        );
    }
}
