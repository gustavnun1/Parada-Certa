package com.paradacerta.api.repository;

import com.paradacerta.api.model.Veiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VeiculoRepository extends JpaRepository<Veiculo, String> {
    boolean existsByPlaca(String placa);
    Veiculo findByPlaca(String placa);
    java.util.List<Veiculo> findAllByClienteId(Long clienteId);
    int countByClienteId(Long clienteId);
    void deleteByClienteId(Long clienteId);
}
