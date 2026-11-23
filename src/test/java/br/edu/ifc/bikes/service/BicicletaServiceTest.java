package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.dto.BicicletaRequestDTO;
import br.edu.ifc.bikes.dto.BicicletaResponseDTO;
import br.edu.ifc.bikes.dto.mapper.BicicletaMapper;
import br.edu.ifc.bikes.entity.Bicicleta;
import br.edu.ifc.bikes.exception.BicicletaIndisponivelException;
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

    @Test
    void updateStatusColocaBicicletaEmManutencao() {
        Bicicleta bicicleta = nova("BK01", Bicicleta.Status.DISPONIVEL);
        BicicletaResponseDTO response = new BicicletaResponseDTO(1L, "BK01", "Caloi Elite", "MANUTENCAO");
        when(bicicletaRepository.findByCodigo("BK01")).thenReturn(Optional.of(bicicleta));
        when(bicicletaRepository.save(bicicleta)).thenReturn(bicicleta);
        when(bicicletaMapper.toResponse(bicicleta)).thenReturn(response);

        assertEquals(response, bicicletaService.updateStatus("BK01", "MANUTENCAO"));
        assertEquals(Bicicleta.Status.MANUTENCAO, bicicleta.getStatus());
    }

    @Test
    void updateStatusDeBicicletaAlugadaLancaConflito() {
        when(bicicletaRepository.findByCodigo("BK01")).thenReturn(Optional.of(nova("BK01", Bicicleta.Status.ALUGADA)));

        BicicletaIndisponivelException ex = assertThrows(BicicletaIndisponivelException.class,
                () -> bicicletaService.updateStatus("BK01", "DISPONIVEL"));

        assertEquals("Bicicleta com código 'BK01' está alugada e só será liberada na devolução", ex.getMessage());
        verify(bicicletaRepository, never()).save(any());
    }

    @Test
    void updateStatusDeBicicletaInexistenteLancaEntityNotFound() {
        when(bicicletaRepository.findByCodigo("BK99")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> bicicletaService.updateStatus("BK99", "MANUTENCAO"));
    }

    @Test
    void buscarDisponivelSemCodigoEscolhePrimeiraDisponivel() {
        Bicicleta disponivel = nova("BK02", Bicicleta.Status.DISPONIVEL);
        when(bicicletaRepository.findFirstByStatusOrderByIdAsc(Bicicleta.Status.DISPONIVEL)).thenReturn(Optional.of(disponivel));

        assertEquals(disponivel, bicicletaService.buscarDisponivelParaLocacao(null));
        assertEquals(disponivel, bicicletaService.buscarDisponivelParaLocacao(" "));
    }

    @Test
    void buscarDisponivelSemNenhumaDisponivelLancaEntityNotFound() {
        when(bicicletaRepository.findFirstByStatusOrderByIdAsc(Bicicleta.Status.DISPONIVEL)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> bicicletaService.buscarDisponivelParaLocacao(null));

        assertEquals("Nenhuma bicicleta disponível para locação", ex.getMessage());
    }

    @Test
    void buscarDisponivelPorCodigoDevolveBicicletaDisponivel() {
        Bicicleta disponivel = nova("BK01", Bicicleta.Status.DISPONIVEL);
        when(bicicletaRepository.findByCodigoParaLocacao("BK01")).thenReturn(Optional.of(disponivel));

        assertEquals(disponivel, bicicletaService.buscarDisponivelParaLocacao("BK01"));
    }

    @Test
    void buscarDisponivelPorCodigoEmManutencaoLancaConflito() {
        when(bicicletaRepository.findByCodigoParaLocacao("BK01")).thenReturn(Optional.of(nova("BK01", Bicicleta.Status.MANUTENCAO)));

        BicicletaIndisponivelException ex = assertThrows(BicicletaIndisponivelException.class,
                () -> bicicletaService.buscarDisponivelParaLocacao("BK01"));

        assertEquals("Bicicleta com código 'BK01' não está disponível (status MANUTENCAO)", ex.getMessage());
    }

    @Test
    void buscarDisponivelPorCodigoInexistenteLancaEntityNotFound() {
        when(bicicletaRepository.findByCodigoParaLocacao("BK99")).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> bicicletaService.buscarDisponivelParaLocacao("BK99"));

        assertEquals("Bicicleta com código 'BK99' não encontrada", ex.getMessage());
    }

    private static Bicicleta nova(String codigo, Bicicleta.Status status) {
        Bicicleta bicicleta = new Bicicleta();
        bicicleta.setId(1L);
        bicicleta.setCodigo(codigo);
        bicicleta.setModelo("Caloi Elite");
        bicicleta.setStatus(status);
        return bicicleta;
    }
}
