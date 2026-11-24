package br.edu.ifc.bikes.repository;

import br.edu.ifc.bikes.entity.Locacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LocacaoRepository extends JpaRepository<Locacao, Long> {
    boolean existsByBicicletaIdAndStatus(Long bicicletaId, Locacao.Status status);

    boolean existsByIdAndClienteUsuarioId(Long id, Long usuarioId);

    List<Locacao> findAllByOrderByDataRetiradaDesc();
}
