package br.edu.ifc.bikes.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UsuarioControllerTest {

    private static final String URL = "/api/v1/usuarios";

    @Autowired
    private MockMvc mockMvc;

    private String criarUsuario(String username) throws Exception {
        String body = mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"123456\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.role").value("CLIENTE"))
                .andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\"id\":(\\d+).*", "$1");
    }

    private static String emailUnico() {
        return "user-" + UUID.randomUUID() + "@bikes.com";
    }

    @Test
    void criaUsuarioValidoRetorna201() throws Exception {
        criarUsuario(emailUnico());
    }

    @Test
    void usuarioInvalidoRetorna422ComErrosPorCampo() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nao-e-email\",\"password\":\"123\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.errors.username").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void buscaPorIdExistenteRetorna200() throws Exception {
        String username = emailUnico();
        String id = criarUsuario(username);

        mockMvc.perform(get(URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    void buscaPorIdInexistenteRetorna404() throws Exception {
        mockMvc.perform(get(URL + "/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void atualizaSenhaRetorna204() throws Exception {
        String id = criarUsuario(emailUnico());

        mockMvc.perform(patch(URL + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"123456\",\"novaSenha\":\"654321\",\"confirmaSenha\":\"654321\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch(URL + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"654321\",\"novaSenha\":\"111111\",\"confirmaSenha\":\"111111\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void senhaAtualInvalidaRetorna400() throws Exception {
        String id = criarUsuario(emailUnico());

        mockMvc.perform(patch(URL + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"000000\",\"novaSenha\":\"654321\",\"confirmaSenha\":\"654321\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("A senha atual não confere"));
    }

    @Test
    void novaSenhaDiferenteDaConfirmacaoRetorna400() throws Exception {
        String id = criarUsuario(emailUnico());

        mockMvc.perform(patch(URL + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"123456\",\"novaSenha\":\"654321\",\"confirmaSenha\":\"111111\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("A nova senha não confere com a confirmação de senha"));
    }

    @Test
    void camposDeSenhaInvalidosRetornam422() throws Exception {
        String id = criarUsuario(emailUnico());

        mockMvc.perform(patch(URL + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"\",\"novaSenha\":\"123\",\"confirmaSenha\":\"\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.errors.senhaAtual").exists())
                .andExpect(jsonPath("$.errors.novaSenha").exists())
                .andExpect(jsonPath("$.errors.confirmaSenha").exists());
    }

    @Test
    void atualizaSenhaDeUsuarioInexistenteRetorna404() throws Exception {
        mockMvc.perform(patch(URL + "/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"123456\",\"novaSenha\":\"654321\",\"confirmaSenha\":\"654321\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void listaUsuariosRetorna200() throws Exception {
        criarUsuario(emailUnico());

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
