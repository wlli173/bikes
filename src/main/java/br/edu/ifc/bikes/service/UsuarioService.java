package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.dto.UsuarioRequestDTO;
import br.edu.ifc.bikes.dto.UsuarioResponseDTO;
import br.edu.ifc.bikes.dto.mapper.UsuarioMapper;
import br.edu.ifc.bikes.entity.Usuario;
import br.edu.ifc.bikes.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;

    public UsuarioResponseDTO create(UsuarioRequestDTO usuarioRequestDTO) {
        Usuario usuario = usuarioMapper.toUsuario(usuarioRequestDTO);
        Usuario salvo = usuarioRepository.save(usuario);
        log.info("Usuário criado: id={}", salvo.getId()); // e-mail (PII) não é logado
        return usuarioMapper.toResponse(salvo);
    }

    @Transactional()
    public UsuarioResponseDTO getById(Long id) {
        log.debug("Buscando usuário id={}", id);
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null) {
            log.warn("Usuário não encontrado: id={}", id);
        }
        return usuarioMapper.toResponse(usuario);
    }

    public UsuarioResponseDTO updatePassword(Long id, String password) {
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario != null) {
            usuario.setPassword(password);
            log.info("Senha atualizada: id={}", id);
            return usuarioMapper.toResponse(usuarioRepository.save(usuario));
        }
        log.warn("Atualização de senha para usuário inexistente: id={}", id);
        return null;
    }

    public List<UsuarioResponseDTO> getAll() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        log.debug("Listagem de usuários: {} registro(s)", usuarios.size());
        return usuarioMapper.toResponse(usuarios);
    }
}