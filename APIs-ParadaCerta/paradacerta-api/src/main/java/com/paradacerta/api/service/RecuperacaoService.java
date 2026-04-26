package com.paradacerta.api.service;

import com.paradacerta.api.exception.RequisicaoInvalidaException;
import com.paradacerta.api.exception.UsuarioNaoEncontradoException;
import com.paradacerta.api.model.*;
import com.paradacerta.api.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RecuperacaoService {

    private final ClienteRepository clienteRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remetente;

    // Armazenamento em memória: cpf → entrada com código e expiração (15 min)
    private final ConcurrentHashMap<String, CodigoEntry> codigos = new ConcurrentHashMap<>();

    private static final SecureRandom random = new SecureRandom();

    // ── Solicitar código ──────────────────────────────────────────────────────

    public ApiResponse solicitarCodigo(SolicitarRecuperacaoRequest req) {
        Cliente cliente = buscarCliente(req.getLogin(), req.isCpf());

        String codigo = String.format("%06d", random.nextInt(1_000_000));
        LocalDateTime expiracao = LocalDateTime.now().plusMinutes(15);

        codigos.put(cliente.getCpf(), new CodigoEntry(codigo, expiracao, cliente.getEmail()));

        try {
            enviarEmail(cliente.getEmail(), cliente.getNome(), codigo);
        } catch (MessagingException | java.io.UnsupportedEncodingException | MailException e) {
            throw new RequisicaoInvalidaException("Falha ao enviar e-mail: " + e.getMessage());
        }

        // Mascara o e-mail na resposta (ex: gus***@gmail.com)
        String emailMascarado = mascararEmail(cliente.getEmail());
        return ApiResponse.ok("Código enviado para " + emailMascarado + ". Válido por 15 minutos.");
    }

    // ── Confirmar código e redefinir senha ────────────────────────────────────

    @Transactional
    public ApiResponse confirmarCodigo(ConfirmarRecuperacaoRequest req) {
        Cliente cliente = buscarCliente(req.getLogin(), req.isCpf());

        CodigoEntry entrada = codigos.get(cliente.getCpf());
        if (entrada == null) {
            throw new RequisicaoInvalidaException("Nenhum código solicitado para este usuário. Solicite novamente.");
        }
        if (LocalDateTime.now().isAfter(entrada.expiracao())) {
            codigos.remove(cliente.getCpf());
            throw new RequisicaoInvalidaException("Código expirado. Solicite um novo código.");
        }
        if (!entrada.codigo().equals(req.getCodigo())) {
            throw new RequisicaoInvalidaException("Código inválido. Verifique o e-mail e tente novamente.");
        }

        cliente.setSenha(BCrypt.hashpw(req.getNovaSenha(), BCrypt.gensalt()));
        clienteRepository.save(cliente);
        codigos.remove(cliente.getCpf());

        return ApiResponse.ok("Senha redefinida com sucesso. Faça login com a nova senha.");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Cliente buscarCliente(String login, boolean isCpf) {
        // Auto-detecta pelo formato: 11 dígitos = CPF, caso contrário = e-mail.
        // Isso evita dependência da flag booleana enviada pelo app, que pode sofrer
        // problema de desserialização Jackson/Lombok com campos boolean primitivos.
        boolean looksLikeCpf = login != null && login.matches("\\d{11}");
        return looksLikeCpf
                ? clienteRepository.findByCpf(login)
                        .orElseThrow(() -> new UsuarioNaoEncontradoException("Nenhuma conta encontrada com este CPF"))
                : clienteRepository.findByEmail(login)
                        .orElseThrow(() -> new UsuarioNaoEncontradoException("Nenhuma conta encontrada com este e-mail"));
    }

    private void enviarEmail(String destinatario, String nome, String codigo)
            throws MessagingException, java.io.UnsupportedEncodingException {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

        helper.setFrom(new jakarta.mail.internet.InternetAddress(remetente, "Parada Certa", "UTF-8"));
        helper.setTo(destinatario);
        helper.setSubject("Seu código de redefinição de senha — Parada Certa");
        helper.setText(construirHtml(nome, codigo), true);

        mailSender.send(msg);
    }

    private String construirHtml(String nome, String codigo) {
        return """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head><meta charset="UTF-8"></head>
            <body style="font-family:Arial,sans-serif;background:#f5f5f5;padding:0;margin:0;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f5f5f5;padding:40px 0;">
                <tr><td align="center">
                  <table width="480" cellpadding="0" cellspacing="0"
                         style="background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.1);">
                    <!-- Header -->
                    <tr>
                      <td style="background:#1B5E20;padding:32px 40px;text-align:center;">
                        <h1 style="color:#ffffff;margin:0;font-size:24px;letter-spacing:1px;">Parada Certa</h1>
                        <p style="color:#A5D6A7;margin:8px 0 0;font-size:14px;">Seu estacionamento inteligente</p>
                      </td>
                    </tr>
                    <!-- Body -->
                    <tr>
                      <td style="padding:40px;">
                        <p style="color:#333;font-size:16px;margin:0 0 16px;">Olá, <strong>%s</strong>!</p>
                        <p style="color:#555;font-size:15px;margin:0 0 24px;">
                          Recebemos uma solicitação para redefinir a senha da sua conta no <strong>Parada Certa</strong>.
                          Use o código abaixo no aplicativo:
                        </p>
                        <!-- Código -->
                        <div style="text-align:center;margin:32px 0;">
                          <span style="display:inline-block;background:#E8F5E9;border:2px dashed #1B5E20;
                                       border-radius:12px;padding:20px 48px;font-size:42px;font-weight:bold;
                                       color:#1B5E20;letter-spacing:12px;">%s</span>
                        </div>
                        <p style="color:#888;font-size:13px;text-align:center;margin:0 0 24px;">
                          ⏱ Este código expira em <strong>15 minutos</strong>.
                        </p>
                        <hr style="border:none;border-top:1px solid #eee;margin:24px 0;">
                        <p style="color:#aaa;font-size:12px;margin:0;">
                          Se você não solicitou a redefinição de senha, ignore este e-mail.
                          Sua conta permanece segura.
                        </p>
                      </td>
                    </tr>
                    <!-- Footer -->
                    <tr>
                      <td style="background:#f9f9f9;padding:20px 40px;text-align:center;">
                        <p style="color:#bbb;font-size:11px;margin:0;">
                          © 2024 Parada Certa · Todos os direitos reservados
                        </p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(nome, codigo);
    }

    private String mascararEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 2) return email;
        String local = email.substring(0, at);
        String dominio = email.substring(at);
        return local.charAt(0) + local.substring(1, Math.min(4, local.length())).replaceAll(".", "*") + dominio;
    }

    // ── Registro interno ──────────────────────────────────────────────────────

    private record CodigoEntry(String codigo, LocalDateTime expiracao, String email) {}
}
