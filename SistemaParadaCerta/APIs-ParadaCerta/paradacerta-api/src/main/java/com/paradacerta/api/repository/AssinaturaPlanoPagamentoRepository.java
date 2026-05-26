package com.paradacerta.api.repository;

import com.paradacerta.api.model.AssinaturaPlanoPagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssinaturaPlanoPagamentoRepository extends JpaRepository<AssinaturaPlanoPagamento, Long> {

    List<AssinaturaPlanoPagamento> findByEstacionamentoIdOrderByDataPagamentoDesc(Integer estacionamentoId);
}
