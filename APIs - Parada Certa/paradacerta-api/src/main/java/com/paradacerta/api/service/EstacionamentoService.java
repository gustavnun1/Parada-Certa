package com.paradacerta.api.service;

import com.paradacerta.api.model.Estacionamento;
import com.paradacerta.api.repository.EstacionamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EstacionamentoService {

    private final EstacionamentoRepository estacionamentoRepository;

    public List<Estacionamento> buscarTodos() {
        return estacionamentoRepository.findByAtivoTrue();
    }

    public List<Estacionamento> buscarProximos(Double latitude, Double longitude, Double raioKm) {
        return estacionamentoRepository.findEstacionamentosProximos(latitude, longitude, raioKm);
    }

    public Optional<Estacionamento> buscarPorId(Integer id) {
        return estacionamentoRepository.findById(id);
    }
}