package br.edu.ifc.bikes.dto.mapper;

import br.edu.ifc.bikes.dto.BicicletaRequestDTO;
import br.edu.ifc.bikes.dto.BicicletaResponseDTO;
import br.edu.ifc.bikes.entity.Bicicleta;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BicicletaMapperTest {

    private final BicicletaMapper mapper = Mappers.getMapper(BicicletaMapper.class);

    @Test
    void toBicicletaCopiaCodigoModeloEStatus() {
        Bicicleta bicicleta = mapper.toBicicleta(new BicicletaRequestDTO("BK01", "Caloi Elite", "DISPONIVEL"));

        assertNull(bicicleta.getId());
        assertEquals("BK01", bicicleta.getCodigo());
        assertEquals("Caloi Elite", bicicleta.getModelo());
        assertEquals(Bicicleta.Status.DISPONIVEL, bicicleta.getStatus());
    }

    @Test
    void toResponseExpoeIdCodigoModeloEStatus() {
        Bicicleta bicicleta = new Bicicleta();
        bicicleta.setId(4L);
        bicicleta.setCodigo("BK01");
        bicicleta.setModelo("Caloi Elite");
        bicicleta.setStatus(Bicicleta.Status.ALUGADA);

        assertEquals(new BicicletaResponseDTO(4L, "BK01", "Caloi Elite", "ALUGADA"), mapper.toResponse(bicicleta));
    }

    @Test
    void toResponseMapeiaLista() {
        Bicicleta bicicleta = new Bicicleta();
        bicicleta.setId(1L);
        bicicleta.setCodigo("BK01");
        bicicleta.setModelo("Caloi Elite");
        bicicleta.setStatus(Bicicleta.Status.MANUTENCAO);

        List<BicicletaResponseDTO> respostas = mapper.toResponse(List.of(bicicleta));

        assertEquals(1, respostas.size());
        assertEquals(new BicicletaResponseDTO(1L, "BK01", "Caloi Elite", "MANUTENCAO"), respostas.getFirst());
    }

    @Test
    void equalsConsideraApenasId() {
        Bicicleta primeira = new Bicicleta();
        primeira.setId(1L);
        primeira.setCodigo("BK01");

        Bicicleta segunda = new Bicicleta();
        segunda.setId(1L);
        segunda.setCodigo("BK02");

        Bicicleta outra = new Bicicleta();
        outra.setId(2L);

        assertEquals(primeira, segunda);
        assertEquals(false, primeira.equals(outra));
        assertEquals(false, primeira.equals(null));
    }
}
