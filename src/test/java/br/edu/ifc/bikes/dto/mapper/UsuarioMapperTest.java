package br.edu.ifc.bikes.dto.mapper;

import br.edu.ifc.bikes.dto.UsuarioRequestDTO;
import br.edu.ifc.bikes.dto.UsuarioResponseDTO;
import br.edu.ifc.bikes.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UsuarioMapperTest {

    private final UsuarioMapper mapper = Mappers.getMapper(UsuarioMapper.class);

    @Test
    void toUsuarioCopiaCredenciaisEMantemRolePadrao() {
        Usuario usuario = mapper.toUsuario(new UsuarioRequestDTO("ana@bikes.com", "123456"));

        assertNull(usuario.getId());
        assertEquals("ana@bikes.com", usuario.getUsername());
        assertEquals("123456", usuario.getPassword());
        assertEquals(Usuario.Role.CLIENTE, usuario.getRole());
    }

    @Test
    void toResponseExpoeIdUsernameERole() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setUsername("ana@bikes.com");
        usuario.setPassword("123456");
        usuario.setRole(Usuario.Role.ADMIN);

        UsuarioResponseDTO response = mapper.toResponse(usuario);

        assertEquals(7L, response.id());
        assertEquals("ana@bikes.com", response.username());
        assertEquals("ADMIN", response.role());
    }

    @Test
    void toResponseMapeiaLista() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("ana@bikes.com");
        usuario.setRole(Usuario.Role.CLIENTE);

        List<UsuarioResponseDTO> respostas = mapper.toResponse(List.of(usuario));

        assertEquals(1, respostas.size());
        assertEquals(new UsuarioResponseDTO(1L, "ana@bikes.com", "CLIENTE"), respostas.getFirst());
    }
}
