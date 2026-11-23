package br.edu.ifc.bikes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

//ALUGADA é controlado exclusivamente pelas locações, por isso não pode ser informado manualmente
public record BicicletaStatusDTO(
        @NotBlank(message = "O status é obrigatório")
        @Pattern(regexp = "DISPONIVEL|MANUTENCAO", message = "O status deve ser DISPONIVEL ou MANUTENCAO")
        String status
) {
}
