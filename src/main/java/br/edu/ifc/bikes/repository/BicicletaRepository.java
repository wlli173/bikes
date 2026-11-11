package br.edu.ifc.bikes.repository;

import br.edu.ifc.bikes.entity.Bicicleta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BicicletaRepository extends JpaRepository<Bicicleta, Long> {
    Optional<Bicicleta> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);
}
