package com.paradacerta.api.repository;

import com.paradacerta.api.model.SessaoEstacionamento;
import com.paradacerta.api.model.SessaoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SessaoRepository extends JpaRepository<SessaoEstacionamento, Long> {
    Optional<SessaoEstacionamento> findByCpfUsuarioAndStatus(String cpfUsuario, SessaoStatus status);
    boolean existsByCpfUsuarioAndStatus(String cpfUsuario, SessaoStatus status);
    Optional<SessaoEstacionamento> findByQrCode(String qrCode);
}
