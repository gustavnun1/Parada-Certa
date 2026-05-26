package com.paradacerta.api.repository;

import com.paradacerta.api.model.VagasEstacionamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VagasEstacionamentoRepository extends JpaRepository<VagasEstacionamento, Integer> {

    Optional<VagasEstacionamento> findByEstacionamentoId(Integer estacionamentoId);

    List<VagasEstacionamento> findAllByEstacionamentoIdIn(List<Integer> estacionamentoIds);

    @Modifying
    @Query("UPDATE VagasEstacionamento v SET v.qtdVagasDisponiveis = v.qtdVagasDisponiveis - 1 WHERE v.estacionamentoId = :id AND v.qtdVagasDisponiveis > 0")
    int decrementarVaga(@Param("id") Integer estacionamentoId);

    @Modifying
    @Query("UPDATE VagasEstacionamento v SET v.qtdVagasDisponiveis = v.qtdVagasDisponiveis + 1 WHERE v.estacionamentoId = :id AND v.qtdVagasDisponiveis < v.qtdVagasTotais")
    int incrementarVaga(@Param("id") Integer estacionamentoId);

    @Modifying
    @Query("UPDATE VagasEstacionamento v SET v.qtdVagasReservadas = v.qtdVagasReservadas + 1 WHERE v.estacionamentoId = :id AND v.qtdVagasReservadas < v.qtdVagasReservaveis")
    int incrementarVagaReservada(@Param("id") Integer estacionamentoId);

    @Modifying
    @Query("UPDATE VagasEstacionamento v SET v.qtdVagasReservadas = v.qtdVagasReservadas - 1 WHERE v.estacionamentoId = :id AND v.qtdVagasReservadas > 0")
    int decrementarVagaReservada(@Param("id") Integer estacionamentoId);
}
