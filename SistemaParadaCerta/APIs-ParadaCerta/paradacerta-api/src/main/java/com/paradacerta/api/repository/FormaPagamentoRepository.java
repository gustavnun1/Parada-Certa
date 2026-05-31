package com.paradacerta.api.repository;

import com.paradacerta.api.model.FormaPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FormaPagamentoRepository extends JpaRepository<FormaPagamento, Integer> {
    List<FormaPagamento> findByClienteId(Long clienteId);
    void deleteByClienteId(Long clienteId);
}
