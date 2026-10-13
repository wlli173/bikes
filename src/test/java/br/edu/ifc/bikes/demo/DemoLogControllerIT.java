package br.edu.ifc.bikes.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DemoLogControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generateLogsRetorna200() throws Exception {
        mockMvc.perform(get("/api/v1/demo/log"))
                .andExpect(status().isOk())
                .andExpect(content().string("Logs de DEBUG, INFO, WARN e ERROR emitidos com sucesso."));
    }

    @Test
    void generateErrorRetorna500() throws Exception {
        mockMvc.perform(get("/api/v1/demo/log/error"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro 500 simulado para evidência no Graylog."));
    }
}
