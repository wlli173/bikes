package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.entity.Usuario;
import br.edu.ifc.bikes.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    private UsuarioRepository usuarioRepository;

    @Transactional
    public Usuario create(Usuario usuario){
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario getbyId(Long id){
        return usuarioRepository.findById(id).get();
    }

}
