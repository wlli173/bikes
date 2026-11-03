package br.edu.ifc.bikes.web.controller;

import br.edu.ifc.bikes.dto.UsuarioRequestDTO;
import br.edu.ifc.bikes.dto.UsuarioResponseDTO;
import br.edu.ifc.bikes.exception.EntityNotFoundException;
import br.edu.ifc.bikes.exception.PasswordInvalidException;
import br.edu.ifc.bikes.exception.UsernameUniqueViolationException;
import br.edu.ifc.bikes.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UsuarioController.class)
@AutoConfigureMockMvc(addFilters = false)
class UsuarioControllerTest {

    private static final String URL = "/api/v1/usuarios";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @Test
    void createDelegaAoServicoERetorna201() throws Exception {
        UsuarioRequestDTO request = new UsuarioRequestDTO("ana@bikes.com", "123456");
        when(usuarioService.create(request)).thenReturn(new UsuarioResponseDTO(1L, "ana@bikes.com", "CLIENTE"));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana@bikes.com\",\"password\":\"123456\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("ana@bikes.com"))
                .andExpect(jsonPath("$.role").value("CLIENTE"));

        verify(usuarioService).create(request);
    }

    @Test
    void createInvalidoRetorna422SemChamarServico() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nao-e-email\",\"password\":\"123\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.errors.username").exists())
                .andExpect(jsonPath("$.errors.password").exists());

        verify(usuarioService, never()).create(any());
    }

    @Test
    void createComUsernameDuplicadoRetorna409() throws Exception {
        when(usuarioService.create(any())).thenThrow(
                new UsernameUniqueViolationException("O nome do usuário 'ana@bikes.com' já existe"));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana@bikes.com\",\"password\":\"123456\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("O nome do usuário 'ana@bikes.com' já existe"));
    }

    @Test
    void getByIdRetorna200() throws Exception {
        when(usuarioService.getById(4L)).thenReturn(new UsuarioResponseDTO(4L, "ana@bikes.com", "CLIENTE"));

        mockMvc.perform(get(URL + "/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ana@bikes.com"));
    }

    @Test
    void getByIdNuloRetorna404() throws Exception {
        when(usuarioService.getById(8L)).thenReturn(null);

        mockMvc.perform(get(URL + "/8"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByIdInexistenteRetorna404() throws Exception {
        when(usuarioService.getById(9L)).thenThrow(new EntityNotFoundException("Usuário com id=9 não encontrado"));

        mockMvc.perform(get(URL + "/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Usuário com id=9 não encontrado"));
    }

    @Test
    void updatePasswordRetorna200() throws Exception {
        when(usuarioService.updatePassword(3L, "654321", "654321", "123456"))
                .thenReturn(new UsuarioResponseDTO(3L, "ana@bikes.com", "CLIENTE"));

        mockMvc.perform(patch(URL + "/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"123456\",\"novaSenha\":\"654321\",\"confirmaSenha\":\"654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.username").value("ana@bikes.com"));

        verify(usuarioService).updatePassword(3L, "654321", "654321", "123456");
    }

    @Test
    void updatePasswordComSenhaInvalidaRetorna400() throws Exception {
        doThrow(new PasswordInvalidException("Senha atual incorreta"))
                .when(usuarioService).updatePassword(eq(3L), any(), any(), any());

        mockMvc.perform(patch(URL + "/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"000000\",\"novaSenha\":\"654321\",\"confirmaSenha\":\"654321\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Senha atual incorreta"));
    }

    @Test
    void updatePasswordDeUsuarioInexistenteRetorna404() throws Exception {
        doThrow(new EntityNotFoundException("Usuário 9 não encontrado"))
                .when(usuarioService).updatePassword(eq(9L), any(), any(), any());

        mockMvc.perform(patch(URL + "/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"123456\",\"novaSenha\":\"654321\",\"confirmaSenha\":\"654321\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Usuário 9 não encontrado"));
    }

    @Test
    void getAllRetorna200() throws Exception {
        when(usuarioService.getAll()).thenReturn(List.of(new UsuarioResponseDTO(1L, "ana@bikes.com", "CLIENTE")));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("ana@bikes.com"));
    }
}
