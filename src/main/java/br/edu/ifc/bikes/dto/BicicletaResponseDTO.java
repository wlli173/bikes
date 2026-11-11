package br.edu.ifc.bikes.dto;

public record BicicletaResponseDTO(
        Long id,
        String codigo,
        String modelo,
        String status
) {
}
