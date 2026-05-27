package com.paradacerta.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class KioskAdmDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        private String usuario;
        private String senha;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LoginResponse {
        private Integer admId;
        private String nomeCompleto;
        private Integer estacionamentoId;
        private String nomeEstacionamento;
        private Integer vagasDisponiveis;
        private Integer vagasTotais;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GerarQrCodeRequest {
        private Integer admId;
        private Integer estacionamentoId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GerarQrCodeResponse {
        private String token;
        private Integer estacionamentoId;
        private String nomeEstacionamento;
        private String geradoEm;
        private String expiradoEm;
        private String qrCodePayload;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusResponse {
        private Integer vagasDisponiveis;
        private Integer vagasTotais;
        private Long sessoesAtivas;
    }
}
