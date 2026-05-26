package com.paradacerta.api.service;

import com.paradacerta.api.dto.AdmDTO;
import com.paradacerta.api.exception.CredenciaisInvalidasException;
import com.paradacerta.api.exception.RequisicaoInvalidaException;
import com.paradacerta.api.model.Estacionamento;
import com.paradacerta.api.model.OperadorEstacionamento;
import com.paradacerta.api.model.QrCodeEntrada;
import com.paradacerta.api.model.SessaoEstacionamento;
import com.paradacerta.api.model.VagasEstacionamento;
import com.paradacerta.api.repository.EstacionamentoRepository;
import com.paradacerta.api.repository.OperadorEstacionamentoRepository;
import com.paradacerta.api.repository.QrCodeEntradaRepository;
import com.paradacerta.api.repository.SessaoEstacionamentoRepository;
import com.paradacerta.api.repository.VagasEstacionamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdmEstacionamentoServiceTest {

    @Mock
    private OperadorEstacionamentoRepository operadorRepository;
    @Mock
    private EstacionamentoRepository estacionamentoRepository;
    @Mock
    private VagasEstacionamentoRepository vagasRepository;
    @Mock
    private QrCodeEntradaRepository qrCodeRepository;
    @Mock
    private SessaoEstacionamentoRepository sessaoRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;

    private AdmEstacionamentoService service;

    @BeforeEach
    void setUp() {
        service = new AdmEstacionamentoService(
                operadorRepository,
                estacionamentoRepository,
                vagasRepository,
                qrCodeRepository,
                sessaoRepository,
                jdbcTemplate
        );
    }

    @Test
    void loginRetornaDadosDoOperadorEstacionamentoEVagas() {
        OperadorEstacionamento operador = operadorAtivo(11, 5);
        operador.setSenhaHash(BCrypt.hashpw("123456", BCrypt.gensalt()));
        when(operadorRepository.findByUsuarioAndAtivoTrue("admin")).thenReturn(Optional.of(operador));
        when(estacionamentoRepository.findById(5)).thenReturn(Optional.of(estacionamento(5)));
        when(vagasRepository.findByEstacionamentoId(5)).thenReturn(Optional.of(vagas(5, 20, 8)));

        AdmDTO.LoginResponse response = service.login(new AdmDTO.LoginRequest("admin", "123456"));

        assertThat(response.getAdmId()).isEqualTo(11);
        assertThat(response.getNomeCompleto()).isEqualTo("Operador Teste");
        assertThat(response.getEstacionamentoId()).isEqualTo(5);
        assertThat(response.getNomeEstacionamento()).isEqualTo("Centro Park");
        assertThat(response.getVagasTotais()).isEqualTo(20);
        assertThat(response.getVagasDisponiveis()).isEqualTo(8);
    }

    @Test
    void loginRejeitaSenhaIncorreta() {
        OperadorEstacionamento operador = operadorAtivo(11, 5);
        operador.setSenhaHash(BCrypt.hashpw("correta", BCrypt.gensalt()));
        when(operadorRepository.findByUsuarioAndAtivoTrue("admin")).thenReturn(Optional.of(operador));

        assertThatThrownBy(() -> service.login(new AdmDTO.LoginRequest("admin", "errada")))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("Usuario ou senha invalidos");

        verifyNoInteractions(estacionamentoRepository, vagasRepository);
    }

    @Test
    void gerarQrCodeCriaTokenSessaoAtivaEPayloadParaEntrada() {
        when(operadorRepository.findById(11)).thenReturn(Optional.of(operadorAtivo(11, 5)));
        when(estacionamentoRepository.findById(5)).thenReturn(Optional.of(estacionamento(5)));
        when(vagasRepository.findByEstacionamentoId(5)).thenReturn(Optional.of(vagas(5, 20, 2)));

        AdmDTO.GerarQrCodeResponse response = service.gerarQrCode(new AdmDTO.GerarQrCodeRequest(11, 5));

        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getEstacionamentoId()).isEqualTo(5);
        assertThat(response.getNomeEstacionamento()).isEqualTo("Centro Park");
        assertThat(response.getQrCodePayload())
                .contains("\"app\":\"paradacerta\"")
                .contains("\"type\":\"entrada\"")
                .contains("\"estacionamentoId\":5")
                .contains("\"token\":\"" + response.getToken() + "\"");

        verify(qrCodeRepository).invalidarTokensDisponiveis(5);

        ArgumentCaptor<QrCodeEntrada> qrCaptor = ArgumentCaptor.forClass(QrCodeEntrada.class);
        verify(qrCodeRepository).save(qrCaptor.capture());
        assertThat(qrCaptor.getValue().getStatus()).isEqualTo("UTILIZADO");
        assertThat(qrCaptor.getValue().getToken()).isEqualTo(response.getToken());

        ArgumentCaptor<SessaoEstacionamento> sessaoCaptor = ArgumentCaptor.forClass(SessaoEstacionamento.class);
        verify(sessaoRepository).save(sessaoCaptor.capture());
        assertThat(sessaoCaptor.getValue().getStatus()).isEqualTo(SessaoEstacionamento.STATUS_ATIVA);
        assertThat(sessaoCaptor.getValue().getQrCode()).isEqualTo(response.getToken());
        assertThat(sessaoCaptor.getValue().getValorPago()).isEqualByComparingTo("12.50");
    }

    @Test
    void gerarQrCodeBloqueiaOperadorDeOutroEstacionamento() {
        when(operadorRepository.findById(11)).thenReturn(Optional.of(operadorAtivo(11, 5)));

        assertThatThrownBy(() -> service.gerarQrCode(new AdmDTO.GerarQrCodeRequest(11, 99)))
                .isInstanceOf(RequisicaoInvalidaException.class)
                .hasMessage("Operador nao pertence a este estacionamento");

        verifyNoInteractions(qrCodeRepository, sessaoRepository);
    }

    @Test
    void gerarQrCodeBloqueiaEstacionamentoLotado() {
        when(operadorRepository.findById(11)).thenReturn(Optional.of(operadorAtivo(11, 5)));
        when(estacionamentoRepository.findById(5)).thenReturn(Optional.of(estacionamento(5)));
        when(vagasRepository.findByEstacionamentoId(5)).thenReturn(Optional.of(vagas(5, 20, 0)));

        assertThatThrownBy(() -> service.gerarQrCode(new AdmDTO.GerarQrCodeRequest(11, 5)))
                .isInstanceOf(RequisicaoInvalidaException.class)
                .hasMessage("Estacionamento lotado - nenhuma vaga disponivel");

        verifyNoInteractions(qrCodeRepository, sessaoRepository);
    }

    private static OperadorEstacionamento operadorAtivo(Integer id, Integer estacionamentoId) {
        return OperadorEstacionamento.builder()
                .id(id)
                .estacionamentoId(estacionamentoId)
                .nome("Operador Teste")
                .usuario("admin")
                .senhaHash(BCrypt.hashpw("123456", BCrypt.gensalt()))
                .ativo(true)
                .build();
    }

    private static Estacionamento estacionamento(Integer id) {
        return Estacionamento.builder()
                .id(id)
                .nome("Centro Park")
                .precoHora(new BigDecimal("12.50"))
                .ativo(true)
                .permiteReserva(true)
                .build();
    }

    private static VagasEstacionamento vagas(Integer estacionamentoId, Integer totais, Integer disponiveis) {
        return VagasEstacionamento.builder()
                .estacionamentoId(estacionamentoId)
                .qtdVagasTotais(totais)
                .qtdVagasDisponiveis(disponiveis)
                .qtdVagasReservaveis(5)
                .qtdVagasReservadas(1)
                .build();
    }
}
