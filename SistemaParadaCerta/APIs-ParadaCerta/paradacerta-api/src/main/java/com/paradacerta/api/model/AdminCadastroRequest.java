package com.paradacerta.api.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Pedido completo de cadastro vindo do formulario web:
 * cria o estacionamento, sua linha de VagasEstacionamento, o admin responsavel
 * e o operador inicial do kiosk.
 */
@Data
public class AdminCadastroRequest {

    @Valid
    @NotNull(message = "Dados do responsavel sao obrigatorios")
    private Responsavel responsavel;

    @Valid
    @NotNull(message = "Dados do estacionamento sao obrigatorios")
    private EstacionamentoDados estacionamento;

    @Valid
    @NotNull(message = "Operador inicial e obrigatorio")
    private OperadorInicial operadorInicial;

    @Data
    public static class OperadorInicial {
        @NotBlank(message = "Nome do operador e obrigatorio")
        @Size(min = 3, max = 80, message = "Nome deve ter entre 3 e 80 caracteres")
        @Pattern(regexp = "^[A-Za-zÀ-ÖØ-öø-ÿ' -]+$",
                message = "Nome inválido. O nome não pode conter números nem caracteres especiais.")
        private String nome;

        @NotBlank(message = "Usuario do operador e obrigatorio")
        @Size(min = 3, max = 50)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$",
                message = "Usuario do operador aceita apenas letras, numeros, ponto, hifen e underline")
        private String usuario;

        @NotBlank(message = "Senha do operador e obrigatoria")
        @Size(min = 6, max = 100, message = "Senha do operador deve ter ao menos 6 caracteres")
        private String senha;

        @NotBlank(message = "CPF do operador e obrigatorio")
        @Pattern(regexp = "^\\d{11}$",
                message = "CPF do operador deve ter 11 digitos numericos")
        private String cpf;

        @NotBlank(message = "E-mail do operador e obrigatorio")
        @Email(message = "E-mail do operador invalido")
        @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "E-mail do operador invalido"
        )
        @Size(max = 200)
        private String email;

        @NotBlank(message = "Telefone do operador e obrigatorio")
        @Size(min = 8, max = 20)
        private String telefone;

        @NotBlank(message = "CEP do operador e obrigatorio")
        @Pattern(regexp = "^\\d{8}$", message = "CEP do operador deve ter 8 digitos numericos")
        private String cep;

        @NotBlank(message = "Logradouro do operador e obrigatorio")
        @Size(max = 200)
        private String logradouro;

        @NotBlank(message = "Numero do operador e obrigatorio")
        @Size(max = 10)
        private String numero;

        @Size(max = 100)
        private String complemento;

        @NotBlank(message = "Bairro do operador e obrigatorio")
        @Size(max = 100)
        private String bairro;

        @NotBlank(message = "Cidade do operador e obrigatoria")
        @Size(max = 100)
        private String cidade;

        @NotBlank(message = "UF do operador e obrigatoria")
        @Size(min = 2, max = 2, message = "UF do operador deve ter 2 letras")
        private String uf;
    }

    @Data
    public static class Responsavel {
        @NotBlank(message = "Nome do responsavel e obrigatorio")
        @Size(min = 3, max = 80, message = "Nome deve ter entre 3 e 80 caracteres")
        @Pattern(regexp = "^[A-Za-zÀ-ÖØ-öø-ÿ' -]+$",
                message = "Nome inválido. O nome não pode conter números nem caracteres especiais.")
        private String nome;

        @NotBlank(message = "E-mail do responsavel e obrigatorio")
        @Email(message = "E-mail invalido")
        @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "E-mail invalido"
        )
        @Size(max = 200)
        private String email;

        @NotBlank(message = "Telefone e obrigatorio")
        @Size(min = 8, max = 20, message = "Telefone deve ter ao menos 8 digitos")
        private String telefone;

        @NotBlank(message = "CPF do responsavel e obrigatorio")
        @Pattern(regexp = "^\\d{11}$", message = "CPF do responsavel deve ter 11 digitos numericos")
        private String cpf;

        private LocalDate dataNascimento;

        /** Usado como login alternativo (usuario UNIQUE). Se vazio, e derivado do email. */
        @Size(max = 50)
        private String usuario;

        @NotBlank(message = "Senha e obrigatoria")
        @Size(min = 6, max = 100, message = "Senha deve ter ao menos 6 caracteres")
        private String senha;
    }

    @Data
    public static class EstacionamentoDados {
        @NotBlank(message = "Nome do estacionamento e obrigatorio")
        @Size(max = 100)
        private String nome;

        @NotBlank(message = "CNPJ do estacionamento e obrigatorio")
        @Pattern(regexp = "^\\d{14}$", message = "CNPJ deve ter 14 digitos numericos")
        private String cnpj;

        @NotBlank(message = "Razao social e obrigatoria")
        @Size(max = 200)
        private String razaoSocial;

        @Size(max = 200)
        private String nomeFantasia;

        /**
         * Texto livre do endereco. Se vazio, e derivado dos campos detalhados
         * (logradouro/numero/bairro/cidade/UF) pelo service.
         */
        @Size(max = 300)
        private String endereco;

        @NotNull(message = "Quantidade de vagas totais e obrigatoria")
        @Min(value = 1, message = "Deve ter ao menos 1 vaga")
        private Integer qtdVagasTotais;

        private Integer qtdVagasReservaveis;

        @NotNull(message = "Preco por hora e obrigatorio")
        @DecimalMin(value = "0.0", inclusive = true, message = "Preco deve ser >= 0")
        private BigDecimal precoHora;

        @NotNull(message = "Horario de abertura e obrigatorio")
        private LocalTime horarioAbertura;

        @NotNull(message = "Horario de fechamento e obrigatorio")
        private LocalTime horarioFechamento;

        @Size(max = 1000)
        private String descricao;

        /** Coordenadas opcionais; se ausentes, ficam 0.0/0.0. */
        private BigDecimal latitude;
        private BigDecimal longitude;

        private Boolean permiteReserva;

        @Size(max = 200)
        private String pixKey;

        @Pattern(regexp = "^\\d{8}$", message = "CEP deve ter 8 digitos numericos")
        private String cep;

        @NotBlank(message = "Logradouro do estacionamento e obrigatorio")
        @Size(max = 200)
        private String logradouro;

        @NotBlank(message = "Numero do estacionamento e obrigatorio")
        @Size(max = 10)
        private String numero;

        @Size(max = 100)
        private String complemento;

        @NotBlank(message = "Bairro do estacionamento e obrigatorio")
        @Size(max = 100)
        private String bairro;

        @NotBlank(message = "Cidade do estacionamento e obrigatoria")
        @Size(max = 100)
        private String cidade;

        @NotBlank(message = "UF do estacionamento e obrigatoria")
        @Size(max = 2, min = 2, message = "UF deve ter 2 letras")
        private String uf;
    }
}
