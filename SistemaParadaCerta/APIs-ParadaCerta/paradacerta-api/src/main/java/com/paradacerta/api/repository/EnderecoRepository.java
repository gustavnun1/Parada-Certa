package com.paradacerta.api.repository;

import com.paradacerta.api.model.Endereco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EnderecoRepository extends JpaRepository<Endereco, Integer> {
    Optional<Endereco> findByClienteId(Long clienteId);
}