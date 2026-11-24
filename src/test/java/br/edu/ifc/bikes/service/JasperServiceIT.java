package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.dto.ClienteResponseDTO;
import br.edu.ifc.bikes.dto.LocacaoRelatorioDTO;
import br.edu.ifc.bikes.exception.RelatorioException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class JasperServiceIT {

    private static final LocalDateTime RETIRADA = LocalDateTime.of(2026, 3, 10, 8, 0);

    @Autowired
    private JasperService jasperService;

    @Test
    void geraRelatorioDeLocacoesEmPdfComEstilosELogo() {
        List<LocacaoRelatorioDTO> dados = List.of(
                new LocacaoRelatorioDTO(1L, "Ana Silva", "BK01", RETIRADA, RETIRADA.plusMinutes(125), 125L, "2h 05min",
                        new BigDecimal("15.00"), "FINALIZADA"),
                new LocacaoRelatorioDTO(2L, "José Conceição", "BK02", RETIRADA.plusDays(1), null, null, "Em andamento",
                        null, "ATIVA"));

        byte[] pdf = jasperService.generatePdf(LocacaoService.RELATORIO_JASPER, "Relatório de Locações", "Teste", dados);

        assertPdf(pdf);
    }

    @Test
    void geraRelatorioDeClientesEmPdf() {
        List<ClienteResponseDTO> dados = List.of(
                new ClienteResponseDTO(1L, "Ana Silva", "12345678901"),
                new ClienteResponseDTO(2L, "Bruno Lima", "10987654321"));

        byte[] pdf = jasperService.generatePdf(ClienteService.RELATORIO_JASPER, "Relatório de Clientes", "Teste", dados);

        assertPdf(pdf);
    }

    @Test
    void geraRelatorioSemRegistros() {
        byte[] pdf = jasperService.generatePdf(LocacaoService.RELATORIO_JASPER, "Relatório de Locações", "Vazio", List.of());

        assertPdf(pdf);
    }

    @Test
    void relatorioInexistenteLancaRelatorioException() {
        RelatorioException ex = assertThrows(RelatorioException.class,
                () -> jasperService.generatePdf("inexistente", "Título", "Subtítulo", List.of()));

        assertEquals("Erro ao gerar o relatório 'inexistente'", ex.getMessage());
    }

    private static void assertPdf(byte[] pdf) {
        assertTrue(pdf != null && pdf.length > 1000, "PDF vazio");
        assertEquals("%PDF-", new String(pdf, 0, 5, StandardCharsets.US_ASCII));
    }
}
