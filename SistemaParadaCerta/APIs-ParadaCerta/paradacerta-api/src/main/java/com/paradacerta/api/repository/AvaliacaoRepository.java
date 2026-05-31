package com.paradacerta.api.repository;

import com.paradacerta.api.model.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Integer> {

    @Query("SELECT AVG(CAST(a.nota AS double)) FROM Avaliacao a WHERE a.estacionamentoId = :id")
    Double calcularMedia(@Param("id") Integer estacionamentoId);

    List<Avaliacao> findByEstacionamentoIdOrderByDataAvaliacaoDesc(Integer estacionamentoId);

    Optional<Avaliacao> findByEstacionamentoIdAndClienteId(Integer estacionamentoId, Long clienteId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Avaliacao a WHERE a.clienteId = :clienteId")
    int deleteByClienteId(@Param("clienteId") Long clienteId);
}
