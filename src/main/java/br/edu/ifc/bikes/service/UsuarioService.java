package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.dto.UsuarioRequestDTO;
import br.edu.ifc.bikes.dto.UsuarioResponseDTO;
import br.edu.ifc.bikes.dto.mappper.UsuarioMapper;
import br.edu.ifc.bikes.entity.Usuario;
import br.edu.ifc.bikes.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;

    @Transactional
    public UsuarioResponseDTO create(UsuarioRequestDTO usuario) {
        return usuarioMapper.toUsuarioResponseDTO(usuarioRepository.save(usuarioMapper.toUsuario(usuario)));
    }

    @Transactional()
    public UsuarioResponseDTO getbyId(Long id){
        return usuarioMapper.toUsuarioResponseDTO(usuarioRepository.findById(id).orElse(null));
    }

    @Transactional()
    public List<UsuarioResponseDTO> getAll(){
        return usuarioMapper.toUsuarioResponseDTO(usuarioRepository.findAll());
    }

    @Transactional
    public UsuarioResponseDTO updatePassword(Long id, String password){
        Usuario usuario = usuarioRepository.findById(id).orElse(null);

        if(usuario != null){
            usuario.setPassword(password);
            return usuarioMapper.toUsuarioResponseDTO(usuarioRepository.save(usuario));
        }

        return null;

    }

}
