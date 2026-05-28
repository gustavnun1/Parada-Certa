package com.paradacerta.api.service;

import com.paradacerta.api.dto.KioskAdmDTO;
import com.paradacerta.api.model.Estacionamento;
import com.paradacerta.api.model.OperadorEstacionamento;
import com.paradacerta.api.model.VagasEstacionamento;
import com.paradacerta.api.repository.EstacionamentoRepository;
import com.paradacerta.api.repository.OperadorEstacionamentoRepository;
import com.paradacerta.api.repository.SessaoRepository;
import com.paradacerta.api.repository.VagasEstacionamentoRepository;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KioskAdmServiceTest {

    private final OperadorEstacionamentoRepository operadorRepository = mock(OperadorEstacionamentoRepository.class);
    private final EstacionamentoRepository estacionamentoRepository = mock(EstacionamentoRepository.class);
    private final VagasEstacionamentoRepository vagasRepository = mock(VagasEstacionamentoRepository.class);
    private final SessaoRepository sessaoRepository = mock(SessaoRepository.class);
    private final KioskAdmService service = new KioskAdmService(
            operadorRepository,
            estacionamentoRepository,
            vagasRepository,
            sessaoRepository
    );

    @Test
    void loginBuscaOperadorPorEstacionamentoEUsuario() {
        OperadorEstacionamento operador = new OperadorEstacionamento();
        operador.setId(12);
        operador.setEstacionamentoId(7);
        operador.setNome("Operador Teste");
        operador.setUsuario("caixa");
        operador.setSenhaHash(BCrypt.hashpw("123456", BCrypt.gensalt()));

        Estacionamento estacionamento = new Estacionamento();
        estacionamento.setId(7);
        estacionamento.setNome("Centro Park");

        VagasEstacionamento vagas = new VagasEstacionamento();
        vagas.setEstacionamentoId(7);
        vagas.setQtdVagasTotais(20);
        vagas.setQtdVagasDisponiveis(6);

        KioskAdmDTO.LoginRequest request = new KioskAdmDTO.LoginRequest();
        request.setEstacionamentoId(7);
        request.setUsuario("caixa");
        request.setSenha("123456");

        when(operadorRepository.findByEstacionamentoIdAndUsuarioAndAtivoTrue(7, "caixa"))
                .thenReturn(Optional.of(operador));
        when(estacionamentoRepository.findById(7)).thenReturn(Optional.of(estacionamento));
        when(vagasRepository.findByEstacionamentoId(7)).thenReturn(Optional.of(vagas));

        KioskAdmDTO.LoginResponse response = service.login(request);

        verify(operadorRepository).findByEstacionamentoIdAndUsuarioAndAtivoTrue(7, "caixa");
        assertThat(response.getAdmId()).isEqualTo(12);
        assertThat(response.getEstacionamentoId()).isEqualTo(7);
        assertThat(response.getNomeEstacionamento()).isEqualTo("Centro Park");
    }
}
