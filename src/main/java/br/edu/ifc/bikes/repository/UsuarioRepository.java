package br.edu.ifc.bikes.repository;

import br.edu.ifc.bikes.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

}
