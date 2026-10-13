package br.edu.ifc.bikes.web.exception;

import br.edu.ifc.bikes.exception.EntityNotFoundException;
import br.edu.ifc.bikes.exception.PasswordInvalidException;
import br.edu.ifc.bikes.exception.UsernameUniqueViolationException;
import br.edu.ifc.bikes.web.controller.UsuarioController;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @BeforeEach
    void stubRequest() {
        when(request.getRequestURI()).thenReturn("/api/v1/usuarios");
        when(request.getMethod()).thenReturn("POST");
    }

    @Test
    void validacaoInvalidaRetorna422ComErrosPorCampo() throws Exception {
        BindingResult result = new BeanPropertyBindingResult(new Object(), "usuario");
        result.addError(new FieldError("usuario", "username", "O email deve ser válido"));
        result.addError(new FieldError("usuario", "password", "A senha é obrigatória"));
        Method method = UsuarioController.class.getDeclaredMethod("create", br.edu.ifc.bikes.dto.UsuarioRequestDTO.class);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(new MethodParameter(method, 0), result);

        ResponseEntity<ErrorMessage> response = handler.methodArgumentNotValidException(ex, request, result);

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, response.getStatusCode());
        assertEquals(422, response.getBody().getStatus());
        assertEquals("Campo(s) inválido(s)", response.getBody().getMessage());
        assertEquals("POST", response.getBody().getMethod());
        assertEquals("/api/v1/usuarios", response.getBody().getPath());
        assertEquals("O email deve ser válido", response.getBody().getErrors().get("username"));
        assertEquals("A senha é obrigatória", response.getBody().getErrors().get("password"));
    }

    @Test
    void usernameDuplicadoRetorna409() {
        UsernameUniqueViolationException ex = new UsernameUniqueViolationException("O nome do usuário 'ana@bikes.com' já existe");

        ResponseEntity<ErrorMessage> response = handler.usernameUniqueViolationException(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().getStatus());
        assertEquals(ex.getMessage(), response.getBody().getMessage());
    }

    @Test
    void usuarioInexistenteRetorna404() {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/usuarios/9");
        EntityNotFoundException ex = new EntityNotFoundException("Usuário com id=9 não encontrado");

        ResponseEntity<ErrorMessage> response = handler.entityNotFoundException(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("GET", response.getBody().getMethod());
        assertEquals(ex.getMessage(), response.getBody().getMessage());
    }

    @Test
    void senhaInvalidaRetorna400() {
        when(request.getMethod()).thenReturn("PATCH");
        PasswordInvalidException ex = new PasswordInvalidException("A senha atual não confere");

        ResponseEntity<ErrorMessage> response = handler.passwordInvalidException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("PATCH", response.getBody().getMethod());
        assertEquals(ex.getMessage(), response.getBody().getMessage());
    }
}
