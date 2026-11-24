package br.edu.ifc.bikes.web.controller;

import br.edu.ifc.bikes.entity.Bicicleta;
import br.edu.ifc.bikes.entity.Locacao;
import br.edu.ifc.bikes.entity.Usuario;
import br.edu.ifc.bikes.repository.BicicletaRepository;
import br.edu.ifc.bikes.repository.LocacaoRepository;
import br.edu.ifc.bikes.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LocacaoControllerIT {

    private static final String URL = "/api/v1/locacoes";
    private static final String CARTAO = "{\"numero\":\"4111111111111111\",\"titular\":\"ANA SILVA\",\"validade\":\"12/30\",\"cvv\":\"123\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BicicletaRepository bicicletaRepository;

    @Autowired
    private LocacaoRepository locacaoRepository;

    @Test
    void locacaoSemAutenticacaoRetorna403() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLocacao(codigoUnico())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(URL + "/relatorio"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminNaoSolicitaLocacaoEClienteNaoListaNemGeraRelatorio() throws Exception {
        String tokenAdmin = tokenAdmin();
        String tokenCliente = tokenCliente();

        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLocacao(null)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(URL + "/relatorio")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioSemCadastroDeClienteRetorna404() throws Exception {
        String token = tokenDe(emailUnico());

        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLocacao(null)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Usuário autenticado não possui cadastro de cliente"));
    }

    @Test
    void locacaoInvalidaRetorna422() throws Exception {
        String token = tokenCliente();

        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dataPrevistaDevolucao\":\"2000-01-01T10:00:00\",\"cartao\":{\"numero\":\"123\",\"titular\":\"A\",\"validade\":\"2030\",\"cvv\":\"1\"}}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.errors.dataPrevistaDevolucao").exists())
                .andExpect(jsonPath("$.errors['cartao.numero']").exists())
                .andExpect(jsonPath("$.errors['cartao.titular']").exists())
                .andExpect(jsonPath("$.errors['cartao.validade']").exists())
                .andExpect(jsonPath("$.errors['cartao.cvv']").exists());
    }

    @Test
    void fluxoCompletoDeLocacaoEDevolucao() throws Exception {
        String tokenAdmin = tokenAdmin();
        String username = emailUnico();
        String tokenCliente = tokenCliente(username, "Ana Silva");
        String codigo = codigoUnico();
        criarBicicleta(tokenAdmin, codigo, "DISPONIVEL");

        String body = mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLocacao(codigo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nomeCliente").value("Ana Silva"))
                .andExpect(jsonPath("$.codigoBicicleta").value(codigo))
                .andExpect(jsonPath("$.dataRetirada").isString())
                .andExpect(jsonPath("$.dataPrevistaDevolucao").isString())
                .andExpect(jsonPath("$.dataDevolucao").doesNotExist())
                .andExpect(jsonPath("$.cartaoFinal").value("1111"))
                .andExpect(jsonPath("$.valor").doesNotExist())
                .andExpect(jsonPath("$.status").value("ATIVA"))
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(body.replaceAll(".*\"id\":(\\d+).*", "$1"));

        Locacao ativa = locacaoRepository.findById(id).orElseThrow();
        assertEquals(username, ativa.getCriadoPor());
        assertNotNull(ativa.getDataCriacao());
        assertEquals(Bicicleta.Status.ALUGADA, bicicletaRepository.findByCodigo(codigo).orElseThrow().getStatus());

        // a mesma bicicleta não participa de duas locações simultâneas
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLocacao(codigo)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Bicicleta com código '" + codigo + "' não está disponível (status ALUGADA)"));

        // o cliente consulta a própria locação
        mockMvc.perform(get(URL + "/" + id)
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        // devolução com cálculo do valor e liberação da bicicleta
        mockMvc.perform(patch(URL + "/" + id + "/devolucao")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"problema\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADA"))
                .andExpect(jsonPath("$.dataDevolucao").isString())
                .andExpect(jsonPath("$.valor").value(5.00));

        Locacao finalizada = locacaoRepository.findById(id).orElseThrow();
        assertNotNull(finalizada.getDataDevolucao());
        assertEquals(0, new BigDecimal("5.00").compareTo(finalizada.getValor()));
        assertEquals(Locacao.Status.FINALIZADA, finalizada.getStatus());
        assertNull(finalizada.getObservacao());
        assertEquals(Bicicleta.Status.DISPONIVEL, bicicletaRepository.findByCodigo(codigo).orElseThrow().getStatus());

        // uma locação só pode ser devolvida uma vez
        mockMvc.perform(patch(URL + "/" + id + "/devolucao")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Locação com id=" + id + " já foi finalizada"));

        // relatório de locações e devoluções (admin)
        mockMvc.perform(get(URL + "/relatorio")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].nomeCliente").value("Ana Silva"))
                .andExpect(jsonPath("$[?(@.id == " + id + ")].codigoBicicleta").value(codigo))
                .andExpect(jsonPath("$[?(@.id == " + id + ")].dataRetirada").isNotEmpty())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].dataDevolucao").isNotEmpty())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].duracaoMinutos").value(0))
                .andExpect(jsonPath("$[?(@.id == " + id + ")].duracao").value("0h 00min"))
                .andExpect(jsonPath("$[?(@.id == " + id + ")].status").value("FINALIZADA"));

        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].status").value("FINALIZADA"));
    }

    @Test
    void devolucaoComProblemaEnviaBicicletaParaManutencao() throws Exception {
        String tokenAdmin = tokenAdmin();
        String tokenCliente = tokenCliente();
        String codigo = codigoUnico();
        criarBicicleta(tokenAdmin, codigo, "DISPONIVEL");
        String id = criarLocacao(tokenCliente, codigo);

        // o admin registra a devolução de qualquer locação
        mockMvc.perform(patch(URL + "/" + id + "/devolucao")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"problema\":true,\"descricaoProblema\":\"Pneu furado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADA"))
                .andExpect(jsonPath("$.observacao").value("Pneu furado"));

        assertEquals(Bicicleta.Status.MANUTENCAO, bicicletaRepository.findByCodigo(codigo).orElseThrow().getStatus());

        // bicicleta em manutenção não pode ser alugada
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLocacao(codigo)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Bicicleta com código '" + codigo + "' não está disponível (status MANUTENCAO)"));
    }

    @Test
    void clienteNaoAcessaLocacaoDeOutroCliente() throws Exception {
        String tokenAdmin = tokenAdmin();
        String tokenA = tokenCliente();
        String tokenB = tokenCliente();
        String codigo = codigoUnico();
        criarBicicleta(tokenAdmin, codigo, "DISPONIVEL");
        String id = criarLocacao(tokenA, codigo);

        mockMvc.perform(get(URL + "/" + id)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch(URL + "/" + id + "/devolucao")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(URL + "/999999")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Locação com id=999999 não encontrada"));
    }

    @Test
    void semCodigoOSistemaEscolheUmaBicicletaDisponivel() throws Exception {
        String tokenAdmin = tokenAdmin();
        String tokenCliente = tokenCliente();
        String codigo = codigoUnico();
        criarBicicleta(tokenAdmin, codigo, "DISPONIVEL");

        String body = mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLocacao(null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoBicicleta").isString())
                .andExpect(jsonPath("$.status").value("ATIVA"))
                .andReturn().getResponse().getContentAsString();
        String codigoEscolhido = body.replaceAll(".*\"codigoBicicleta\":\"([^\"]+)\".*", "$1");

        assertEquals(Bicicleta.Status.ALUGADA, bicicletaRepository.findByCodigo(codigoEscolhido).orElseThrow().getStatus());

        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLocacao("INEXISTENTE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Bicicleta com código 'INEXISTENTE' não encontrada"));
    }

    @Test
    void adminGeraRelatorioEmPdfEClienteNaoTemAcesso() throws Exception {
        String tokenAdmin = tokenAdmin();
        String tokenCliente = tokenCliente();
        String codigo = codigoUnico();
        criarBicicleta(tokenAdmin, codigo, "DISPONIVEL");
        criarLocacao(tokenCliente, codigo);

        byte[] pdf = mockMvc.perform(get(URL + "/relatorio/pdf")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", startsWith("attachment; filename=\"locacoes-")))
                .andReturn().getResponse().getContentAsByteArray();

        assertTrue(pdf.length > 1000);
        assertEquals("%PDF-", new String(pdf, 0, 5, StandardCharsets.US_ASCII));

        mockMvc.perform(get(URL + "/relatorio/pdf")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isForbidden());
    }

    private String criarLocacao(String token, String codigo) throws Exception {
        String body = mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLocacao(codigo)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\"id\":(\\d+).*", "$1");
    }

    private void criarBicicleta(String tokenAdmin, String codigo, String status) throws Exception {
        mockMvc.perform(post("/api/v1/bicicletas")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"" + codigo + "\",\"modelo\":\"Caloi Elite\",\"status\":\"" + status + "\"}"))
                .andExpect(status().isCreated());
    }

    private String tokenCliente() throws Exception {
        return tokenCliente(emailUnico(), "Bruno Lima");
    }

    private String tokenCliente(String username, String nome) throws Exception {
        String token = tokenDe(username);
        mockMvc.perform(post("/api/v1/clientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\",\"cpf\":\"" + cpfUnico() + "\"}"))
                .andExpect(status().isCreated());
        return token;
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
        return "locacao-" + UUID.randomUUID() + "@bikes.com";
    }

    private static String cpfUnico() {
        long numero = Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 100_000_000_000L);
        return String.format("%011d", numero);
    }

    private static String codigoUnico() {
        return "LC" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private static String jsonLocacao(String codigo) {
        String previsao = LocalDateTime.now().plusHours(2).withNano(0).toString();
        String codigoJson = codigo == null ? "" : "\"codigoBicicleta\":\"" + codigo + "\",";
        return "{" + codigoJson + "\"dataPrevistaDevolucao\":\"" + previsao + "\",\"cartao\":" + CARTAO + "}";
    }
}
