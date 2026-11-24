package br.edu.ifc.bikes.repository;

import br.edu.ifc.bikes.entity.Bicicleta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface BicicletaRepository extends JpaRepository<Bicicleta, Long> {
    Optional<Bicicleta> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    //bloqueio pessimista evita que duas locações simultâneas escolham a mesma bicicleta
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Bicicleta b WHERE b.codigo = :codigo")
    Optional<Bicicleta> findByCodigoParaLocacao(String codigo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Bicicleta> findFirstByStatusOrderByIdAsc(Bicicleta.Status status);
}
