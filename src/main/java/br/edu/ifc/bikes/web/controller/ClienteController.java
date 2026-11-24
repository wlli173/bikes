package br.edu.ifc.bikes.web.controller;

import br.edu.ifc.bikes.dto.ClienteRequestDTO;
import br.edu.ifc.bikes.dto.ClienteResponseDTO;
import br.edu.ifc.bikes.service.ClienteService;
import br.edu.ifc.bikes.web.exception.ErrorMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Clientes", description = "Contém todas as operações relativas aos recursos para cadastro e leitura de um cliente.")
@RestController
@RequestMapping("api/v1/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    @Operation(summary = "Criar um novo cliente", description = "Recurso para criar um cliente associado ao usuário autenticado. Restrito ao perfil cliente.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Recurso criado com sucesso",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClienteResponseDTO.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário não autenticado ou sem perfil cliente",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "409", description = "CPF já cadastrado ou usuário já possui um cliente",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "422", description = "Recurso não processado por dados de entrada inválidos",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ClienteResponseDTO> create(@Valid @RequestBody ClienteRequestDTO cliente) {
        ClienteResponseDTO clienteCriado = clienteService.create(cliente);
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteCriado);
    }

    @Operation(summary = "Recuperar os dados do cliente autenticado", description = "O cliente recupera os próprios dados a partir do token. Restrito ao perfil cliente.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Recurso recuperado com sucesso",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClienteResponseDTO.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário não autenticado ou sem perfil cliente",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "404", description = "Usuário ainda não possui cadastro de cliente",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @GetMapping("/detalhes")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ClienteResponseDTO> getDetalhes() {
        ClienteResponseDTO cliente = clienteService.getDetalhes();
        return ResponseEntity.ok(cliente);
    }

    @Operation(summary = "Recuperar um cliente pelo id", description = "O cliente consulta apenas os seus dados. O admin consulta qualquer cliente.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Recurso recuperado com sucesso",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClienteResponseDTO.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário não autenticado ou cliente consultando dados de outro cliente",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "404", description = "Recurso não encontrado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') OR (hasRole('CLIENTE') AND @clienteRepository.existsByIdAndUsuarioId(#id, authentication.principal.id))")
    public ResponseEntity<ClienteResponseDTO> getById(@PathVariable Long id) {
        ClienteResponseDTO cliente = clienteService.getById(id);
        if (cliente != null) {
            return ResponseEntity.ok(cliente);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Listar todos os clientes", description = "Lista todos os clientes. Restrito ao perfil admin.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista com todos os clientes cadastrados",
                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = ClienteResponseDTO.class)))),
                    @ApiResponse(responseCode = "403", description = "Usuário não autenticado ou sem perfil admin",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ClienteResponseDTO>> getAll() {
        List<ClienteResponseDTO> clientes = clienteService.getAll();
        return ResponseEntity.ok(clientes);
    }

    @Operation(summary = "Relatório de clientes em PDF", description = "Relação de todos os clientes gerada com JasperReports. Restrito ao perfil admin.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Relatório gerado com sucesso",
                            content = @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary"))),
                    @ApiResponse(responseCode = "403", description = "Usuário não autenticado ou sem perfil admin",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "500", description = "Falha ao gerar o relatório",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @GetMapping("/relatorio")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> gerarRelatorio() {
        byte[] relatorio = clienteService.getRelatorioPdf();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=clientes-" + System.currentTimeMillis() + ".pdf")
                .body(relatorio);
    }
}
