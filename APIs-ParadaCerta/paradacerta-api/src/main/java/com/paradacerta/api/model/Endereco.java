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
@Table(name = "Endereco")
public class Endereco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "cep", nullable = false, length = 8)
    @NotBlank(message = "CEP é obrigatório")
    private String cep;

    @Column(name = "logradouro", nullable = false)
    @NotBlank(message = "Logradouro é obrigatório")
    private String logradouro;

    @Column(name = "numero", nullable = false)
    @NotBlank(message = "Número é obrigatório")
    private String numero;

    @Column(name = "complemento")
    private String complemento;

    @Column(name = "bairro", nullable = false)
    @NotBlank(message = "Bairro é obrigatório")
    private String bairro;

    @Column(name = "cidade", nullable = false)
    @NotBlank(message = "Cidade é obrigatória")
    private String cidade;

    @Column(name = "estado", nullable = false, length = 2)
    @NotBlank(message = "Estado é obrigatório")
    private String estado;

    @Column(name = "clienteId", nullable = false)
    private Long clienteId;
}
