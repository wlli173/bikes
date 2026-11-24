package br.edu.ifc.bikes.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LocacaoResponseDTO(
        Long id,
        Long clienteId,
        String nomeCliente,
        String codigoBicicleta,
        LocalDateTime dataRetirada,
        LocalDateTime dataPrevistaDevolucao,
        LocalDateTime dataDevolucao,
        String cartaoFinal,
        BigDecimal valor,
        String status,
        String observacao
) {
}
