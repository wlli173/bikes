package br.edu.ifc.bikes.config;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

public class SpringTimeZoneConfig {

    @PostConstruct //garante que o método seja executado logo após o construtor
                   // da classe inicializada pelo Spring
    public void timezoneConfig(){

        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
    }
}