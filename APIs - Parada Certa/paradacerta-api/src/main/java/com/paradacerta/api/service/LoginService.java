package com.paradacerta.api.service;

import com.paradacerta.api.exception.CredenciaisInvalidasException;
import com.paradacerta.api.exception.RequisicaoInvalidaException;
import com.paradacerta.api.exception.UsuarioNaoEncontradoException;
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

        boolean temEmail = req.getEmail() != null && !req.getEmail().isBlank();
        boolean temCpf   = req.getCpf()   != null && !req.getCpf().isBlank();

        if (!temEmail && !temCpf) {
            throw new RequisicaoInvalidaException("Informe o e-mail ou o CPF para realizar o login");
        }
        if (temEmail && temCpf) {
            throw new RequisicaoInvalidaException("Informe apenas um: e-mail ou CPF");
        }

        Optional<Cliente> clienteOpt = temCpf
                ? clienteRepository.findByCpf(req.getCpf())
                : clienteRepository.findByEmail(req.getEmail());

        if (clienteOpt.isEmpty()) {
            throw new UsuarioNaoEncontradoException("Usuário não encontrado");
        }

        Cliente cliente = clienteOpt.get();

        if (!BCrypt.checkpw(req.getSenha(), cliente.getSenha())) {
            throw new CredenciaisInvalidasException("Credenciais incorretas");
        }

        return ApiResponse.ok("Login realizado com sucesso!");
    }
}
