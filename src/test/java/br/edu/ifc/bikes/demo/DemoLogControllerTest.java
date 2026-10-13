package br.edu.ifc.bikes.demo;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DemoLogControllerTest {

    private final DemoLogController controller = new DemoLogController();

    @Test
    void generateLogsRetorna200ComOsNiveisEmitidos() {
        ResponseEntity<String> response = controller.generateLogs();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Logs de DEBUG, INFO, WARN e ERROR emitidos com sucesso.", response.getBody());
    }

    @Test
    void generateErrorRetorna500() {
        ResponseEntity<String> response = controller.generateError();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Erro 500 simulado para evidência no Graylog.", response.getBody());
    }
}
