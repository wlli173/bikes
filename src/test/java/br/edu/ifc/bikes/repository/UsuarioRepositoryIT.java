package br.edu.ifc.bikes.repository;

import br.edu.ifc.bikes.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class UsuarioRepositoryIT {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void saveAtribuiIdERolePadrao() {
        Usuario salvo = usuarioRepository.saveAndFlush(novoUsuario());

        assertTrue(salvo.getId() != null && salvo.getId() > 0);
        assertEquals(Usuario.Role.CLIENTE, usuarioRepository.findById(salvo.getId()).orElseThrow().getRole());
    }

    @Test
    void usernameDuplicadoViolentaRestricaoUnica() {
        Usuario primeiro = novoUsuario();
        usuarioRepository.saveAndFlush(primeiro);

        Usuario repetido = novoUsuario();
        repetido.setUsername(primeiro.getUsername());

        assertThrows(DataIntegrityViolationException.class, () -> usuarioRepository.saveAndFlush(repetido));
    }

    @Test
    void findByUsernameERole() {
        Usuario salvo = usuarioRepository.saveAndFlush(novoUsuario());

        assertEquals(salvo.getId(), usuarioRepository.findByUsername(salvo.getUsername()).orElseThrow().getId());
        assertEquals(Usuario.Role.CLIENTE, usuarioRepository.findRoleByUsername(salvo.getUsername()));
    }

    private static Usuario novoUsuario() {
        Usuario usuario = new Usuario();
        usuario.setUsername("repo-" + UUID.randomUUID() + "@bikes.com");
        usuario.setPassword("123456");
        return usuario;
    }
}
