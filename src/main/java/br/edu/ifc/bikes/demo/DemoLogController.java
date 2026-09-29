package br.edu.ifc.bikes.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller de demonstração para gerar logs em todos os níveis.
 * Usado exclusivamente para evidenciar que DEBUG, INFO, WARN e ERROR
 * chegam ao Graylog de forma estruturada e pesquisável.
 *
 * <p>Opcional — pode ser removido antes do merge final.</p>
 */
@RestController
@RequestMapping("api/v1/demo/log")
public class DemoLogController {

    private static final Logger log = LoggerFactory.getLogger(DemoLogController.class);

    @GetMapping
    public ResponseEntity<String> generateLogs() {
        log.debug("Mensagem de DEBUG gerada pelo DemoLogController");
        log.info("Mensagem de INFO gerada pelo DemoLogController");
        log.warn("Mensagem de WARN gerada pelo DemoLogController");
        log.error("Mensagem de ERROR gerada pelo DemoLogController");
        return ResponseEntity.ok("Logs de DEBUG, INFO, WARN e ERROR emitidos com sucesso.");
    }

    @GetMapping("/error")
    public ResponseEntity<String> generateError() {
        log.error("Erro simulado — DemoLogController /error endpoint");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erro 500 simulado para evidência no Graylog.");
    }
}
