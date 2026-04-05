package com.paradacerta.api.repository;

import com.paradacerta.api.model.Estacionamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstacionamentoRepository extends JpaRepository<Estacionamento, Integer> {

    List<Estacionamento> findByAtivoTrue();

    /**
     * Busca estacionamentos próximos a uma coordenada (raio em km)
     * Fórmula de Haversine para distância entre coordenadas
     */
    @Query(value = """
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
        HAVING distancia <= :raioKm
        ORDER BY distancia
        """, nativeQuery = true)
    List<Estacionamento> findEstacionamentosProximos(
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("raioKm") Double raioKm
    );
}