package com.paradacerta.api.service;

import com.paradacerta.api.model.Cliente;
import com.paradacerta.api.model.Estacionamento;
import com.paradacerta.api.model.SessaoEstacionamento;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Serviço centralizado de envio de e-mails transacionais da plataforma.
 *
 * <p>Esta classe foi extraída para reuso entre as várias rotinas que precisam
 * disparar notificações por e-mail (recuperação de senha, cancelamento de
 * reserva pelo admin, etc.). Todos os métodos públicos seguem o contrato de
 * "fail-safe": exceções são tratadas e propagadas como {@code false} no retorno
 * — o chamador deve decidir se loga, se reverte ou se ignora.
 *
 * <p><b>LGPD:</b> nenhum log de envio inclui o e-mail completo do destinatário
 * nem informações pessoais (CPF, placa, etc.). Identificamos somente por
 * {@code clienteId} e {@code sessaoId}.
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter FMT_BR =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", new Locale("pt", "BR"));

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remetente;

    /**
     * Envia notificação ao motorista informando que a reserva dele foi cancelada
     * pela administração do estacionamento (painel web).
     *
     * <p>O método é "fail-safe": qualquer falha de SMTP, de template ou de
     * dados inválidos é logada e o retorno indica o resultado. O chamador NÃO
     * deve reverter a transação de cancelamento com base em uma falha aqui.
     *
     * @param cliente        motorista dono da reserva (obrigatório)
     * @param estacionamento estacionamento (obrigatório)
     * @param sessao         reserva já cancelada (obrigatório)
     * @param modeloVeiculo  modelo do veículo associado à placa da sessão (opcional)
     * @return {@code true} se o e-mail foi entregue ao servidor SMTP; {@code false} caso contrário
     */
    public boolean enviarEmailReservaCancelada(Cliente cliente,
                                               Estacionamento estacionamento,
                                               SessaoEstacionamento sessao,
                                               String modeloVeiculo) {
        if (cliente == null || cliente.getEmail() == null || cliente.getEmail().isBlank()) {
            log.warn("E-mail de cancelamento não enviado: cliente sem e-mail (sessaoId={})",
                    sessao != null ? sessao.getId() : null);
            return false;
        }
        if (estacionamento == null) {
            log.warn("E-mail de cancelamento não enviado: estacionamento ausente (sessaoId={})",
                    sessao != null ? sessao.getId() : null);
            return false;
        }

        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

            helper.setFrom(new InternetAddress(remetente, "Parada Certa", "UTF-8"));
            helper.setTo(cliente.getEmail());
            helper.setSubject("Sua reserva foi cancelada");
            helper.setText(construirHtmlReservaCancelada(cliente, estacionamento, sessao, modeloVeiculo), true);

            mailSender.send(msg);
            log.info("E-mail de cancelamento de reserva enviado (clienteId={}, sessaoId={})",
                    cliente.getId(), sessao != null ? sessao.getId() : null);
            return true;
        } catch (MessagingException | UnsupportedEncodingException | MailException e) {
            // Não logamos o e-mail nem outros dados pessoais — apenas o tipo da falha.
            log.error("Falha ao enviar e-mail de cancelamento de reserva (clienteId={}, sessaoId={}): {}",
                    cliente.getId(),
                    sessao != null ? sessao.getId() : null,
                    e.getClass().getSimpleName());
            return false;
        } catch (Exception e) {
            // Garantia extra: qualquer erro inesperado vira false. Nunca relança
            // para o caller — o cancelamento NÃO pode ser revertido por causa de e-mail.
            log.error("Erro inesperado ao montar e-mail de cancelamento (clienteId={}, sessaoId={}): {}",
                    cliente.getId(),
                    sessao != null ? sessao.getId() : null,
                    e.getClass().getSimpleName());
            return false;
        }
    }

    // ── Template HTML ────────────────────────────────────────────────────────

    private String construirHtmlReservaCancelada(Cliente cliente,
                                                 Estacionamento estacionamento,
                                                 SessaoEstacionamento sessao,
                                                 String modeloVeiculo) {
        String nomeMotorista   = safe(cliente.getNome(), "Motorista");
        String nomeEstac       = safe(estacionamento.getNome(), "—");
        String enderecoEstac   = safe(estacionamento.getEndereco(), "—");
        String dataHoraReserva = sessao != null && sessao.getHoraEntrada() != null
                ? sessao.getHoraEntrada().format(FMT_BR)
                : null;
        String veiculo         = montarVeiculo(modeloVeiculo, sessao != null ? sessao.getPlaca() : null);

        StringBuilder detalhes = new StringBuilder();
        detalhes.append(linhaDetalhe("Estacionamento", nomeEstac));
        if (!"—".equals(enderecoEstac)) {
            detalhes.append(linhaDetalhe("Endereço", enderecoEstac));
        }
        if (dataHoraReserva != null) {
            detalhes.append(linhaDetalhe("Data/Horário da reserva", dataHoraReserva));
        }
        if (veiculo != null) {
            detalhes.append(linhaDetalhe("Veículo", veiculo));
        }
        detalhes.append(linhaDetalhe("Status", "Cancelada"));

        return """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head><meta charset="UTF-8"></head>
            <body style="font-family:Arial,sans-serif;background:#f5f5f5;padding:0;margin:0;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f5f5f5;padding:40px 0;">
                <tr><td align="center">
                  <table width="520" cellpadding="0" cellspacing="0"
                         style="background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.1);">
                    <tr>
                      <td style="background:#1B5E20;padding:28px 40px;text-align:center;">
                        <h1 style="color:#ffffff;margin:0;font-size:22px;letter-spacing:1px;">Parada Certa</h1>
                        <p style="color:#A5D6A7;margin:6px 0 0;font-size:13px;">Seu estacionamento inteligente</p>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:36px 40px 12px;">
                        <p style="color:#333;font-size:16px;margin:0 0 16px;">Olá, <strong>%s</strong>.</p>
                        <p style="color:#555;font-size:15px;line-height:1.5;margin:0 0 20px;">
                          Informamos que sua reserva no estacionamento
                          <strong>%s</strong> foi <strong>cancelada pela administração do estabelecimento</strong>.
                        </p>
                        <h3 style="color:#1B5E20;font-size:15px;margin:24px 0 12px;">Detalhes da reserva</h3>
                        <table cellpadding="0" cellspacing="0" width="100%%" style="border-collapse:collapse;">
                          %s
                        </table>
                        <p style="color:#555;font-size:14px;line-height:1.5;margin:24px 0 8px;">
                          Caso tenha dúvidas, entre em contato com o estacionamento ou realize uma nova reserva
                          pelo aplicativo Parada Certa.
                        </p>
                        <p style="color:#777;font-size:14px;margin:24px 0 0;">
                          Atenciosamente,<br>
                          <strong>Equipe Parada Certa</strong>
                        </p>
                      </td>
                    </tr>
                    <tr>
                      <td style="background:#f9f9f9;padding:18px 40px;text-align:center;">
                        <p style="color:#bbb;font-size:11px;margin:0;">
                          © 2026 Parada Certa · Todos os direitos reservados
                        </p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(escape(nomeMotorista), escape(nomeEstac), detalhes.toString());
    }

    private String linhaDetalhe(String rotulo, String valor) {
        return """
            <tr>
              <td style="padding:6px 0;color:#888;font-size:13px;width:42%%;">%s</td>
              <td style="padding:6px 0;color:#333;font-size:14px;font-weight:600;">%s</td>
            </tr>
            """.formatted(escape(rotulo), escape(valor));
    }

    private String montarVeiculo(String modelo, String placa) {
        boolean temModelo = modelo != null && !modelo.isBlank();
        boolean temPlaca  = placa  != null && !placa.isBlank();
        if (!temModelo && !temPlaca) return null;
        if (temModelo && temPlaca)   return modelo + " — " + placa;
        return temModelo ? modelo : placa;
    }

    private static String safe(String s, String fallback) {
        return (s == null || s.isBlank()) ? fallback : s;
    }

    /** Escape mínimo para evitar HTML injection nos campos textuais. */
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    // Reservado para evolução: outros templates futuros podem reaproveitar este
    // padrão de método público + construtor de HTML + fail-safe interno.
    @SuppressWarnings("unused")
    private static LocalDateTime now() { return LocalDateTime.now(); }
}
