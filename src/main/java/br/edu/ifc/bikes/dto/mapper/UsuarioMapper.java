package br.edu.ifc.bikes.dto.mapper;

import br.edu.ifc.bikes.dto.UsuarioRequestDTO;
import br.edu.ifc.bikes.dto.UsuarioResponseDTO;
import br.edu.ifc.bikes.entity.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {
    Usuario toUsuario(UsuarioRequestDTO usuarioRequestDTO);

    @Mapping(target = "role", source = "role", qualifiedByName = "role")
    UsuarioResponseDTO toResponse(Usuario usuario);

    List<UsuarioResponseDTO> toResponse(List<Usuario> usuarios);

    @Named("role")
    default String mapRole(Usuario.Role role) {
        if (role == null) {
            return null;
        }
        return role.name().substring("ROLE_".length());
    }
}