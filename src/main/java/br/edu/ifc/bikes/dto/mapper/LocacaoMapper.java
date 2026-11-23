package br.edu.ifc.bikes.dto.mapper;

import br.edu.ifc.bikes.dto.LocacaoRelatorioDTO;
import br.edu.ifc.bikes.dto.LocacaoResponseDTO;
import br.edu.ifc.bikes.entity.Locacao;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Duration;
import java.util.List;

@Mapper(componentModel = "spring")
public interface LocacaoMapper {
    @Mapping(target = "clienteId", source = "cliente.id")
    @Mapping(target = "nomeCliente", source = "cliente.nome")
    @Mapping(target = "codigoBicicleta", source = "bicicleta.codigo")
    @Mapping(target = "status", source = "status", qualifiedByName = "statusNome")
    LocacaoResponseDTO toResponse(Locacao locacao);

    List<LocacaoResponseDTO> toResponse(List<Locacao> locacoes);

    @Mapping(target = "nomeCliente", source = "cliente.nome")
    @Mapping(target = "codigoBicicleta", source = "bicicleta.codigo")
    @Mapping(target = "duracaoMinutos", source = "locacao", qualifiedByName = "duracaoMinutos")
    @Mapping(target = "duracao", source = "locacao", qualifiedByName = "duracaoFormatada")
    @Mapping(target = "status", source = "status", qualifiedByName = "statusNome")
    LocacaoRelatorioDTO toRelatorio(Locacao locacao);

    List<LocacaoRelatorioDTO> toRelatorio(List<Locacao> locacoes);

    @Named("statusNome")
    default String mapStatus(Locacao.Status status) {
        if (status == null) {
            return null;
        }
        return status.name();
    }

    //a duração só é conhecida depois da devolução
    @Named("duracaoMinutos")
    default Long duracaoMinutos(Locacao locacao) {
        if (locacao.getDataRetirada() == null || locacao.getDataDevolucao() == null) {
            return null;
        }
        return Duration.between(locacao.getDataRetirada(), locacao.getDataDevolucao()).toMinutes();
    }

    @Named("duracaoFormatada")
    default String duracaoFormatada(Locacao locacao) {
        Long minutos = duracaoMinutos(locacao);
        if (minutos == null) {
            return "Em andamento";
        }
        return String.format("%dh %02dmin", minutos / 60, minutos % 60);
    }
}
