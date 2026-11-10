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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;
    private final UsuarioService usuarioService;

    public ClienteResponseDTO create(ClienteRequestDTO clienteRequestDTO) {
        Usuario usuario = usuarioAutenticado();
        if (clienteRepository.existsByUsuarioId(usuario.getId())) {
            log.warn("Usuário já possui cliente: usuarioId={}", usuario.getId());
            throw new ClienteUniqueViolationException("Usuário já possui um cliente cadastrado");
        }
        if (clienteRepository.existsByCpf(clienteRequestDTO.cpf())) {
            log.warn("CPF já cadastrado: usuarioId={}", usuario.getId());
            throw new ClienteUniqueViolationException(String.format("CPF '%s' já cadastrado", clienteRequestDTO.cpf()));
        }

        Cliente cliente = clienteMapper.toCliente(clienteRequestDTO);
        cliente.setUsuario(usuario);
        try {
            Cliente salvo = clienteRepository.save(cliente);
            log.info("Cliente criado: id={} usuarioId={}", salvo.getId(), usuario.getId());
            return clienteMapper.toResponse(salvo);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Violação de unicidade ao cadastrar cliente: usuarioId={}", usuario.getId());
            throw new ClienteUniqueViolationException("CPF já cadastrado ou usuário já possui um cliente");
        }
    }

    @Transactional
    public ClienteResponseDTO getById(Long id) {
        log.debug("Buscando cliente id={}", id);
        return clienteMapper.toResponse(clienteRepository.findById(id).orElseThrow(
                () -> {
                    log.warn("Cliente não encontrado: id={}", id);
                    return new EntityNotFoundException(String.format("Cliente com id=%d não encontrado", id));
                }
        ));
    }

    public List<ClienteResponseDTO> getAll() {
        List<Cliente> clientes = clienteRepository.findAll();
        log.debug("Listagem de clientes: {} registro(s)", clientes.size());
        return clienteMapper.toResponse(clientes);
    }

    private Usuario usuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof JwtUserDetails details)) {
            log.warn("Cadastro de cliente sem usuário autenticado");
            throw new AccessDeniedException("Usuário não autenticado");
        }
        return usuarioService.buscarPorNome(details.getUsername());
    }
}
