package br.edu.ifc.bikes.config;

import jakarta.annotation.PostConstruct;

import java.util.TimeZone;

public class SpringTimeZoneConfig {

    @PostConstruct //Garante que o metodo seja executado logo após o construtor de classe inicializada pelo Sptring
    public void TimeZoneConfig(){
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
    }


}
