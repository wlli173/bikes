package br.edu.ifc.bikes.source;

import br.edu.ifc.bikes.entity.Usuario;
import br.edu.ifc.bikes.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UsuarioSource {

    private final UsuarioRepository usuarioRepository;

    @Transactional
    public Usuario create(Usuario usuario){
        return usuarioRepository.save(usuario);
    }

}
