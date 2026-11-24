package br.edu.ifc.bikes;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Garante que a documentação OpenAPI usada pelo Swagger UI (/docs-bikes.html) continua pública,
 * expõe o esquema Bearer JWT (botão "Authorize") e marca com cadeado apenas as operações protegidas.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DocumentacaoApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void documentacaoEPublicaEExpoeEsquemaBearerJwt() throws Exception {
        mockMvc.perform(get("/docs-bikes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Bikes API"))
                .andExpect(jsonPath("$.components.securitySchemes.security.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.security.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.security.bearerFormat").value("JWT"));
    }

    @Test
    void operacoesProtegidasExigemTokenENaoAsPublicas() throws Exception {
        mockMvc.perform(get("/docs-bikes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/auth'].post.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/usuarios'].post.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/usuarios'].get.security[*]", hasItem(hasKey("security"))))
                .andExpect(jsonPath("$.paths['/api/v1/clientes'].post.security[*]", hasItem(hasKey("security"))))
                .andExpect(jsonPath("$.paths['/api/v1/locacoes'].post.security[*]", hasItem(hasKey("security"))))
                .andExpect(jsonPath("$.paths['/api/v1/locacoes/relatorio/pdf'].get.security[*]", hasItem(hasKey("security"))));
    }

    @Test
    void swaggerUiEstaAcessivelSemAutenticacao() throws Exception {
        mockMvc.perform(get("/docs-bikes.html"))
                .andExpect(status().is3xxRedirection());
    }
}
