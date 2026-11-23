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

    @Test
    void findFirstByStatusDevolveADisponivelDeMenorId() {
        bicicletaRepository.saveAndFlush(nova("BK01", Bicicleta.Status.ALUGADA));
        Bicicleta disponivel = bicicletaRepository.saveAndFlush(nova("BK02", Bicicleta.Status.DISPONIVEL));
        bicicletaRepository.saveAndFlush(nova("BK03", Bicicleta.Status.DISPONIVEL));

        assertEquals(disponivel.getId(),
                bicicletaRepository.findFirstByStatusOrderByIdAsc(Bicicleta.Status.DISPONIVEL).orElseThrow().getId());
        assertTrue(bicicletaRepository.findFirstByStatusOrderByIdAsc(Bicicleta.Status.MANUTENCAO).isEmpty());
    }

    @Test
    void findByCodigoParaLocacaoLocalizaPeloCodigo() {
        Bicicleta salva = bicicletaRepository.saveAndFlush(nova("BK01"));

        assertEquals(salva.getId(), bicicletaRepository.findByCodigoParaLocacao("BK01").orElseThrow().getId());
        assertTrue(bicicletaRepository.findByCodigoParaLocacao("BK99").isEmpty());
    }

    private static Bicicleta nova(String codigo) {
        return nova(codigo, Bicicleta.Status.DISPONIVEL);
    }

    private static Bicicleta nova(String codigo, Bicicleta.Status status) {
        Bicicleta bicicleta = new Bicicleta();
        bicicleta.setCodigo(codigo);
        bicicleta.setModelo("Caloi Elite");
        bicicleta.setStatus(status);
        return bicicleta;
    }
}
