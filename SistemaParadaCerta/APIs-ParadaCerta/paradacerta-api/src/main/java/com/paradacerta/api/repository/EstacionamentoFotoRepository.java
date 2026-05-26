package com.paradacerta.api.repository;

import com.paradacerta.api.model.EstacionamentoFoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstacionamentoFotoRepository extends JpaRepository<EstacionamentoFoto, Integer> {

    List<EstacionamentoFoto> findByEstacionamentoIdOrderByOrdemAscIdAsc(Integer estacionamentoId);

    long countByEstacionamentoId(Integer estacionamentoId);

    /** Zera a marcação de principal nas fotos atuais do estacionamento (antes de marcar uma nova). */
    @Modifying
    @Query("update EstacionamentoFoto f set f.principal = false where f.estacionamentoId = :estId and f.principal = true")
    void zerarPrincipal(@Param("estId") Integer estacionamentoId);
}
