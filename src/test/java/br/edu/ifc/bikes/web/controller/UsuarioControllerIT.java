package br.edu.ifc.bikes.web.controller;

import br.edu.ifc.bikes.entity.Usuario;
import br.edu.ifc.bikes.repository.UsuarioRepository;
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
class UsuarioControllerIT {

    private static final String URL = "/api/v1/usuarios";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

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

    private String tokenDe(String username) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    @Test
    void criaUsuarioValidoRetorna201() throws Exception {
        criarUsuario(emailUnico());
    }

    @Test
    void usernameDuplicadoRetorna409() throws Exception {
        String username = emailUnico();
        criarUsuario(username);

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"123456\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("O nome do usuário '" + username + "' já existe"));
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

        String token = tokenDe(username);

        mockMvc.perform(get(URL + "/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    void buscaPorIdInexistenteRetorna404() throws Exception {
        String token = tokenAdmin();

        mockMvc.perform(get(URL + "/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void clienteNaoConsultaUsuarioDeOutroId() throws Exception {
        String token = tokenDeUsuarioNovo();

        mockMvc.perform(get(URL + "/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void atualizaSenhaRetorna200() throws Exception {
        String username = emailUnico();
        String id = criarUsuario(username);
        String token = tokenDe(username);

        mockMvc.perform(patch(URL + "/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"123456\",\"novaSenha\":\"654321\",\"confirmaSenha\":\"654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));

        mockMvc.perform(patch(URL + "/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"654321\",\"novaSenha\":\"111111\",\"confirmaSenha\":\"111111\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void senhaAtualInvalidaRetorna400() throws Exception {
        String username = emailUnico();
        String id = criarUsuario(username);
        String token = tokenDe(username);

        mockMvc.perform(patch(URL + "/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"000000\",\"novaSenha\":\"654321\",\"confirmaSenha\":\"654321\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Senha atual incorreta"));
    }

    @Test
    void novaSenhaDiferenteDaConfirmacaoRetorna400() throws Exception {
        String username = emailUnico();
        String id = criarUsuario(username);
        String token = tokenDe(username);

        mockMvc.perform(patch(URL + "/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"123456\",\"novaSenha\":\"654321\",\"confirmaSenha\":\"111111\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("As senhas não coincidem"));
    }

    @Test
    void atualizaSenhaDeOutroUsuarioRetorna403() throws Exception {
        String token = tokenDeUsuarioNovo();

        mockMvc.perform(patch(URL + "/999999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"123456\",\"novaSenha\":\"654321\",\"confirmaSenha\":\"654321\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listaUsuariosRetorna200() throws Exception {
        String token = tokenAdmin();

        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void clienteNaoListaUsuarios() throws Exception {
        String token = tokenDeUsuarioNovo();

        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private String tokenDeUsuarioNovo() throws Exception {
        String username = emailUnico();
        criarUsuario(username);
        return tokenDe(username);
    }

    private String tokenAdmin() throws Exception {
        String username = emailUnico();
        criarUsuario(username);
        Usuario usuario = usuarioRepository.findByUsername(username).orElseThrow();
        usuario.setRole(Usuario.Role.ROLE_ADMIN);
        usuarioRepository.saveAndFlush(usuario);
        return tokenDe(username);
    }
}
