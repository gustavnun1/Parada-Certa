package com.paradacerta.api.service;

import com.paradacerta.api.model.AdmEstacionamento;
import com.paradacerta.api.model.AdminLoginRequest;
import com.paradacerta.api.model.AdminLoginResponse;
import com.paradacerta.api.model.Estacionamento;
import com.paradacerta.api.repository.AdmEstacionamentoRepository;
import com.paradacerta.api.repository.EstacionamentoRepository;
import com.paradacerta.api.repository.VagasEstacionamentoRepository;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminServiceTest {

    private final AdmEstacionamentoRepository admRepository = mock(AdmEstacionamentoRepository.class);
    private final EstacionamentoRepository estacionamentoRepository = mock(EstacionamentoRepository.class);
    private final VagasEstacionamentoRepository vagasRepository = mock(VagasEstacionamentoRepository.class);
    private final OperadorService operadorService = mock(OperadorService.class);
    private final FotoService fotoService = mock(FotoService.class);
    private final AdminService service = new AdminService(
            admRepository,
            estacionamentoRepository,
            vagasRepository,
            operadorService,
            fotoService
    );

    @Test
    void loginAceitaCpfDoAdministrador() {
        AdmEstacionamento adm = new AdmEstacionamento();
        adm.setId(3);
        adm.setEstacionamentoId(9);
        adm.setUsuario("admin");
        adm.setNomeCompleto("Admin Teste");
        adm.setEmail("admin@paradacerta.com");
        adm.setTelefone("11999999999");
        adm.setCpf("12345678909");
        adm.setAtivo(true);
        adm.setSenhaHash(BCrypt.hashpw("123456", BCrypt.gensalt()));

        Estacionamento estacionamento = new Estacionamento();
        estacionamento.setId(9);
        estacionamento.setNome("Centro Park");

        AdminLoginRequest request = new AdminLoginRequest();
        request.setEmail("123.456.789-09");
        request.setSenha("123456");

        when(admRepository.findByCpf("12345678909")).thenReturn(Optional.of(adm));
        when(estacionamentoRepository.findById(9)).thenReturn(Optional.of(estacionamento));

        AdminLoginResponse response = service.login(request);

        verify(admRepository).findByCpf("12345678909");
        verify(admRepository, never()).findByEmailIgnoreCase(anyString());
        assertThat(response.getId()).isEqualTo(3);
        assertThat(response.getEstacionamentoId()).isEqualTo(9);
        assertThat(response.getEstacionamentoNome()).isEqualTo("Centro Park");
    }
}
