package br.edu.ifc.bikes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CartaoCreditoDTO(
        @NotBlank(message = "O número do cartão é obrigatório")
        @Pattern(regexp = "\\d{13,19}", message = "O número do cartão deve conter entre 13 e 19 dígitos")
        String numero,
        @NotBlank(message = "O nome do titular é obrigatório")
        @Size(min = 2, max = 100, message = "O nome do titular deve ter entre 2 e 100 caracteres")
        String titular,
        @NotBlank(message = "A validade é obrigatória")
        @Pattern(regexp = "(0[1-9]|1[0-2])/\\d{2}", message = "A validade deve estar no formato MM/AA")
        String validade,
        @NotBlank(message = "O código de segurança é obrigatório")
        @Pattern(regexp = "\\d{3,4}", message = "O código de segurança deve conter 3 ou 4 dígitos")
        String cvv
) {
}
