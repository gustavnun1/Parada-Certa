package com.paradacerta.api.repository;

import com.paradacerta.api.model.VagasEstacionamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VagasEstacionamentoRepository extends JpaRepository<VagasEstacionamento, Integer> {

    Optional<VagasEstacionamento> findByEstacionamentoId(Integer estacionamentoId);

    void deleteByEstacionamentoId(Integer estacionamentoId);
}
