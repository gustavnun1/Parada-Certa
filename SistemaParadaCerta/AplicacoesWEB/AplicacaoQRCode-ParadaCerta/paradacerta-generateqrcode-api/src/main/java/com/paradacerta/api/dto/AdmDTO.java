package com.paradacerta.api.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public class AdmDTO {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class LoginRequest {
        private Integer estacionamentoId;
        private String usuario;
        private String senha;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LoginResponse {
        private Integer admId;
        private String nomeCompleto;
        private Integer estacionamentoId;
        private String nomeEstacionamento;
        private Integer vagasDisponiveis;
        private Integer vagasTotais;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class GerarQrCodeRequest {
        private Integer admId;
        private Integer estacionamentoId;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class GerarQrCodeResponse {
        private String token;
        private Integer estacionamentoId;
        private String nomeEstacionamento;
        private String geradoEm;
        private String expiradoEm;
        private String qrCodePayload;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class StatusResponse {
        private Integer vagasDisponiveis;
        private Integer vagasTotais;
        private Long sessoesAtivas;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class AtualizarEstacionamentoRequest {
        private Integer adminId;
        private String nome;
        private String endereco;
        private BigDecimal precoHora;
        private LocalTime horarioAbertura;
        private LocalTime horarioFechamento;
        private String descricao;
        private Boolean ativo;
        private Boolean permiteReserva;
        private String pixKey;
        private String cep;
        private String logradouro;
        private String numero;
        private String complemento;
        private String bairro;
        private String cidade;
        private String uf;

        // Mantidos para compatibilidade de JSON, mas ignorados na edicao comum.
        private BigDecimal latitude;
        private BigDecimal longitude;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class AtualizarVagasRequest {
        private Integer adminId;
        private Integer qtdVagasTotais;
        private Integer qtdVagasReservaveis;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class EstacionamentoResponse {
        private Integer id;
        private String nome;
        private BigDecimal avaliacaoMedia;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String endereco;
        private BigDecimal precoHora;
        private LocalTime horarioAbertura;
        private LocalTime horarioFechamento;
        private String fotoPrincipal;
        private String descricao;
        private Boolean ativo;
        private String pixKey;
        private Boolean permiteReserva;
        private String cep;
        private String logradouro;
        private String numero;
        private String complemento;
        private String bairro;
        private String cidade;
        private String uf;
        private Integer qtdVagasTotais;
        private Integer qtdVagasDisponiveis;
        private Integer qtdVagasReservaveis;
        private Integer qtdVagasReservadas;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ExclusaoContaResponse {
        private Integer adminId;
        private List<Integer> estacionamentosExcluidos;
        private Integer operadoresExcluidos;
    }
}
