package br.edu.ifc.bikes.source;

import br.edu.ifc.bikes.entity.Usuario;
import br.edu.ifc.bikes.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioSourceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioSource usuarioSource;

    @Test
    void createDelegaPersistenciaAoRepositorio() {
        Usuario usuario = new Usuario();
        usuario.setUsername("ana@bikes.com");
        usuario.setPassword("123456");
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        assertSame(usuario, usuarioSource.create(usuario));
        verify(usuarioRepository).save(usuario);
    }
}
