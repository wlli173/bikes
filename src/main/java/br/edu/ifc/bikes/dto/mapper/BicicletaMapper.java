package br.edu.ifc.bikes.dto.mapper;

import br.edu.ifc.bikes.dto.BicicletaRequestDTO;
import br.edu.ifc.bikes.dto.BicicletaResponseDTO;
import br.edu.ifc.bikes.entity.Bicicleta;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BicicletaMapper {
    @Mapping(target = "status", source = "status", qualifiedByName = "status")
    Bicicleta toBicicleta(BicicletaRequestDTO bicicletaRequestDTO);

    @Mapping(target = "status", source = "status", qualifiedByName = "statusNome")
    BicicletaResponseDTO toResponse(Bicicleta bicicleta);

    List<BicicletaResponseDTO> toResponse(List<Bicicleta> bicicletas);

    @Named("status")
    default Bicicleta.Status mapStatus(String status) {
        if (status == null) {
            return null;
        }
        return Bicicleta.Status.valueOf(status);
    }

    @Named("statusNome")
    default String mapStatus(Bicicleta.Status status) {
        if (status == null) {
            return null;
        }
        return status.name();
    }
}
