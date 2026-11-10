package br.edu.ifc.bikes.repository;

import br.edu.ifc.bikes.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    boolean existsByCpf(String cpf);

    boolean existsByUsuarioId(Long usuarioId);

    boolean existsByIdAndUsuarioId(Long id, Long usuarioId);
}
