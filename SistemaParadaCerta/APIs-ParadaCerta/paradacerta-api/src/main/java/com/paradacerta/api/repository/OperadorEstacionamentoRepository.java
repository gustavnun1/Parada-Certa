package com.paradacerta.api.repository;

import com.paradacerta.api.model.OperadorEstacionamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OperadorEstacionamentoRepository extends JpaRepository<OperadorEstacionamento, Integer> {

    Optional<OperadorEstacionamento> findByEstacionamentoIdAndUsuarioAndAtivoTrue(
            Integer estacionamentoId,
            String usuario
    );

    boolean existsByEstacionamentoIdAndUsuario(Integer estacionamentoId, String usuario);

    boolean existsByCpf(String cpf);

    List<OperadorEstacionamento> findByEstacionamentoIdOrderByIdAsc(Integer estacionamentoId);
}
