package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.dto.UsuarioRequestDTO;
import br.edu.ifc.bikes.dto.UsuarioResponseDTO;
import br.edu.ifc.bikes.dto.UsuarioSenhaDTO;
import br.edu.ifc.bikes.exception.EntityNotFoundException;
import br.edu.ifc.bikes.exception.PasswordInvalidException;
import br.edu.ifc.bikes.exception.UsernameUniqueViolationException;
import br.edu.ifc.bikes.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class UsuarioServiceIT {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void createPersisteUsuarioCliente() {
        String email = emailUnico();

        UsuarioResponseDTO criado = usuarioService.create(new UsuarioRequestDTO(email, "123456"));

        assertEquals(email, criado.username());
        assertEquals("CLIENTE", criado.role());
        assertTrue(usuarioRepository.findById(criado.id()).isPresent());
    }

    @Test
    void createComUsernameRepetidoLancaViolacao() {
        String email = emailUnico();
        usuarioService.create(new UsuarioRequestDTO(email, "123456"));

        UsernameUniqueViolationException ex = assertThrows(UsernameUniqueViolationException.class,
                () -> usuarioService.create(new UsuarioRequestDTO(email, "123456")));

        assertEquals("O nome do usuário '" + email + "' já existe", ex.getMessage());
    }

    @Test
    void getByIdRecuperaUsuarioPersistido() {
        String email = emailUnico();
        Long id = usuarioService.create(new UsuarioRequestDTO(email, "123456")).id();

        assertEquals(email, usuarioService.getById(id).username());
    }

    @Test
    void getByIdInexistenteLancaEntityNotFound() {
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> usuarioService.getById(999_999L));

        assertEquals("Usuário com id=999999 não encontrado", ex.getMessage());
    }

    @Test
    void updatePasswordPersisteNovaSenha() {
        Long id = usuarioService.create(new UsuarioRequestDTO(emailUnico(), "123456")).id();

        usuarioService.updatePassword(id, new UsuarioSenhaDTO("123456", "654321", "654321"));

        assertEquals("654321", usuarioRepository.findById(id).orElseThrow().getPassword());
    }

    @Test
    void updatePasswordRejeitaSenhaAtualEConfirmacao() {
        Long id = usuarioService.create(new UsuarioRequestDTO(emailUnico(), "123456")).id();

        assertThrows(PasswordInvalidException.class,
                () -> usuarioService.updatePassword(id, new UsuarioSenhaDTO("000000", "654321", "654321")));
        assertThrows(PasswordInvalidException.class,
                () -> usuarioService.updatePassword(id, new UsuarioSenhaDTO("123456", "654321", "111111")));
        assertEquals("123456", usuarioRepository.findById(id).orElseThrow().getPassword());
    }

    @Test
    void getAllIncluiUsuarioCriado() {
        String email = emailUnico();
        usuarioService.create(new UsuarioRequestDTO(email, "123456"));

        assertTrue(usuarioService.getAll().stream().anyMatch(usuario -> email.equals(usuario.username())));
    }

    private static String emailUnico() {
        return "svc-" + UUID.randomUUID() + "@bikes.com";
    }
}
