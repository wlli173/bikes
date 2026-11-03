package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.dto.UsuarioRequestDTO;
import br.edu.ifc.bikes.dto.UsuarioResponseDTO;
import br.edu.ifc.bikes.exception.EntityNotFoundException;
import br.edu.ifc.bikes.exception.PasswordInvalidException;
import br.edu.ifc.bikes.exception.UsernameUniqueViolationException;
import br.edu.ifc.bikes.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    void updatePasswordPersisteNovaSenhaCodificada() {
        Long id = usuarioService.create(new UsuarioRequestDTO(emailUnico(), "123456")).id();

        UsuarioResponseDTO atualizado = usuarioService.updatePassword(id, "654321", "654321", "123456");

        assertEquals(id, atualizado.id());
        assertTrue(passwordEncoder.matches("654321", usuarioRepository.findById(id).orElseThrow().getPassword()));
    }

    @Test
    void updatePasswordRejeitaSenhaAtualEConfirmacao() {
        Long id = usuarioService.create(new UsuarioRequestDTO(emailUnico(), "123456")).id();

        PasswordInvalidException senhaAtual = assertThrows(PasswordInvalidException.class,
                () -> usuarioService.updatePassword(id, "654321", "654321", "000000"));
        PasswordInvalidException confirmacao = assertThrows(PasswordInvalidException.class,
                () -> usuarioService.updatePassword(id, "654321", "111111", "123456"));

        assertEquals("Senha atual incorreta", senhaAtual.getMessage());
        assertEquals("As senhas não coincidem", confirmacao.getMessage());
        assertTrue(passwordEncoder.matches("123456", usuarioRepository.findById(id).orElseThrow().getPassword()));
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
