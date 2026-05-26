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

    boolean existsByCnpj(String cnpj);

    /**
     * Lista estacionamentos vinculados ao administrador informado.
     * Usado no painel web para que cada admin veja apenas o(s) seu(s) estacionamento(s).
     */
    @Query("""
        SELECT e FROM Estacionamento e
        WHERE e.ativo = true
          AND e.id IN (
              SELECT a.estacionamentoId FROM AdmEstacionamento a
              WHERE a.id = :admId AND a.ativo = true
          )
        ORDER BY e.nome
    """)
    List<Estacionamento> findEstacionamentosDoAdmin(@Param("admId") Integer admId);

    /**
     * Busca estacionamentos próximos a uma coordenada (raio em km)
     * Fórmula de Haversine para distância entre coordenadas
     */
    @Query(value = """
        SELECT id, nome, cnpj, razaoSocial, nomeFantasia, avaliacaoMedia,
               latitude, longitude, endereco, precoHora, horarioAbertura,
               horarioFechamento, fotoPrincipal, descricao, ativo, pixKey,
               permiteReserva, cep, logradouro, numero, complemento, bairro,
               cidade, uf, plano, planoInicio, planoFim, planoCobranca
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

    /** Estacionamentos ativos na mesma cidade (case-insensitive). Usado em relatórios regionais (PREMIUM). */
    @Query("""
        SELECT e FROM Estacionamento e
        WHERE e.ativo = true
          AND e.cidade IS NOT NULL
          AND LOWER(e.cidade) = LOWER(:cidade)
    """)
    List<Estacionamento> findAtivosPorCidade(@Param("cidade") String cidade);
}
