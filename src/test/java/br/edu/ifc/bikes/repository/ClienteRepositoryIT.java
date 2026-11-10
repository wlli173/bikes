package br.edu.ifc.bikes.repository;

import br.edu.ifc.bikes.entity.Cliente;
import br.edu.ifc.bikes.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class ClienteRepositoryIT {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void saveAssociaUsuarioEPermiteConsultaPeloDono() {
        Usuario usuario = usuarioRepository.saveAndFlush(novoUsuario());
        Cliente salvo = clienteRepository.saveAndFlush(novoCliente(usuario, "12345678901"));

        assertTrue(salvo.getId() != null && salvo.getId() > 0);
        assertTrue(clienteRepository.existsByCpf("12345678901"));
        assertTrue(clienteRepository.existsByUsuarioId(usuario.getId()));
        assertTrue(clienteRepository.existsByIdAndUsuarioId(salvo.getId(), usuario.getId()));
        assertFalse(clienteRepository.existsByIdAndUsuarioId(salvo.getId(), usuario.getId() + 1));
    }

    @Test
    void cpfDuplicadoViolentaRestricaoUnica() {
        Usuario primeiro = usuarioRepository.saveAndFlush(novoUsuario());
        Usuario segundo = usuarioRepository.saveAndFlush(novoUsuario());
        clienteRepository.saveAndFlush(novoCliente(primeiro, "12345678901"));

        assertThrows(DataIntegrityViolationException.class,
                () -> clienteRepository.saveAndFlush(novoCliente(segundo, "12345678901")));
    }

    @Test
    void usuarioDuplicadoViolentaRestricaoUnica() {
        Usuario usuario = usuarioRepository.saveAndFlush(novoUsuario());
        clienteRepository.saveAndFlush(novoCliente(usuario, "12345678901"));

        assertThrows(DataIntegrityViolationException.class,
                () -> clienteRepository.saveAndFlush(novoCliente(usuario, "10987654321")));
    }

    private static Usuario novoUsuario() {
        Usuario usuario = new Usuario();
        usuario.setUsername("repo-" + UUID.randomUUID() + "@bikes.com");
        usuario.setPassword("123456");
        return usuario;
    }

    private static Cliente novoCliente(Usuario usuario, String cpf) {
        Cliente cliente = new Cliente();
        cliente.setNome("Ana Silva");
        cliente.setCpf(cpf);
        cliente.setUsuario(usuario);
        return cliente;
    }
}
