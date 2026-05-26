package com.paradacerta.api.repository;

import com.paradacerta.api.model.SessaoEstacionamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessaoEstacionamentoRepository extends JpaRepository<SessaoEstacionamento, Long> {

    long countByEstacionamentoIdAndStatus(Integer estacionamentoId, String status);

    void deleteByEstacionamentoId(Integer estacionamentoId);
}
