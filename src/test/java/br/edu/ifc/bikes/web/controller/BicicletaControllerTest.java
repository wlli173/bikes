package br.edu.ifc.bikes.web.controller;

import br.edu.ifc.bikes.dto.BicicletaRequestDTO;
import br.edu.ifc.bikes.dto.BicicletaResponseDTO;
import br.edu.ifc.bikes.exception.BicicletaUniqueViolationException;
import br.edu.ifc.bikes.exception.EntityNotFoundException;
import br.edu.ifc.bikes.service.BicicletaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BicicletaController.class)
@AutoConfigureMockMvc(addFilters = false)
class BicicletaControllerTest {

    private static final String URL = "/api/v1/bicicletas";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BicicletaService bicicletaService;

    @Test
    void createDelegaAoServicoERetorna201() throws Exception {
        BicicletaRequestDTO request = new BicicletaRequestDTO("BK01", "Caloi Elite", "DISPONIVEL");
        when(bicicletaService.create(request)).thenReturn(new BicicletaResponseDTO(1L, "BK01", "Caloi Elite", "DISPONIVEL"));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"BK01\",\"modelo\":\"Caloi Elite\",\"status\":\"DISPONIVEL\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.codigo").value("BK01"))
                .andExpect(jsonPath("$.modelo").value("Caloi Elite"))
                .andExpect(jsonPath("$.status").value("DISPONIVEL"));

        verify(bicicletaService).create(request);
    }

    @Test
    void createInvalidoRetorna422SemChamarServico() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"\",\"modelo\":\"\",\"status\":\"QUEBRADA\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.errors.codigo").exists())
                .andExpect(jsonPath("$.errors.modelo").exists())
                .andExpect(jsonPath("$.errors.status").exists());

        verify(bicicletaService, never()).create(any());
    }

    @Test
    void createComCodigoDuplicadoRetorna409() throws Exception {
        when(bicicletaService.create(any())).thenThrow(new BicicletaUniqueViolationException("Código 'BK01' já cadastrado"));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"BK01\",\"modelo\":\"Caloi Elite\",\"status\":\"DISPONIVEL\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Código 'BK01' já cadastrado"));
    }

    @Test
    void getByCodigoRetorna200() throws Exception {
        when(bicicletaService.getByCodigo("BK01")).thenReturn(new BicicletaResponseDTO(4L, "BK01", "Caloi Elite", "ALUGADA"));

        mockMvc.perform(get(URL + "/BK01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("BK01"))
                .andExpect(jsonPath("$.status").value("ALUGADA"));
    }

    @Test
    void getByCodigoInexistenteRetorna404() throws Exception {
        when(bicicletaService.getByCodigo("BK99")).thenThrow(
                new EntityNotFoundException("Bicicleta com código 'BK99' não encontrada"));

        mockMvc.perform(get(URL + "/BK99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Bicicleta com código 'BK99' não encontrada"));
    }

    @Test
    void getAllRetorna200() throws Exception {
        when(bicicletaService.getAll()).thenReturn(List.of(new BicicletaResponseDTO(1L, "BK01", "Caloi Elite", "DISPONIVEL")));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("BK01"));
    }
}
