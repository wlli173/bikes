package br.edu.ifc.bikes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BicicletaRequestDTO(
        @NotBlank(message = "O código é obrigatório")
        @Size(min = 2, max = 20, message = "O código deve ter entre 2 e 20 caracteres")
        String codigo,
        @NotBlank(message = "O modelo é obrigatório")
        @Size(min = 2, max = 100, message = "O modelo deve ter entre 2 e 100 caracteres")
        String modelo,
        @NotBlank(message = "O status é obrigatório")
        @Pattern(regexp = "DISPONIVEL|ALUGADA|MANUTENCAO", message = "O status deve ser DISPONIVEL, ALUGADA ou MANUTENCAO")
        String status
) {
}
