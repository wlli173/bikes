package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.dto.UsuarioRequestDTO;
import br.edu.ifc.bikes.dto.UsuarioResponseDTO;
import br.edu.ifc.bikes.dto.UsuarioSenhaDTO;
import br.edu.ifc.bikes.dto.mapper.UsuarioMapper;
import br.edu.ifc.bikes.entity.Usuario;
import br.edu.ifc.bikes.exception.EntityNotFoundException;
import br.edu.ifc.bikes.exception.PasswordInvalidException;
import br.edu.ifc.bikes.exception.UsernameUniqueViolationException;
import br.edu.ifc.bikes.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void createPersisteEDevolveResposta() {
        UsuarioRequestDTO request = new UsuarioRequestDTO("ana@bikes.com", "123456");
        Usuario usuario = usuario(null, "ana@bikes.com", "123456");
        Usuario salvo = usuario(10L, "ana@bikes.com", "123456");
        UsuarioResponseDTO response = new UsuarioResponseDTO(10L, "ana@bikes.com", "CLIENTE");

        when(usuarioMapper.toUsuario(request)).thenReturn(usuario);
        when(usuarioRepository.save(usuario)).thenReturn(salvo);
        when(usuarioMapper.toResponse(salvo)).thenReturn(response);

        assertEquals(response, usuarioService.create(request));
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void createComUsernameRepetidoLancaViolacaoDeUnicidade() {
        UsuarioRequestDTO request = new UsuarioRequestDTO("ana@bikes.com", "123456");
        Usuario usuario = usuario(null, "ana@bikes.com", "123456");
        when(usuarioMapper.toUsuario(request)).thenReturn(usuario);
        when(usuarioRepository.save(usuario)).thenThrow(new RuntimeException("duplicado"));

        UsernameUniqueViolationException ex = assertThrows(
                UsernameUniqueViolationException.class, () -> usuarioService.create(request));

        assertEquals("O nome do usuário 'ana@bikes.com' já existe", ex.getMessage());
    }

    @Test
    void getByIdDevolveUsuarioEncontrado() {
        Usuario usuario = usuario(4L, "ana@bikes.com", "123456");
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
    void updatePasswordAlteraSenhaQuandoDadosConferem() {
        Usuario usuario = usuario(3L, "ana@bikes.com", "123456");
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(usuario));

        usuarioService.updatePassword(3L, new UsuarioSenhaDTO("123456", "654321", "654321"));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("654321", captor.getValue().getPassword());
    }

    @Test
    void updatePasswordDeUsuarioInexistenteLancaEntityNotFound() {
        when(usuarioRepository.findById(9L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () ->
                usuarioService.updatePassword(9L, new UsuarioSenhaDTO("123456", "654321", "654321")));

        assertEquals("Usuário com id=9 não encontrado", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void updatePasswordComSenhaAtualIncorretaLancaPasswordInvalid() {
        Usuario usuario = usuario(3L, "ana@bikes.com", "123456");
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(usuario));

        PasswordInvalidException ex = assertThrows(PasswordInvalidException.class, () ->
                usuarioService.updatePassword(3L, new UsuarioSenhaDTO("000000", "654321", "654321")));

        assertEquals("A senha atual não confere", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void updatePasswordComConfirmacaoDiferenteLancaPasswordInvalid() {
        Usuario usuario = usuario(3L, "ana@bikes.com", "123456");
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(usuario));

        PasswordInvalidException ex = assertThrows(PasswordInvalidException.class, () ->
                usuarioService.updatePassword(3L, new UsuarioSenhaDTO("123456", "654321", "111111")));

        assertEquals("A nova senha não confere com a confirmação de senha", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void getAllMapeiaUsuariosDoRepositorio() {
        Usuario usuario = usuario(1L, "ana@bikes.com", "123456");
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
