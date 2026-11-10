package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.dto.UsuarioRequestDTO;
import br.edu.ifc.bikes.dto.UsuarioResponseDTO;
import br.edu.ifc.bikes.dto.mapper.UsuarioMapper;
import br.edu.ifc.bikes.entity.Usuario;
import br.edu.ifc.bikes.exception.EntityNotFoundException;
import br.edu.ifc.bikes.exception.PasswordInvalidException;
import br.edu.ifc.bikes.exception.UsernameUniqueViolationException;
import br.edu.ifc.bikes.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioMapper usuarioMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void createCodificaSenhaEDevolveResposta() {
        UsuarioRequestDTO request = new UsuarioRequestDTO("ana@bikes.com", "123456");
        Usuario usuario = usuario(null, "ana@bikes.com", "123456");
        UsuarioResponseDTO response = new UsuarioResponseDTO(10L, "ana@bikes.com", "CLIENTE");

        when(usuarioMapper.toUsuario(request)).thenReturn(usuario);
        when(passwordEncoder.encode("123456")).thenReturn("hash");
        when(usuarioRepository.save(usuario)).thenAnswer(invocation -> {
            Usuario salvo = invocation.getArgument(0);
            salvo.setId(10L);
            return salvo;
        });
        when(usuarioMapper.toResponse(usuario)).thenReturn(response);

        assertEquals(response, usuarioService.create(request));
        assertEquals("hash", usuario.getPassword());
    }

    @Test
    void createComUsernameRepetidoLancaViolacaoDeUnicidade() {
        UsuarioRequestDTO request = new UsuarioRequestDTO("ana@bikes.com", "123456");
        Usuario usuario = usuario(null, "ana@bikes.com", "123456");
        when(usuarioMapper.toUsuario(request)).thenReturn(usuario);
        when(passwordEncoder.encode("123456")).thenReturn("hash");
        when(usuarioRepository.save(usuario)).thenThrow(new RuntimeException("duplicado"));

        UsernameUniqueViolationException ex = assertThrows(
                UsernameUniqueViolationException.class, () -> usuarioService.create(request));

        assertEquals("O nome do usuário 'ana@bikes.com' já existe", ex.getMessage());
    }

    @Test
    void getByIdDevolveUsuarioEncontrado() {
        Usuario usuario = usuario(4L, "ana@bikes.com", "hash");
        UsuarioResponseDTO response = new UsuarioResponseDTO(4L, "ana@bikes.com", "CLIENTE");
        when(usuarioRepository.findById(4L)).thenReturn(Optional.of(usuario));
        when(usuarioMapper.toResponse(usuario)).thenReturn(response);

        assertEquals(response, usuarioService.getById(4L));
    }

    @Test
    void getByIdInexistenteLancaEntityNotFound() {
        when(usuarioRepository.findById(9L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(
                EntityNotFoundException.class, () -> usuarioService.getById(9L));

        assertEquals("Usuário com id=9 não encontrado", ex.getMessage());
    }

    @Test
    void updatePasswordCodificaNovaSenhaQuandoDadosConferem() {
        Usuario usuario = usuario(3L, "ana@bikes.com", "hash-atual");
        UsuarioResponseDTO response = new UsuarioResponseDTO(3L, "ana@bikes.com", "CLIENTE");
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("123456", "hash-atual")).thenReturn(true);
        when(passwordEncoder.encode("654321")).thenReturn("hash-nova");
        when(usuarioMapper.toResponse(usuario)).thenReturn(response);

        assertEquals(response, usuarioService.updatePassword(3L, "654321", "654321", "123456"));
        assertEquals("hash-nova", usuario.getPassword());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void updatePasswordDeUsuarioInexistenteLancaEntityNotFound() {
        when(usuarioRepository.findById(9L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () ->
                usuarioService.updatePassword(9L, "654321", "654321", "123456"));

        assertEquals("Usuário 9 não encontrado", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void updatePasswordComSenhaAtualIncorretaLancaPasswordInvalid() {
        Usuario usuario = usuario(3L, "ana@bikes.com", "hash-atual");
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("000000", "hash-atual")).thenReturn(false);

        PasswordInvalidException ex = assertThrows(PasswordInvalidException.class, () ->
                usuarioService.updatePassword(3L, "654321", "654321", "000000"));

        assertEquals("Senha atual incorreta", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void updatePasswordComConfirmacaoDiferenteLancaPasswordInvalid() {
        PasswordInvalidException ex = assertThrows(PasswordInvalidException.class, () ->
                usuarioService.updatePassword(3L, "654321", "111111", "123456"));

        assertEquals("As senhas não coincidem", ex.getMessage());
        verify(usuarioRepository, never()).findById(any());
    }

    @Test
    void buscarPorNomeDevolveUsuario() {
        Usuario usuario = usuario(1L, "ana@bikes.com", "hash");
        when(usuarioRepository.findByUsername("ana@bikes.com")).thenReturn(Optional.of(usuario));

        assertEquals(usuario, usuarioService.buscarPorNome("ana@bikes.com"));
    }

    @Test
    void buscarPorNomeInexistenteLancaEntityNotFound() {
        when(usuarioRepository.findByUsername("ana@bikes.com")).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> usuarioService.buscarPorNome("ana@bikes.com"));

        assertEquals("Usuário 'ana@bikes.com' não encontrado", ex.getMessage());
    }

    @Test
    void buscarRolePorUsernameDelegaAoRepositorio() {
        when(usuarioRepository.findRoleByUsername("ana@bikes.com")).thenReturn(Usuario.Role.ROLE_ADMIN);

        assertEquals(Usuario.Role.ROLE_ADMIN, usuarioService.buscarRolePorUsername("ana@bikes.com"));
    }

    @Test
    void getAllMapeiaUsuariosDoRepositorio() {
        Usuario usuario = usuario(1L, "ana@bikes.com", "hash");
        List<Usuario> usuarios = List.of(usuario);
        List<UsuarioResponseDTO> resposta = List.of(new UsuarioResponseDTO(1L, "ana@bikes.com", "CLIENTE"));
        when(usuarioRepository.findAll()).thenReturn(usuarios);
        when(usuarioMapper.toResponse(usuarios)).thenReturn(resposta);

        assertEquals(resposta, usuarioService.getAll());
    }

    @Test
    void getAllSemRegistrosDevolveListaVazia() {
        List<Usuario> vazia = List.of();
        when(usuarioRepository.findAll()).thenReturn(vazia);
        when(usuarioMapper.toResponse(vazia)).thenReturn(List.of());

        assertTrue(usuarioService.getAll().isEmpty());
    }

    private static Usuario usuario(Long id, String username, String password) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setUsername(username);
        usuario.setPassword(password);
        return usuario;
    }
}
