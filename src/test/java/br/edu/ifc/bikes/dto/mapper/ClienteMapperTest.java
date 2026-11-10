package br.edu.ifc.bikes.dto.mapper;

import br.edu.ifc.bikes.dto.ClienteRequestDTO;
import br.edu.ifc.bikes.dto.ClienteResponseDTO;
import br.edu.ifc.bikes.entity.Cliente;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ClienteMapperTest {

    private final ClienteMapper mapper = Mappers.getMapper(ClienteMapper.class);

    @Test
    void toClienteCopiaNomeECpf() {
        Cliente cliente = mapper.toCliente(new ClienteRequestDTO("Ana Silva", "12345678901"));

        assertNull(cliente.getId());
        assertNull(cliente.getUsuario());
        assertEquals("Ana Silva", cliente.getNome());
        assertEquals("12345678901", cliente.getCpf());
    }

    @Test
    void toResponseExpoeIdNomeECpf() {
        Cliente cliente = new Cliente();
        cliente.setId(4L);
        cliente.setNome("Ana Silva");
        cliente.setCpf("12345678901");

        ClienteResponseDTO response = mapper.toResponse(cliente);

        assertEquals(new ClienteResponseDTO(4L, "Ana Silva", "12345678901"), response);
    }

    @Test
    void toResponseMapeiaLista() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Ana Silva");
        cliente.setCpf("12345678901");

        List<ClienteResponseDTO> respostas = mapper.toResponse(List.of(cliente));

        assertEquals(1, respostas.size());
        assertEquals(new ClienteResponseDTO(1L, "Ana Silva", "12345678901"), respostas.getFirst());
    }

    @Test
    void equalsConsideraApenasId() {
        Cliente primeiro = new Cliente();
        primeiro.setId(1L);
        primeiro.setCpf("12345678901");

        Cliente segundo = new Cliente();
        segundo.setId(1L);
        segundo.setCpf("10987654321");

        Cliente outro = new Cliente();
        outro.setId(2L);

        assertEquals(primeiro, segundo);
        assertEquals(false, primeiro.equals(outro));
        assertEquals(false, primeiro.equals(null));
    }
}
