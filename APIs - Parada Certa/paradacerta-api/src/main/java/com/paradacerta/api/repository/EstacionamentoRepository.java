package com.paradacerta.api.repository;

import com.paradacerta.api.model.Estacionamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;
import java.util.List;

@Repository
public interface EstacionamentoRepository extends JpaRepository<Estacionamento, Integer> {

    @Modifying
    @Query("UPDATE Estacionamento e SET e.qtdVagasDisponiveis = e.qtdVagasDisponiveis - 1 WHERE e.id = :id AND e.qtdVagasDisponiveis > 0")
    int decrementarVaga(@Param("id") Integer id);

    @Modifying
    @Query("UPDATE Estacionamento e SET e.qtdVagasDisponiveis = e.qtdVagasDisponiveis + 1 WHERE e.id = :id AND e.qtdVagasDisponiveis < e.qtdVagasTotais")
    int incrementarVaga(@Param("id") Integer id);

    List<Estacionamento> findByAtivoTrue();

    /**
     * Busca estacionamentos próximos a uma coordenada (raio em km)
     * Fórmula de Haversine para distância entre coordenadas
     */
    @Query(value = """
        SELECT id, nome, qtdVagasTotais, qtdVagasDisponiveis, avaliacaoMedia,
               latitude, longitude, endereco, precoHora, horarioAbertura,
               horarioFechamento, fotoPrincipal, descricao, ativo, pixKey
        FROM (
            SELECT *,
                (6371 * ACOS(
                    COS(RADIANS(:latitude)) *
                    COS(RADIANS(latitude)) *
                    COS(RADIANS(longitude) - RADIANS(:longitude)) +
                    SIN(RADIANS(:latitude)) *
                    SIN(RADIANS(latitude))
                )) AS distancia
            FROM Estacionamento
            WHERE ativo = 1
        ) sub
        WHERE sub.distancia <= :raioKm
        ORDER BY sub.distancia
        """, nativeQuery = true)
    List<Estacionamento> findEstacionamentosProximos(
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("raioKm") Double raioKm
    );
}