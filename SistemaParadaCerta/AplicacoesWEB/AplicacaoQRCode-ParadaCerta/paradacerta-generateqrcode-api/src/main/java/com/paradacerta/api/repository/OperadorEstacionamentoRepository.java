package com.paradacerta.api.repository;

import com.paradacerta.api.model.OperadorEstacionamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface OperadorEstacionamentoRepository extends JpaRepository<OperadorEstacionamento, Integer> {

    Optional<OperadorEstacionamento> findByEstacionamentoIdAndUsuarioAndAtivoTrue(
            Integer estacionamentoId,
            String usuario
    );

    List<OperadorEstacionamento> findByEstacionamentoId(Integer estacionamentoId);

    void deleteByEstacionamentoId(Integer estacionamentoId);
}
