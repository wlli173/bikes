package br.edu.ifc.bikes.web.controller;

import br.edu.ifc.bikes.dto.CartaoCreditoDTO;
import br.edu.ifc.bikes.dto.DevolucaoRequestDTO;
import br.edu.ifc.bikes.dto.LocacaoRelatorioDTO;
import br.edu.ifc.bikes.dto.LocacaoRequestDTO;
import br.edu.ifc.bikes.dto.LocacaoResponseDTO;
import br.edu.ifc.bikes.exception.BicicletaIndisponivelException;
import br.edu.ifc.bikes.exception.EntityNotFoundException;
import br.edu.ifc.bikes.exception.LocacaoInvalidaException;
import br.edu.ifc.bikes.exception.RelatorioException;
import br.edu.ifc.bikes.service.LocacaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LocacaoController.class)
@AutoConfigureMockMvc(addFilters = false)
class LocacaoControllerTest {

    private static final String URL = "/api/v1/locacoes";
    private static final LocalDateTime RETIRADA = LocalDateTime.of(2026, 3, 10, 8, 0);
    private static final String CARTAO = "{\"numero\":\"4111111111111111\",\"titular\":\"ANA SILVA\",\"validade\":\"12/30\",\"cvv\":\"123\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LocacaoService locacaoService;

    @Test
    void createDelegaAoServicoERetorna201() throws Exception {
        LocalDateTime previsao = LocalDateTime.of(2099, 1, 1, 10, 0);
        when(locacaoService.create(any(LocacaoRequestDTO.class))).thenReturn(response("ATIVA", null));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigoBicicleta\":\"BK01\",\"dataPrevistaDevolucao\":\"2099-01-01T10:00:00\",\"cartao\":" + CARTAO + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.codigoBicicleta").value("BK01"))
                .andExpect(jsonPath("$.nomeCliente").value("Ana Silva"))
                .andExpect(jsonPath("$.cartaoFinal").value("1111"))
                .andExpect(jsonPath("$.status").value("ATIVA"));

        verify(locacaoService).create(new LocacaoRequestDTO("BK01", previsao,
                new CartaoCreditoDTO("4111111111111111", "ANA SILVA", "12/30", "123")));
    }

    @Test
    void createInvalidoRetorna422SemChamarServico() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigoBicicleta\":\"B\",\"dataPrevistaDevolucao\":\"2000-01-01T10:00:00\"," +
                                "\"cartao\":{\"numero\":\"123\",\"titular\":\"\",\"validade\":\"13/30\",\"cvv\":\"12\"}}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.errors.codigoBicicleta").exists())
                .andExpect(jsonPath("$.errors.dataPrevistaDevolucao").exists())
                .andExpect(jsonPath("$.errors['cartao.numero']").exists())
                .andExpect(jsonPath("$.errors['cartao.titular']").exists())
                .andExpect(jsonPath("$.errors['cartao.validade']").exists())
                .andExpect(jsonPath("$.errors['cartao.cvv']").exists());

        verify(locacaoService, never()).create(any());
    }

    @Test
    void createSemCartaoRetorna422() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dataPrevistaDevolucao\":\"2099-01-01T10:00:00\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.cartao").exists());

        verify(locacaoService, never()).create(any());
    }

    @Test
    void createComBicicletaIndisponivelRetorna409() throws Exception {
        when(locacaoService.create(any())).thenThrow(
                new BicicletaIndisponivelException("Bicicleta com código 'BK01' não está disponível (status MANUTENCAO)"));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigoBicicleta\":\"BK01\",\"dataPrevistaDevolucao\":\"2099-01-01T10:00:00\",\"cartao\":" + CARTAO + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Bicicleta com código 'BK01' não está disponível (status MANUTENCAO)"));
    }

    @Test
    void createSemBicicletaDisponivelRetorna404() throws Exception {
        when(locacaoService.create(any())).thenThrow(new EntityNotFoundException("Nenhuma bicicleta disponível para locação"));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dataPrevistaDevolucao\":\"2099-01-01T10:00:00\",\"cartao\":" + CARTAO + "}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Nenhuma bicicleta disponível para locação"));
    }

    @Test
    void devolverDelegaAoServicoERetorna200() throws Exception {
        when(locacaoService.devolver(eq(7L), any(DevolucaoRequestDTO.class)))
                .thenReturn(response("FINALIZADA", new BigDecimal("10.00")));

        mockMvc.perform(patch(URL + "/7/devolucao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"problema\":true,\"descricaoProblema\":\"Pneu furado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADA"))
                .andExpect(jsonPath("$.valor").value(10.00));

        verify(locacaoService).devolver(7L, new DevolucaoRequestDTO(true, "Pneu furado"));
    }

    @Test
    void devolverSemCorpoRetorna200() throws Exception {
        when(locacaoService.devolver(eq(7L), isNull())).thenReturn(response("FINALIZADA", new BigDecimal("5.00")));

        mockMvc.perform(patch(URL + "/7/devolucao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value(5.00));
    }

    @Test
    void devolverLocacaoFinalizadaRetorna409() throws Exception {
        when(locacaoService.devolver(eq(7L), any())).thenThrow(new LocacaoInvalidaException("Locação com id=7 já foi finalizada"));

        mockMvc.perform(patch(URL + "/7/devolucao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"problema\":false}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Locação com id=7 já foi finalizada"));
    }

    @Test
    void getByIdRetorna200() throws Exception {
        when(locacaoService.getById(7L)).thenReturn(response("ATIVA", null));

        mockMvc.perform(get(URL + "/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoBicicleta").value("BK01"));
    }

    @Test
    void getByIdInexistenteRetorna404() throws Exception {
        when(locacaoService.getById(9L)).thenThrow(new EntityNotFoundException("Locação com id=9 não encontrada"));

        mockMvc.perform(get(URL + "/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Locação com id=9 não encontrada"));
    }

    @Test
    void getAllRetorna200() throws Exception {
        when(locacaoService.getAll()).thenReturn(List.of(response("ATIVA", null)));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigoBicicleta").value("BK01"));
    }

    @Test
    void getRelatorioRetorna200() throws Exception {
        when(locacaoService.getRelatorio()).thenReturn(List.of(new LocacaoRelatorioDTO(7L, "Ana Silva", "BK01",
                RETIRADA, RETIRADA.plusMinutes(125), 125L, "2h 05min", new BigDecimal("15.00"), "FINALIZADA")));

        mockMvc.perform(get(URL + "/relatorio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nomeCliente").value("Ana Silva"))
                .andExpect(jsonPath("$[0].codigoBicicleta").value("BK01"))
                .andExpect(jsonPath("$[0].duracaoMinutos").value(125))
                .andExpect(jsonPath("$[0].duracao").value("2h 05min"))
                .andExpect(jsonPath("$[0].status").value("FINALIZADA"));
    }

    @Test
    void getRelatorioPdfRetornaPdfInline() throws Exception {
        byte[] pdf = "%PDF-1.7 teste".getBytes();
        when(locacaoService.getRelatorioPdf()).thenReturn(pdf);

        mockMvc.perform(get(URL + "/relatorio/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", startsWith("attachment; filename=\"locacoes-")))
                .andExpect(content().bytes(pdf));
    }

    @Test
    void getRelatorioPdfComFalhaRetorna500() throws Exception {
        when(locacaoService.getRelatorioPdf()).thenThrow(
                new RelatorioException("Erro ao gerar o relatório 'locacoes-bikes'", new RuntimeException()));

        mockMvc.perform(get(URL + "/relatorio/pdf"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Erro ao gerar o relatório 'locacoes-bikes'"));
    }

    private static LocacaoResponseDTO response(String status, BigDecimal valor) {
        return new LocacaoResponseDTO(7L, 4L, "Ana Silva", "BK01", RETIRADA, RETIRADA.plusHours(3),
                valor == null ? null : RETIRADA.plusHours(2), "1111", valor, status, null);
    }
}
