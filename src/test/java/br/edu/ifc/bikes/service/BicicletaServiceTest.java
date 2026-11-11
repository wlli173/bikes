package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.dto.BicicletaRequestDTO;
import br.edu.ifc.bikes.dto.BicicletaResponseDTO;
import br.edu.ifc.bikes.dto.mapper.BicicletaMapper;
import br.edu.ifc.bikes.entity.Bicicleta;
import br.edu.ifc.bikes.exception.BicicletaUniqueViolationException;
import br.edu.ifc.bikes.exception.EntityNotFoundException;
import br.edu.ifc.bikes.repository.BicicletaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BicicletaServiceTest {

    @Mock
    private BicicletaRepository bicicletaRepository;

    @Mock
    private BicicletaMapper bicicletaMapper;

    @InjectMocks
    private BicicletaService bicicletaService;

    @Test
    void createPersisteBicicleta() {
        BicicletaRequestDTO request = new BicicletaRequestDTO("BK01", "Caloi Elite", "DISPONIVEL");
        Bicicleta bicicleta = new Bicicleta();
        BicicletaResponseDTO response = new BicicletaResponseDTO(3L, "BK01", "Caloi Elite", "DISPONIVEL");

        when(bicicletaRepository.existsByCodigo("BK01")).thenReturn(false);
        when(bicicletaMapper.toBicicleta(request)).thenReturn(bicicleta);
        when(bicicletaRepository.save(bicicleta)).thenAnswer(invocation -> {
            Bicicleta salva = invocation.getArgument(0);
            salva.setId(3L);
            salva.setCodigo("BK01");
            return salva;
        });
        when(bicicletaMapper.toResponse(bicicleta)).thenReturn(response);

        assertEquals(response, bicicletaService.create(request));
    }

    @Test
    void createComCodigoRepetidoLancaConflito() {
        when(bicicletaRepository.existsByCodigo("BK01")).thenReturn(true);

        BicicletaUniqueViolationException ex = assertThrows(BicicletaUniqueViolationException.class,
                () -> bicicletaService.create(new BicicletaRequestDTO("BK01", "Caloi Elite", "DISPONIVEL")));

        assertEquals("Código 'BK01' já cadastrado", ex.getMessage());
        verify(bicicletaRepository, never()).save(any());
    }

    @Test
    void getByCodigoDevolveBicicletaEncontrada() {
        Bicicleta bicicleta = new Bicicleta();
        bicicleta.setCodigo("BK01");
        BicicletaResponseDTO response = new BicicletaResponseDTO(4L, "BK01", "Caloi Elite", "ALUGADA");
        when(bicicletaRepository.findByCodigo("BK01")).thenReturn(Optional.of(bicicleta));
        when(bicicletaMapper.toResponse(bicicleta)).thenReturn(response);

        assertEquals(response, bicicletaService.getByCodigo("BK01"));
    }

    @Test
    void getByCodigoInexistenteLancaEntityNotFound() {
        when(bicicletaRepository.findByCodigo("BK99")).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> bicicletaService.getByCodigo("BK99"));

        assertEquals("Bicicleta com código 'BK99' não encontrada", ex.getMessage());
    }

    @Test
    void getAllMapeiaBicicletasDoRepositorio() {
        Bicicleta bicicleta = new Bicicleta();
        List<Bicicleta> bicicletas = List.of(bicicleta);
        List<BicicletaResponseDTO> resposta = List.of(new BicicletaResponseDTO(1L, "BK01", "Caloi Elite", "DISPONIVEL"));
        when(bicicletaRepository.findAll()).thenReturn(bicicletas);
        when(bicicletaMapper.toResponse(bicicletas)).thenReturn(resposta);

        assertEquals(resposta, bicicletaService.getAll());
    }

    @Test
    void getAllSemRegistrosDevolveListaVazia() {
        List<Bicicleta> vazia = List.of();
        when(bicicletaRepository.findAll()).thenReturn(vazia);
        when(bicicletaMapper.toResponse(vazia)).thenReturn(List.of());

        assertTrue(bicicletaService.getAll().isEmpty());
    }
}
