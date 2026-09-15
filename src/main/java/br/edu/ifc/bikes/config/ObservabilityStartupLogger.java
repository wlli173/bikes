package br.edu.ifc.bikes.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ObservabilityStartupLogger implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        log.debug("Log DEBUG da aplicação bikes visível no Graylog");
        log.info("Log INFO da aplicação bikes visível no Graylog");
        log.error("Log ERROR da aplicação bikes visível no Graylog (demo, sem falha de negócio)");
    }
}
