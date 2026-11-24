package br.edu.ifc.bikes.web.controller;

import br.edu.ifc.bikes.dto.UsuarioLoginDto;
import br.edu.ifc.bikes.jwt.JwtToken;
import br.edu.ifc.bikes.jwt.JwtUserDetailsService;
import br.edu.ifc.bikes.web.exception.ErrorMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticacao", description = "Recurso para autenticar um usuário e obter o token JWT usado nas demais operações.")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class AutenticacaoController {

    private final JwtUserDetailsService detailsService;
    private final AuthenticationManager authenticationManager;

    @Operation(summary = "Autenticar na API", description = "Recurso público. Informe o e-mail e a senha (6 caracteres) e use o token devolvido no botão Authorize.",
        responses = {
                @ApiResponse(responseCode = "200", description = "Autenticação realizada com sucesso e retorno do token JWT (válido por 20 minutos)",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = JwtToken.class))),
                @ApiResponse(responseCode = "400", description = "Credenciais inválidas",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                @ApiResponse(responseCode = "422", description = "Campo(s) inválido(s)",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
        })
    @PostMapping("/auth")
    public ResponseEntity<?> autenticar(@RequestBody @Valid UsuarioLoginDto dto, HttpServletRequest request){
        try{
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(dto.username(), dto.password());
            authenticationManager.authenticate(authenticationToken);
            JwtToken token = detailsService.getTokenAuthenticated(dto.username());
            return ResponseEntity.ok(token);
        }catch (AuthenticationException ex){
            System.err.println("Credenciais inválidas!");
        }
        return ResponseEntity.badRequest().body(new ErrorMessage(request, HttpStatus.BAD_REQUEST, "Credenciais inválidas"));
    }
}