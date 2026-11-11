package br.edu.ifc.bikes.web.controller;

import br.edu.ifc.bikes.entity.Bicicleta;
import br.edu.ifc.bikes.entity.Usuario;
import br.edu.ifc.bikes.repository.BicicletaRepository;
import br.edu.ifc.bikes.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BicicletaControllerIT {

    private static final String URL = "/api/v1/bicicletas";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BicicletaRepository bicicletaRepository;

    @Test
    void cadastroSemAutenticacaoRetorna403() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(codigoUnico(), "Caloi Elite", "DISPONIVEL")))
                .andExpect(status().isForbidden());
    }

    @Test
    void clienteNaoGerenciaBicicletas() throws Exception {
        String token = tokenDe(emailUnico());
        String codigo = codigoUnico();

        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(codigo, "Caloi Elite", "DISPONIVEL")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(URL + "/" + codigo)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void createValidoRetorna201EPreencheAuditoria() throws Exception {
        String username = emailUnico();
        String token = tokenAdmin(username);
        String codigo = codigoUnico();

        String body = mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(codigo, "Caloi Elite", "DISPONIVEL")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.codigo").value(codigo))
                .andExpect(jsonPath("$.modelo").value("Caloi Elite"))
                .andExpect(jsonPath("$.status").value("DISPONIVEL"))
                .andReturn().getResponse().getContentAsString();

        Long id = Long.valueOf(body.replaceAll(".*\"id\":(\\d+).*", "$1"));
        Bicicleta salva = bicicletaRepository.findById(id).orElseThrow();
        assertEquals(codigo, salva.getCodigo());
        assertEquals(Bicicleta.Status.DISPONIVEL, salva.getStatus());
        assertEquals(username, salva.getCriadoPor());
        assertEquals(username, salva.getModificadoPor());
        assertNotNull(salva.getDataCriacao());
        assertNotNull(salva.getDataModificacao());
    }

    @Test
    void bicicletaInvalidaRetorna422() throws Exception {
        String token = tokenAdmin(emailUnico());

        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"\",\"modelo\":\"\",\"status\":\"QUEBRADA\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.errors.codigo").exists())
                .andExpect(jsonPath("$.errors.modelo").exists())
                .andExpect(jsonPath("$.errors.status").exists());
    }

    @Test
    void codigoDuplicadoRetorna409() throws Exception {
        String token = tokenAdmin(emailUnico());
        String codigo = codigoUnico();
        criar(token, codigo, "ALUGADA");

        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(codigo, "Oggi", "MANUTENCAO")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Código '" + codigo + "' já cadastrado"));
    }

    @Test
    void adminLocalizaPeloCodigoEListaTodas() throws Exception {
        String token = tokenAdmin(emailUnico());
        String codigo = codigoUnico();
        criar(token, codigo, "ALUGADA");

        mockMvc.perform(get(URL + "/" + codigo)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(codigo))
                .andExpect(jsonPath("$.status").value("ALUGADA"));

        mockMvc.perform(get(URL + "/INEXISTENTE")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Bicicleta com código 'INEXISTENTE' não encontrada"));

        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    private void criar(String token, String codigo, String status) throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(codigo, "Caloi Elite", status)))
                .andExpect(status().isCreated());
    }

    private String tokenDe(String username) throws Exception {
        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"123456\"}"))
                .andExpect(status().isCreated());

        String body = mockMvc.perform(post("/api/v1/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    private String tokenAdmin(String username) throws Exception {
        String token = tokenDe(username);
        Usuario usuario = usuarioRepository.findByUsername(username).orElseThrow();
        usuario.setRole(Usuario.Role.ROLE_ADMIN);
        usuarioRepository.saveAndFlush(usuario);
        return token;
    }

    private static String emailUnico() {
        return "bike-" + UUID.randomUUID() + "@bikes.com";
    }

    private static String codigoUnico() {
        return "BK" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private static String json(String codigo, String modelo, String status) {
        return "{\"codigo\":\"" + codigo + "\",\"modelo\":\"" + modelo + "\",\"status\":\"" + status + "\"}";
    }
}
