package com.paradacerta.api.repository;

import com.paradacerta.api.model.AdmEstacionamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdmEstacionamentoRepository extends JpaRepository<AdmEstacionamento, Integer> {

    Optional<AdmEstacionamento> findByUsuarioAndAtivoTrue(String usuario);
}
