package br.edu.ifc.bikes.dto;

public record ClienteResponseDTO(
        Long id,
        String nome,
        String cpf
) {
}
