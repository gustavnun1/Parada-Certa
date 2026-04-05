package com.paradacerta.api.service;

import com.paradacerta.api.model.*;
import com.paradacerta.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.mindrot.jbcrypt.BCrypt;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final ClienteRepository clienteRepository;

    @Transactional(readOnly = true)
    public ApiResponse login(LoginRequest req) {

        Optional<Cliente> clienteOpt = clienteRepository.findByEmail(req.getEmail());

        if (clienteOpt.isEmpty()) {
            return ApiResponse.erro("E-mail ou senha incorretos");
        }

        Cliente cliente = clienteOpt.get();

        if (!BCrypt.checkpw(req.getSenha(), cliente.getSenha())) {
            return ApiResponse.erro("E-mail ou senha incorretos");
        }

        return ApiResponse.ok("Login realizado com sucesso!");
    }
}