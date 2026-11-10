package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.dto.ClienteRequestDTO;
import br.edu.ifc.bikes.dto.ClienteResponseDTO;
import br.edu.ifc.bikes.dto.mapper.ClienteMapper;
import br.edu.ifc.bikes.entity.Cliente;
import br.edu.ifc.bikes.entity.Usuario;
import br.edu.ifc.bikes.exception.ClienteUniqueViolationException;
import br.edu.ifc.bikes.exception.EntityNotFoundException;
import br.edu.ifc.bikes.jwt.JwtUserDetails;
import br.edu.ifc.bikes.repository.ClienteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ClienteMapper clienteMapper;

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private ClienteService clienteService;

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createAssociaUsuarioAutenticado() {
        Usuario usuario = usuario(8L);
        autenticar(usuario);
        ClienteRequestDTO request = new ClienteRequestDTO("Ana Silva", "12345678901");
        Cliente cliente = new Cliente();
        ClienteResponseDTO response = new ClienteResponseDTO(3L, "Ana Silva", "12345678901");

        when(usuarioService.buscarPorNome("ana@bikes.com")).thenReturn(usuario);
        when(clienteRepository.existsByUsuarioId(8L)).thenReturn(false);
        when(clienteRepository.existsByCpf("12345678901")).thenReturn(false);
        when(clienteMapper.toCliente(request)).thenReturn(cliente);
        when(clienteRepository.save(cliente)).thenAnswer(invocation -> {
            Cliente salvo = invocation.getArgument(0);
            salvo.setId(3L);
            return salvo;
        });
        when(clienteMapper.toResponse(cliente)).thenReturn(response);

        assertEquals(response, clienteService.create(request));
        assertEquals(usuario, cliente.getUsuario());
    }

    @Test
    void createComUsuarioQueJaPossuiClienteLancaConflito() {
        Usuario usuario = usuario(8L);
        autenticar(usuario);
        when(usuarioService.buscarPorNome("ana@bikes.com")).thenReturn(usuario);
        when(clienteRepository.existsByUsuarioId(8L)).thenReturn(true);

        ClienteUniqueViolationException ex = assertThrows(ClienteUniqueViolationException.class,
                () -> clienteService.create(new ClienteRequestDTO("Ana Silva", "12345678901")));

        assertEquals("Usuário já possui um cliente cadastrado", ex.getMessage());
        verify(clienteRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createComCpfRepetidoLancaConflito() {
        Usuario usuario = usuario(8L);
        autenticar(usuario);
        when(usuarioService.buscarPorNome("ana@bikes.com")).thenReturn(usuario);
        when(clienteRepository.existsByUsuarioId(8L)).thenReturn(false);
        when(clienteRepository.existsByCpf("12345678901")).thenReturn(true);

        ClienteUniqueViolationException ex = assertThrows(ClienteUniqueViolationException.class,
                () -> clienteService.create(new ClienteRequestDTO("Ana Silva", "12345678901")));

        assertEquals("CPF '12345678901' já cadastrado", ex.getMessage());
        verify(clienteRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createSemAutenticacaoLancaAcessoNegado() {
        assertThrows(AccessDeniedException.class,
                () -> clienteService.create(new ClienteRequestDTO("Ana Silva", "12345678901")));
        verify(clienteRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getByIdDevolveClienteEncontrado() {
        Cliente cliente = new Cliente();
        cliente.setId(4L);
        ClienteResponseDTO response = new ClienteResponseDTO(4L, "Ana Silva", "12345678901");
        when(clienteRepository.findById(4L)).thenReturn(Optional.of(cliente));
        when(clienteMapper.toResponse(cliente)).thenReturn(response);

        assertEquals(response, clienteService.getById(4L));
    }

    @Test
    void getByIdInexistenteLancaEntityNotFound() {
        when(clienteRepository.findById(9L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> clienteService.getById(9L));

        assertEquals("Cliente com id=9 não encontrado", ex.getMessage());
    }

    @Test
    void getAllMapeiaClientesDoRepositorio() {
        Cliente cliente = new Cliente();
        List<Cliente> clientes = List.of(cliente);
        List<ClienteResponseDTO> resposta = List.of(new ClienteResponseDTO(1L, "Ana Silva", "12345678901"));
        when(clienteRepository.findAll()).thenReturn(clientes);
        when(clienteMapper.toResponse(clientes)).thenReturn(resposta);

        assertEquals(resposta, clienteService.getAll());
    }

    @Test
    void getAllSemRegistrosDevolveListaVazia() {
        List<Cliente> vazia = List.of();
        when(clienteRepository.findAll()).thenReturn(vazia);
        when(clienteMapper.toResponse(vazia)).thenReturn(List.of());

        assertTrue(clienteService.getAll().isEmpty());
    }

    private static Usuario usuario(Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setUsername("ana@bikes.com");
        usuario.setPassword("hash");
        usuario.setRole(Usuario.Role.ROLE_CLIENTE);
        return usuario;
    }

    private static void autenticar(Usuario usuario) {
        JwtUserDetails details = new JwtUserDetails(usuario);
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                details, null, details.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
