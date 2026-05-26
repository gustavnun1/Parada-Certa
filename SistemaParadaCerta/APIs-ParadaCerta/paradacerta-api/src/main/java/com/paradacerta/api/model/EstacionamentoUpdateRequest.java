package com.paradacerta.api.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Atualização parcial de estacionamento (PUT /api/estacionamentos/{id}).
 * Campos null permanecem como estão no banco.
 */
@Data
public class EstacionamentoUpdateRequest {

    @Size(max = 100)
    private String nome;

    @Pattern(regexp = "^\\d{14}$", message = "CNPJ deve ter 14 digitos numericos")
    private String cnpj;

    @Size(max = 200)
    private String razaoSocial;

    @Size(max = 200)
    private String nomeFantasia;

    @Size(max = 300)
    private String endereco;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal precoHora;

    private LocalTime horarioAbertura;
    private LocalTime horarioFechamento;

    @Size(max = 1000)
    private String descricao;

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
