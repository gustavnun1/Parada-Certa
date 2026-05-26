package com.paradacerta.api.repository;

import com.paradacerta.api.model.AdmEstacionamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdmEstacionamentoRepository extends JpaRepository<AdmEstacionamento, Integer> {

    Optional<AdmEstacionamento> findByEmailIgnoreCase(String email);

    Optional<AdmEstacionamento> findByCpf(String cpf);

    Optional<AdmEstacionamento> findByUsuario(String usuario);

    boolean existsByUsuario(String usuario);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByCpf(String cpf);

    List<AdmEstacionamento> findByEstacionamentoIdOrderByIdAsc(Integer estacionamentoId);
}
