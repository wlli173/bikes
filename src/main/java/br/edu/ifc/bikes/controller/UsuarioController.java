package br.edu.ifc.bikes.controller;


import br.edu.ifc.bikes.dto.UsuarioRequestDTO;
import br.edu.ifc.bikes.dto.UsuarioResponseDTO;
import br.edu.ifc.bikes.entity.Usuario;
import br.edu.ifc.bikes.service.UsuarioService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> create(@RequestBody UsuarioRequestDTO usuario){
        UsuarioResponseDTO usuarioCriado = usuarioService.create(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioCriado);
    }

    @GetMapping("/id")
    public ResponseEntity<UsuarioResponseDTO> getById(@PathVariable Long id){
        UsuarioResponseDTO usuario = usuarioService.getbyId(id);

        if(usuario != null){
            return ResponseEntity.ok(usuario);
        }else{
            return ResponseEntity.notFound().build();
        }

    }
    
    // PATCH deve ser utilizado no lugar do PUT quando a atualização for de poucos campos. 
    @PatchMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> updatePassword(@PathVariable Long id, @RequestBody Usuario usuario){
        
        UsuarioResponseDTO updateUsuario = usuarioService.updatePassword(id, usuario.getPassword());
        
        if(updateUsuario != null){
            return ResponseEntity.ok(updateUsuario);
        }else {
            return ResponseEntity.notFound().build();
        }
        
    }

    @Transactional
    public ResponseEntity<UsuarioResponseDTO> getAll(){
        List<UsuarioResponseDTO> usuarios = usuarioService.getAll();
        return ResponseEntity.ok(usuarios);
    }

}
