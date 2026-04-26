package com.paradacerta.api.repository;

import com.paradacerta.api.model.QrCodeEntrada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface QrCodeEntradaRepository extends JpaRepository<QrCodeEntrada, Long> {

    Optional<QrCodeEntrada> findByTokenAndStatus(String token, String status);

    @Modifying
    @Query("UPDATE QrCodeEntrada q SET q.status = 'EXPIRADO' " +
           "WHERE q.estacionamentoId = :estacionamentoId AND q.status = 'DISPONIVEL'")
    void invalidarTokensDisponiveis(@Param("estacionamentoId") Integer estacionamentoId);

    @Query("SELECT q FROM QrCodeEntrada q WHERE q.token = :token " +
           "AND q.status = 'DISPONIVEL' AND q.expiradoEm > :agora")
    Optional<QrCodeEntrada> findTokenValido(@Param("token") String token,
                                            @Param("agora") LocalDateTime agora);
}
