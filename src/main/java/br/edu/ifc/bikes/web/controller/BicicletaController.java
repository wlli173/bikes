package br.edu.ifc.bikes.web.controller;

import br.edu.ifc.bikes.dto.BicicletaRequestDTO;
import br.edu.ifc.bikes.dto.BicicletaResponseDTO;
import br.edu.ifc.bikes.service.BicicletaService;
import br.edu.ifc.bikes.web.exception.ErrorMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Bicicletas", description = "Contém todas as operações relativas aos recursos para cadastro e leitura de uma bicicleta.")
@RestController
@RequestMapping("api/v1/bicicletas")
@RequiredArgsConstructor
public class BicicletaController {

    private final BicicletaService bicicletaService;

    @Operation(summary = "Criar uma nova bicicleta", description = "Recurso para cadastrar uma bicicleta. Restrito ao administrador.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Recurso criado com sucesso",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BicicletaResponseDTO.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário não autenticado ou sem perfil admin",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "409", description = "Código já cadastrado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "422", description = "Recurso não processado por dados de entrada inválidos",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BicicletaResponseDTO> create(@Valid @RequestBody BicicletaRequestDTO bicicleta) {
        BicicletaResponseDTO bicicletaCriada = bicicletaService.create(bicicleta);
        return ResponseEntity.status(HttpStatus.CREATED).body(bicicletaCriada);
    }

    @Operation(summary = "Recuperar uma bicicleta pelo código", description = "Localiza a bicicleta pelo código único. Restrito ao administrador.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Recurso recuperado com sucesso",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BicicletaResponseDTO.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário não autenticado ou sem perfil admin",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "404", description = "Recurso não encontrado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @GetMapping("/{codigo}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BicicletaResponseDTO> getByCodigo(@PathVariable String codigo) {
        BicicletaResponseDTO bicicleta = bicicletaService.getByCodigo(codigo);
        if (bicicleta != null) {
            return ResponseEntity.ok(bicicleta);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Listar todas as bicicletas", description = "Lista todas as bicicletas. Restrito ao administrador.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista com todas as bicicletas cadastradas",
                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = BicicletaResponseDTO.class)))),
                    @ApiResponse(responseCode = "403", description = "Usuário não autenticado ou sem perfil admin",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BicicletaResponseDTO>> getAll() {
        List<BicicletaResponseDTO> bicicletas = bicicletaService.getAll();
        return ResponseEntity.ok(bicicletas);
    }
}
