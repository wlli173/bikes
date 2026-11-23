package br.edu.ifc.bikes.dto;

import jakarta.validation.constraints.Size;

//problema=true envia a bicicleta para manutenção em vez de liberá-la
public record DevolucaoRequestDTO(
        boolean problema,
        @Size(max = 255, message = "A descrição do problema deve ter no máximo 255 caracteres")
        String descricaoProblema
) {
}
