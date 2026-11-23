package br.edu.ifc.bikes.web.controller;

import br.edu.ifc.bikes.dto.DevolucaoRequestDTO;
import br.edu.ifc.bikes.dto.LocacaoRelatorioDTO;
import br.edu.ifc.bikes.dto.LocacaoRequestDTO;
import br.edu.ifc.bikes.dto.LocacaoResponseDTO;
import br.edu.ifc.bikes.service.LocacaoService;
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

@Tag(name = "Locacoes", description = "Contém todas as operações relativas às locações, devoluções e ao relatório de locações.")
@RestController
@RequestMapping("api/v1/locacoes")
@RequiredArgsConstructor
public class LocacaoController {

    private final LocacaoService locacaoService;

    @Operation(summary = "Solicitar a locação de uma bicicleta",
            description = "O cliente autenticado solicita uma locação informando a previsão de devolução e os dados do cartão de crédito. " +
                    "Se o código da bicicleta for omitido o sistema escolhe a primeira bicicleta disponível. Restrito ao perfil cliente.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Locação criada com sucesso",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LocacaoResponseDTO.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário não autenticado ou sem perfil cliente",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "404", description = "Cliente não cadastrado, bicicleta não encontrada ou nenhuma bicicleta disponível",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "409", description = "Bicicleta indisponível ou já em outra locação",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "422", description = "Recurso não processado por dados de entrada inválidos",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<LocacaoResponseDTO> create(@Valid @RequestBody LocacaoRequestDTO locacao) {
        LocacaoResponseDTO locacaoCriada = locacaoService.create(locacao);
        return ResponseEntity.status(HttpStatus.CREATED).body(locacaoCriada);
    }

    @Operation(summary = "Registrar a devolução de uma bicicleta",
            description = "Registra a data/hora efetiva de devolução, calcula o valor a ser debitado no cartão do cliente e libera a bicicleta. " +
                    "Com 'problema' = true a bicicleta vai para manutenção. O cliente devolve apenas as próprias locações; o admin, qualquer uma.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Devolução registrada com sucesso",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LocacaoResponseDTO.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário não autenticado ou locação de outro cliente",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "404", description = "Locação não encontrada",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "409", description = "Locação já finalizada",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "422", description = "Recurso não processado por dados de entrada inválidos",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @PatchMapping("/{id}/devolucao")
    @PreAuthorize("hasRole('ADMIN') OR (hasRole('CLIENTE') AND @locacaoRepository.existsByIdAndClienteUsuarioId(#id, authentication.principal.id))")
    public ResponseEntity<LocacaoResponseDTO> devolver(@PathVariable Long id,
                                                       @Valid @RequestBody(required = false) DevolucaoRequestDTO devolucao) {
        LocacaoResponseDTO locacao = locacaoService.devolver(id, devolucao);
        return ResponseEntity.ok(locacao);
    }

    @Operation(summary = "Recuperar uma locação pelo id", description = "O cliente consulta apenas as próprias locações. O admin consulta qualquer locação.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Recurso recuperado com sucesso",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LocacaoResponseDTO.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário não autenticado ou locação de outro cliente",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "404", description = "Recurso não encontrado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') OR (hasRole('CLIENTE') AND @locacaoRepository.existsByIdAndClienteUsuarioId(#id, authentication.principal.id))")
    public ResponseEntity<LocacaoResponseDTO> getById(@PathVariable Long id) {
        LocacaoResponseDTO locacao = locacaoService.getById(id);
        return ResponseEntity.ok(locacao);
    }

    @Operation(summary = "Listar todas as locações", description = "Lista todas as locações. Restrito ao administrador.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista com todas as locações registradas",
                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = LocacaoResponseDTO.class)))),
                    @ApiResponse(responseCode = "403", description = "Usuário não autenticado ou sem perfil admin",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LocacaoResponseDTO>> getAll() {
        List<LocacaoResponseDTO> locacoes = locacaoService.getAll();
        return ResponseEntity.ok(locacoes);
    }

    @Operation(summary = "Relatório de locações e devoluções",
            description = "Relação de todas as locações com nome do cliente, código da bicicleta, datas de retirada e devolução, duração e status. " +
                    "Restrito ao administrador.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Relatório gerado com sucesso",
                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = LocacaoRelatorioDTO.class)))),
                    @ApiResponse(responseCode = "403", description = "Usuário não autenticado ou sem perfil admin",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @GetMapping("/relatorio")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LocacaoRelatorioDTO>> getRelatorio() {
        List<LocacaoRelatorioDTO> relatorio = locacaoService.getRelatorio();
        return ResponseEntity.ok(relatorio);
    }
}
