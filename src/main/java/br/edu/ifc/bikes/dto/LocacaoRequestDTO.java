package br.edu.ifc.bikes.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

//codigoBicicleta é opcional: quando omitido o sistema escolhe a primeira bicicleta disponível
public record LocacaoRequestDTO(
        @Size(min = 2, max = 20, message = "O código deve ter entre 2 e 20 caracteres")
        String codigoBicicleta,
        @NotNull(message = "A data prevista para devolução é obrigatória")
        @Future(message = "A data prevista para devolução deve ser futura")
        LocalDateTime dataPrevistaDevolucao,
        @NotNull(message = "Os dados do cartão de crédito são obrigatórios")
        @Valid
        CartaoCreditoDTO cartao
) {
}
