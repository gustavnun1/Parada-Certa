package com.paradacerta.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserCompleteData {
    private Cliente cliente;
    private Veiculo veiculo;
    private Endereco endereco;
}