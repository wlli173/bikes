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
        try {
            Usuario usuario = usuarioMapper.toUsuario(usuarioRequestDTO);
            Usuario salvo = usuarioRepository.save(usuario);
            log.info("Usuário criado: id={}", salvo.getId()); // e-mail (PII) não é logado
            return usuarioMapper.toResponse(salvo);
        } catch (RuntimeException ex) {
            log.warn("Nome de usuário já cadastrado");
            throw new UsernameUniqueViolationException(
                    String.format("O nome do usuário '%s' já existe", usuarioRequestDTO.username()));
        }
    }

    @Transactional()
    public UsuarioResponseDTO getById(Long id) {
        log.debug("Buscando usuário id={}", id);
        return usuarioMapper.toResponse(usuarioRepository.findById(id).orElseThrow(
                () -> {
                    log.warn("Usuário não encontrado: id={}", id);
                    return new EntityNotFoundException(String.format("Usuário com id=%d não encontrado", id));
                }
        ));
    }

    public void updatePassword(Long id, UsuarioSenhaDTO dto) {
        Usuario usuario = usuarioRepository.findById(id).orElseThrow(
                () -> {
                    log.warn("Atualização de senha para usuário inexistente: id={}", id);
                    return new EntityNotFoundException(String.format("Usuário com id=%d não encontrado", id));
                }
        );

        if (!usuario.getPassword().equals(dto.senhaAtual())) {
            log.warn("Senha atual não confere: id={}", id);
            throw new PasswordInvalidException("A senha atual não confere");
        }

        if (!dto.novaSenha().equals(dto.confirmaSenha())) {
            log.warn("Nova senha difere da confirmação: id={}", id);
            throw new PasswordInvalidException("A nova senha não confere com a confirmação de senha");
        }

        usuario.setPassword(dto.novaSenha());
        usuarioRepository.save(usuario);
        log.info("Senha atualizada: id={}", id);
    }

    public List<UsuarioResponseDTO> getAll() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        log.debug("Listagem de usuários: {} registro(s)", usuarios.size());
        return usuarioMapper.toResponse(usuarios);
    }
}
