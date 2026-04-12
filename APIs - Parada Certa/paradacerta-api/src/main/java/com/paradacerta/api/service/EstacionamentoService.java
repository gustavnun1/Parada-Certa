package com.paradacerta.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paradacerta.api.exception.UsuarioNaoEncontradoException;
import com.paradacerta.api.model.Estacionamento;
import com.paradacerta.api.model.QrResponse;
import com.paradacerta.api.repository.EstacionamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EstacionamentoService {

    private final EstacionamentoRepository estacionamentoRepository;
    private final ObjectMapper objectMapper;

    public List<Estacionamento> buscarTodos() {
        return estacionamentoRepository.findByAtivoTrue();
    }

    public List<Estacionamento> buscarProximos(Double latitude, Double longitude, Double raioKm) {
        return estacionamentoRepository.findEstacionamentosProximos(latitude, longitude, raioKm);
    }

    public Optional<Estacionamento> buscarPorId(Integer id) {
        return estacionamentoRepository.findById(id);
    }

    @Transactional
    public void decrementarVaga(Integer id) {
        estacionamentoRepository.decrementarVaga(id);
    }

    @Transactional
    public void incrementarVaga(Integer id) {
        estacionamentoRepository.incrementarVaga(id);
    }

    public QrResponse gerarQrEntrada(Integer id) {
        Estacionamento e = estacionamentoRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Estacionamento não encontrado"));

        Map<String, Object> qrMap = new LinkedHashMap<>();
        qrMap.put("tipo", "ENTRADA");
        qrMap.put("id", e.getId());
        qrMap.put("nome", e.getNome());
        qrMap.put("precoHora", e.getPrecoHora());

        try {
            return new QrResponse(objectMapper.writeValueAsString(qrMap));
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Erro ao gerar JSON do QR Code", ex);
        }
    }
}