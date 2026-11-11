package br.edu.ifc.bikes.repository;

import br.edu.ifc.bikes.entity.Bicicleta;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class BicicletaRepositoryIT {

    @Autowired
    private BicicletaRepository bicicletaRepository;

    @Test
    void saveAtribuiIdEPermiteBuscaPeloCodigo() {
        Bicicleta salva = bicicletaRepository.saveAndFlush(nova("BK01"));

        assertTrue(salva.getId() != null && salva.getId() > 0);
        assertEquals(salva.getId(), bicicletaRepository.findByCodigo("BK01").orElseThrow().getId());
        assertTrue(bicicletaRepository.existsByCodigo("BK01"));
        assertEquals(Bicicleta.Status.DISPONIVEL, salva.getStatus());
    }

    @Test
    void codigoDuplicadoViolentaRestricaoUnica() {
        bicicletaRepository.saveAndFlush(nova("BK01"));

        assertThrows(DataIntegrityViolationException.class, () -> bicicletaRepository.saveAndFlush(nova("BK01")));
    }

    private static Bicicleta nova(String codigo) {
        Bicicleta bicicleta = new Bicicleta();
        bicicleta.setCodigo(codigo);
        bicicleta.setModelo("Caloi Elite");
        bicicleta.setStatus(Bicicleta.Status.DISPONIVEL);
        return bicicleta;
    }
}
