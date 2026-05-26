package com.paradacerta.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CancelarSessaoRequest {

    @NotNull
    private Integer adminId;

    @NotBlank
    private String senhaAdmin;
}
