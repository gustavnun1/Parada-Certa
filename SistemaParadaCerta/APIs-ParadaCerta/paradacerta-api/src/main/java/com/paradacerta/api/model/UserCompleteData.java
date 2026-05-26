package com.paradacerta.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class UserCompleteData {
    private Cliente cliente;
    private List<Veiculo> veiculos;
    private Endereco endereco;
}