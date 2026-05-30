package com.paradacerta.api.model;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

/** Criação de novo estacionamento (POST /api/estacionamentos). */
@Data
public class EstacionamentoRequest {

    @NotNull(message = "Administrador responsavel e obrigatorio")
    private Integer adminId;

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100)
    private String nome;

    @NotBlank(message = "CNPJ e obrigatorio")
    @Pattern(regexp = "^\\d{14}$", message = "CNPJ deve ter 14 digitos numericos")
    private String cnpj;

    @NotBlank(message = "Razao social e obrigatoria")
    @Size(max = 200)
    private String razaoSocial;

    @Size(max = 200)
    private String nomeFantasia;

    /**
     * Endereço em texto livre. Quando os campos detalhados (logradouro/numero/bairro/cidade/uf)
     * forem informados, o service preenche esse campo automaticamente — manter para compatibilidade.
     */
    @Size(max = 300)
    private String endereco;

    @NotNull(message = "Preço por hora é obrigatório")
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal precoHora;

    @NotNull(message = "Quantidade de vagas totais é obrigatória")
    @Min(value = 1, message = "Deve ter ao menos 1 vaga")
    private Integer qtdVagasTotais;

    /** Padrão = 0 se não enviado. */
    private Integer qtdVagasReservaveis;

    private LocalTime horarioAbertura;
    private LocalTime horarioFechamento;

    @Size(max = 1000)
    private String descricao;

    /** Padrão 0.0 se não enviado (cobertura SP exige idealmente, mas TCC aceita). */
    private BigDecimal latitude;
    private BigDecimal longitude;

    private Boolean ativo;
    private Boolean permiteReserva;

    @Size(max = 200)
    private String pixKey;

    // ── Endereço detalhado ────────────────────────────────────────────────────
    @Pattern(regexp = "^\\d{8}$", message = "CEP deve ter 8 dígitos numéricos")
    private String cep;

    @Size(max = 200)
    private String logradouro;

    @Size(max = 10)
    private String numero;

    @Size(max = 100)
    private String complemento;

    @Size(max = 100)
    private String bairro;

    @Size(max = 100)
    private String cidade;

    @Size(max = 2, min = 2, message = "UF deve ter 2 letras")
    private String uf;
}
