package com.paradacerta.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Veiculo")
public class Veiculo {

    @Id
    @Column(name = "placa", length = 7, nullable = false)
    @NotBlank(message = "Placa é obrigatória")
    private String placa;

    @Column(name = "nome", nullable = false)
    @NotBlank(message = "Modelo é obrigatório")
    private String nome;

    @Column(name = "cor", nullable = false)
    @NotBlank(message = "Cor é obrigatória")
    private String cor;

    @Column(name = "responsavel", nullable = false)
    @NotBlank(message = "Responsável é obrigatório")
    private String responsavel;
}
