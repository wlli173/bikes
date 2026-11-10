package br.edu.ifc.bikes.web.controller;

import br.edu.ifc.bikes.dto.ClienteRequestDTO;
import br.edu.ifc.bikes.dto.ClienteResponseDTO;
import br.edu.ifc.bikes.exception.ClienteUniqueViolationException;
import br.edu.ifc.bikes.exception.EntityNotFoundException;
import br.edu.ifc.bikes.service.ClienteService;
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

@WebMvcTest(controllers = ClienteController.class)
@AutoConfigureMockMvc(addFilters = false)
class ClienteControllerTest {

    private static final String URL = "/api/v1/clientes";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClienteService clienteService;

    @Test
    void createDelegaAoServicoERetorna201() throws Exception {
        ClienteRequestDTO request = new ClienteRequestDTO("Ana Silva", "12345678901");
        when(clienteService.create(request)).thenReturn(new ClienteResponseDTO(1L, "Ana Silva", "12345678901"));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana Silva\",\"cpf\":\"12345678901\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Ana Silva"))
                .andExpect(jsonPath("$.cpf").value("12345678901"));

        verify(clienteService).create(request);
    }

    @Test
    void createInvalidoRetorna422SemChamarServico() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"cpf\":\"123\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.errors.nome").exists())
                .andExpect(jsonPath("$.errors.cpf").exists());

        verify(clienteService, never()).create(any());
    }

    @Test
    void createComCpfDuplicadoRetorna409() throws Exception {
        when(clienteService.create(any())).thenThrow(new ClienteUniqueViolationException("CPF '12345678901' já cadastrado"));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana Silva\",\"cpf\":\"12345678901\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("CPF '12345678901' já cadastrado"));
    }

    @Test
    void getByIdRetorna200() throws Exception {
        when(clienteService.getById(4L)).thenReturn(new ClienteResponseDTO(4L, "Ana Silva", "12345678901"));

        mockMvc.perform(get(URL + "/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Silva"));
    }

    @Test
    void getByIdInexistenteRetorna404() throws Exception {
        when(clienteService.getById(9L)).thenThrow(new EntityNotFoundException("Cliente com id=9 não encontrado"));

        mockMvc.perform(get(URL + "/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cliente com id=9 não encontrado"));
    }

    @Test
    void getAllRetorna200() throws Exception {
        when(clienteService.getAll()).thenReturn(List.of(new ClienteResponseDTO(1L, "Ana Silva", "12345678901")));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cpf").value("12345678901"));
    }
}
