package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.dto.UsuarioRequestDTO;
import br.edu.ifc.bikes.dto.UsuarioResponseDTO;
import br.edu.ifc.bikes.dto.mapper.UsuarioMapper;
import br.edu.ifc.bikes.entity.Usuario;
import br.edu.ifc.bikes.exception.EntityNotFoundException;
import br.edu.ifc.bikes.exception.PasswordInvalidException;
import br.edu.ifc.bikes.exception.UsernameUniqueViolationException;
import br.edu.ifc.bikes.jwt.JwtAuthorizationFilter;
import br.edu.ifc.bikes.jwt.JwtUserDetailsService;
import br.edu.ifc.bikes.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;

    public UsuarioResponseDTO create(UsuarioRequestDTO usuarioRequestDTO) {
        try {
            Usuario usuario = usuarioMapper.toUsuario(usuarioRequestDTO);
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            Usuario salvo = usuarioRepository.save(usuario);
            log.info("Usuário criado: id={}", salvo.getId()); // e-mail (PII) não é logado
            return usuarioMapper.toResponse(salvo);
        }catch (RuntimeException ex){
            log.warn("Nome de usuário já cadastrado");
            throw new UsernameUniqueViolationException(String.format("O nome do usuário '%s' já existe", usuarioRequestDTO.username()));
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

    public UsuarioResponseDTO updatePassword(Long id, String novaSenha, String confirmaSenha, String senhaAtual) {
        if (!novaSenha.equals(confirmaSenha)){
            log.warn("Nova senha difere da confirmação: id={}", id);
            throw new PasswordInvalidException("As senhas não coincidem");
        }
        Usuario usuario = usuarioRepository.findById(id).orElseThrow(
                () -> {
                    log.warn("Atualização de senha para usuário inexistente: id={}", id);
                    return new EntityNotFoundException(String.format("Usuário %s não encontrado", id));
                }
        );
        if (!passwordEncoder.matches(senhaAtual, usuario.getPassword())){
            log.warn("Senha atual não confere: id={}", id);
            throw new PasswordInvalidException("Senha atual incorreta");
        }
        usuario.setPassword(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
        log.info("Senha atualizada: id={}", id);
        return usuarioMapper.toResponse(usuario);
    }

    public List<UsuarioResponseDTO> getAll(){
        List<Usuario> usuarios = usuarioRepository.findAll();
        log.debug("Listagem de usuários: {} registro(s)", usuarios.size());
        return usuarioMapper.toResponse(usuarios);
    }

    @Transactional()
    public Usuario buscarPorNome(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(()->new EntityNotFoundException(String.format("Usuário '%s' não encontrado", username)));
    }

    public Usuario.Role buscarRolePorUsername(String username){
        return usuarioRepository.findRoleByUsername(username);
    }

    @Bean
    public JwtAuthorizationFilter jwtAuthorizationFilter(JwtUserDetailsService detailsService){
        return new JwtAuthorizationFilter(detailsService);
    }
}
