package br.edu.ifc.bikes.web.controller;

import br.edu.ifc.bikes.entity.Cliente;
import br.edu.ifc.bikes.entity.Usuario;
import br.edu.ifc.bikes.repository.ClienteRepository;
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
class ClienteControllerIT {

    private static final String URL = "/api/v1/clientes";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Test
    void cadastroSemAutenticacaoRetorna403() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Ana Silva", cpfUnico())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createValidoRetorna201EPreencheAuditoria() throws Exception {
        String username = emailUnico();
        String cpf = cpfUnico();
        String token = tokenDe(username);

        String body = mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Ana Silva", cpf)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Ana Silva"))
                .andExpect(jsonPath("$.cpf").value(cpf))
                .andReturn().getResponse().getContentAsString();

        Long id = Long.valueOf(body.replaceAll(".*\"id\":(\\d+).*", "$1"));
        Cliente salvo = clienteRepository.findById(id).orElseThrow();
        assertEquals(username, salvo.getCriadoPor());
        assertEquals(username, salvo.getModificadoPor());
        assertNotNull(salvo.getDataCriacao());
        assertNotNull(salvo.getDataModificacao());
        assertEquals(username, salvo.getUsuario().getUsername());
    }

    @Test
    void clienteInvalidoRetorna422() throws Exception {
        String token = tokenDe(emailUnico());

        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"cpf\":\"123\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.errors.nome").exists())
                .andExpect(jsonPath("$.errors.cpf").exists());
    }

    @Test
    void cpfDuplicadoRetorna409() throws Exception {
        String cpf = cpfUnico();
        String tokenA = tokenDe(emailUnico());
        String tokenB = tokenDe(emailUnico());
        criarCliente(tokenA, "Ana Silva", cpf);

        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Bruno Lima", cpf)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("CPF '" + cpf + "' já cadastrado"));
    }

    @Test
    void segundoCadastroDoMesmoUsuarioRetorna409() throws Exception {
        String token = tokenDe(emailUnico());
        criarCliente(token, "Ana Silva", cpfUnico());

        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Ana Silva", cpfUnico())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Usuário já possui um cliente cadastrado"));
    }

    @Test
    void clienteConsultaApenasOsPropriosDados() throws Exception {
        String tokenA = tokenDe(emailUnico());
        String tokenB = tokenDe(emailUnico());
        String idA = criarCliente(tokenA, "Ana Silva", cpfUnico());
        String idB = criarCliente(tokenB, "Bruno Lima", cpfUnico());

        mockMvc.perform(get(URL + "/" + idA)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Silva"));

        mockMvc.perform(get(URL + "/" + idB)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminConsultaTodosOsClientes() throws Exception {
        String tokenCliente = tokenDe(emailUnico());
        String id = criarCliente(tokenCliente, "Ana Silva", cpfUnico());
        String tokenAdmin = tokenAdmin();

        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get(URL + "/" + id)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(Integer.parseInt(id)));

        mockMvc.perform(get(URL + "/999999")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cliente com id=999999 não encontrado"));
    }

    private String criarCliente(String token, String nome, String cpf) throws Exception {
        String body = mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(nome, cpf)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\"id\":(\\d+).*", "$1");
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

    private String tokenAdmin() throws Exception {
        String username = emailUnico();
        String token = tokenDe(username);
        Usuario usuario = usuarioRepository.findByUsername(username).orElseThrow();
        usuario.setRole(Usuario.Role.ROLE_ADMIN);
        usuarioRepository.saveAndFlush(usuario);
        return token;
    }

    private static String emailUnico() {
        return "cliente-" + UUID.randomUUID() + "@bikes.com";
    }

    private static String cpfUnico() {
        long numero = Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 100_000_000_000L);
        return String.format("%011d", numero);
    }

    private static String json(String nome, String cpf) {
        return "{\"nome\":\"" + nome + "\",\"cpf\":\"" + cpf + "\"}";
    }
}
