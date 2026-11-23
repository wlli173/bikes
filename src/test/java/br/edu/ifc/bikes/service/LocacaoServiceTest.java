package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.dto.CartaoCreditoDTO;
import br.edu.ifc.bikes.dto.DevolucaoRequestDTO;
import br.edu.ifc.bikes.dto.LocacaoRelatorioDTO;
import br.edu.ifc.bikes.dto.LocacaoRequestDTO;
import br.edu.ifc.bikes.dto.LocacaoResponseDTO;
import br.edu.ifc.bikes.dto.mapper.LocacaoMapper;
import br.edu.ifc.bikes.entity.Bicicleta;
import br.edu.ifc.bikes.entity.Cliente;
import br.edu.ifc.bikes.entity.Locacao;
import br.edu.ifc.bikes.exception.BicicletaIndisponivelException;
import br.edu.ifc.bikes.exception.EntityNotFoundException;
import br.edu.ifc.bikes.exception.LocacaoInvalidaException;
import br.edu.ifc.bikes.repository.LocacaoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocacaoServiceTest {

    private static final CartaoCreditoDTO CARTAO = new CartaoCreditoDTO("4111111111111111", "ANA SILVA", "12/30", "123");

    @Mock
    private LocacaoRepository locacaoRepository;

    @Mock
    private LocacaoMapper locacaoMapper;

    @Mock
    private ClienteService clienteService;

    @Mock
    private BicicletaService bicicletaService;

    @InjectMocks
    private LocacaoService locacaoService;

    @Test
    void createRegistraRetiradaCartaoMascaradoEAlugaBicicleta() {
        Cliente cliente = cliente();
        Bicicleta bicicleta = bicicleta(Bicicleta.Status.DISPONIVEL);
        LocalDateTime previsao = LocalDateTime.now().plusHours(2);
        LocacaoRequestDTO request = new LocacaoRequestDTO("BK01", previsao, CARTAO);
        LocacaoResponseDTO response = response("ATIVA", null);

        when(clienteService.buscarClienteAutenticado()).thenReturn(cliente);
        when(bicicletaService.buscarDisponivelParaLocacao("BK01")).thenReturn(bicicleta);
        when(locacaoRepository.existsByBicicletaIdAndStatus(2L, Locacao.Status.ATIVA)).thenReturn(false);
        when(locacaoRepository.save(any(Locacao.class))).thenAnswer(invocation -> {
            Locacao salva = invocation.getArgument(0);
            salva.setId(7L);
            return salva;
        });
        when(locacaoMapper.toResponse(any(Locacao.class))).thenReturn(response);

        assertEquals(response, locacaoService.create(request));

        ArgumentCaptor<Locacao> captor = ArgumentCaptor.forClass(Locacao.class);
        verify(locacaoRepository).save(captor.capture());
        Locacao locacao = captor.getValue();
        assertEquals(cliente, locacao.getCliente());
        assertEquals(bicicleta, locacao.getBicicleta());
        assertNotNull(locacao.getDataRetirada());
        assertEquals(previsao, locacao.getDataPrevistaDevolucao());
        assertEquals("1111", locacao.getCartaoFinal());
        assertEquals("ANA SILVA", locacao.getCartaoTitular());
        assertNull(locacao.getDataDevolucao());
        assertEquals(Locacao.Status.ATIVA, locacao.getStatus());
        verify(bicicletaService).alterarStatus(bicicleta, Bicicleta.Status.ALUGADA);
    }

    @Test
    void createSemCodigoDelegaEscolhaDaBicicletaAoServico() {
        Bicicleta bicicleta = bicicleta(Bicicleta.Status.DISPONIVEL);
        when(clienteService.buscarClienteAutenticado()).thenReturn(cliente());
        when(bicicletaService.buscarDisponivelParaLocacao(null)).thenReturn(bicicleta);
        when(locacaoRepository.existsByBicicletaIdAndStatus(2L, Locacao.Status.ATIVA)).thenReturn(false);
        when(locacaoRepository.save(any(Locacao.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(locacaoMapper.toResponse(any(Locacao.class))).thenReturn(response("ATIVA", null));

        locacaoService.create(new LocacaoRequestDTO(null, LocalDateTime.now().plusHours(1), CARTAO));

        verify(bicicletaService).buscarDisponivelParaLocacao(null);
    }

    @Test
    void createComBicicletaEmLocacaoAtivaLancaConflito() {
        when(clienteService.buscarClienteAutenticado()).thenReturn(cliente());
        when(bicicletaService.buscarDisponivelParaLocacao("BK01")).thenReturn(bicicleta(Bicicleta.Status.DISPONIVEL));
        when(locacaoRepository.existsByBicicletaIdAndStatus(2L, Locacao.Status.ATIVA)).thenReturn(true);

        BicicletaIndisponivelException ex = assertThrows(BicicletaIndisponivelException.class,
                () -> locacaoService.create(new LocacaoRequestDTO("BK01", LocalDateTime.now().plusHours(1), CARTAO)));

        assertEquals("Bicicleta com código 'BK01' já possui uma locação ativa", ex.getMessage());
        verify(locacaoRepository, never()).save(any());
        verify(bicicletaService, never()).alterarStatus(any(), any());
    }

    @Test
    void createSemClienteCadastradoPropagaEntityNotFound() {
        when(clienteService.buscarClienteAutenticado())
                .thenThrow(new EntityNotFoundException("Usuário autenticado não possui cadastro de cliente"));

        assertThrows(EntityNotFoundException.class,
                () -> locacaoService.create(new LocacaoRequestDTO("BK01", LocalDateTime.now().plusHours(1), CARTAO)));
        verify(locacaoRepository, never()).save(any());
    }

    @Test
    void devolverCalculaValorFinalizaELiberaBicicleta() {
        Bicicleta bicicleta = bicicleta(Bicicleta.Status.ALUGADA);
        Locacao locacao = locacaoAtiva(bicicleta, LocalDateTime.now().minusMinutes(90));
        LocacaoResponseDTO response = response("FINALIZADA", new BigDecimal("10.00"));
        when(locacaoRepository.findById(7L)).thenReturn(Optional.of(locacao));
        when(locacaoRepository.save(locacao)).thenReturn(locacao);
        when(locacaoMapper.toResponse(locacao)).thenReturn(response);

        assertEquals(response, locacaoService.devolver(7L, new DevolucaoRequestDTO(false, null)));

        assertNotNull(locacao.getDataDevolucao());
        assertEquals(new BigDecimal("10.00"), locacao.getValor());
        assertEquals(Locacao.Status.FINALIZADA, locacao.getStatus());
        assertNull(locacao.getObservacao());
        verify(bicicletaService).alterarStatus(bicicleta, Bicicleta.Status.DISPONIVEL);
    }

    @Test
    void devolverSemCorpoLiberaBicicleta() {
        Bicicleta bicicleta = bicicleta(Bicicleta.Status.ALUGADA);
        Locacao locacao = locacaoAtiva(bicicleta, LocalDateTime.now().minusMinutes(10));
        when(locacaoRepository.findById(7L)).thenReturn(Optional.of(locacao));
        when(locacaoRepository.save(locacao)).thenReturn(locacao);
        when(locacaoMapper.toResponse(locacao)).thenReturn(response("FINALIZADA", new BigDecimal("5.00")));

        locacaoService.devolver(7L, null);

        assertEquals(new BigDecimal("5.00"), locacao.getValor());
        verify(bicicletaService).alterarStatus(bicicleta, Bicicleta.Status.DISPONIVEL);
    }

    @Test
    void devolverComProblemaEnviaBicicletaParaManutencao() {
        Bicicleta bicicleta = bicicleta(Bicicleta.Status.ALUGADA);
        Locacao locacao = locacaoAtiva(bicicleta, LocalDateTime.now().minusMinutes(10));
        when(locacaoRepository.findById(7L)).thenReturn(Optional.of(locacao));
        when(locacaoRepository.save(locacao)).thenReturn(locacao);
        when(locacaoMapper.toResponse(locacao)).thenReturn(response("FINALIZADA", new BigDecimal("5.00")));

        locacaoService.devolver(7L, new DevolucaoRequestDTO(true, "Pneu furado"));

        assertEquals("Pneu furado", locacao.getObservacao());
        verify(bicicletaService).alterarStatus(bicicleta, Bicicleta.Status.MANUTENCAO);
    }

    @Test
    void devolverLocacaoJaFinalizadaLancaConflito() {
        Locacao locacao = locacaoAtiva(bicicleta(Bicicleta.Status.DISPONIVEL), LocalDateTime.now().minusHours(3));
        locacao.setStatus(Locacao.Status.FINALIZADA);
        when(locacaoRepository.findById(7L)).thenReturn(Optional.of(locacao));

        LocacaoInvalidaException ex = assertThrows(LocacaoInvalidaException.class,
                () -> locacaoService.devolver(7L, null));

        assertEquals("Locação com id=7 já foi finalizada", ex.getMessage());
        verify(locacaoRepository, never()).save(any());
        verify(bicicletaService, never()).alterarStatus(any(), any());
    }

    @Test
    void devolverLocacaoInexistenteLancaEntityNotFound() {
        when(locacaoRepository.findById(9L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> locacaoService.devolver(9L, null));

        assertEquals("Locação com id=9 não encontrada", ex.getMessage());
    }

    @Test
    void calcularValorCobraPorHoraIniciadaComMinimoDeUmaHora() {
        LocalDateTime retirada = LocalDateTime.of(2026, 3, 10, 8, 0);

        assertEquals(new BigDecimal("5.00"), LocacaoService.calcularValor(retirada, retirada));
        assertEquals(new BigDecimal("5.00"), LocacaoService.calcularValor(retirada, retirada.plusMinutes(59)));
        assertEquals(new BigDecimal("5.00"), LocacaoService.calcularValor(retirada, retirada.plusMinutes(60)));
        assertEquals(new BigDecimal("10.00"), LocacaoService.calcularValor(retirada, retirada.plusMinutes(61)));
        assertEquals(new BigDecimal("15.00"), LocacaoService.calcularValor(retirada, retirada.plusHours(2).plusMinutes(30)));
    }

    @Test
    void getByIdDevolveLocacaoEncontrada() {
        Locacao locacao = locacaoAtiva(bicicleta(Bicicleta.Status.ALUGADA), LocalDateTime.now());
        LocacaoResponseDTO response = response("ATIVA", null);
        when(locacaoRepository.findById(7L)).thenReturn(Optional.of(locacao));
        when(locacaoMapper.toResponse(locacao)).thenReturn(response);

        assertEquals(response, locacaoService.getById(7L));
    }

    @Test
    void getByIdInexistenteLancaEntityNotFound() {
        when(locacaoRepository.findById(9L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> locacaoService.getById(9L));

        assertEquals("Locação com id=9 não encontrada", ex.getMessage());
    }

    @Test
    void getAllMapeiaLocacoesDoRepositorio() {
        List<Locacao> locacoes = List.of(new Locacao());
        List<LocacaoResponseDTO> resposta = List.of(response("ATIVA", null));
        when(locacaoRepository.findAll()).thenReturn(locacoes);
        when(locacaoMapper.toResponse(locacoes)).thenReturn(resposta);

        assertEquals(resposta, locacaoService.getAll());
    }

    @Test
    void getRelatorioMapeiaLocacoesOrdenadasPorRetirada() {
        List<Locacao> locacoes = List.of(new Locacao());
        List<LocacaoRelatorioDTO> relatorio = List.of(new LocacaoRelatorioDTO(7L, "Ana Silva", "BK01",
                LocalDateTime.now(), null, null, "Em andamento", null, "ATIVA"));
        when(locacaoRepository.findAllByOrderByDataRetiradaDesc()).thenReturn(locacoes);
        when(locacaoMapper.toRelatorio(locacoes)).thenReturn(relatorio);

        assertEquals(relatorio, locacaoService.getRelatorio());
    }

    @Test
    void getRelatorioSemRegistrosDevolveListaVazia() {
        List<Locacao> vazia = List.of();
        when(locacaoRepository.findAllByOrderByDataRetiradaDesc()).thenReturn(vazia);
        when(locacaoMapper.toRelatorio(vazia)).thenReturn(List.of());

        assertTrue(locacaoService.getRelatorio().isEmpty());
    }

    private static Cliente cliente() {
        Cliente cliente = new Cliente();
        cliente.setId(4L);
        cliente.setNome("Ana Silva");
        cliente.setCpf("12345678901");
        return cliente;
    }

    private static Bicicleta bicicleta(Bicicleta.Status status) {
        Bicicleta bicicleta = new Bicicleta();
        bicicleta.setId(2L);
        bicicleta.setCodigo("BK01");
        bicicleta.setModelo("Caloi Elite");
        bicicleta.setStatus(status);
        return bicicleta;
    }

    private static Locacao locacaoAtiva(Bicicleta bicicleta, LocalDateTime retirada) {
        Locacao locacao = new Locacao();
        locacao.setId(7L);
        locacao.setCliente(cliente());
        locacao.setBicicleta(bicicleta);
        locacao.setDataRetirada(retirada);
        locacao.setDataPrevistaDevolucao(retirada.plusHours(3));
        locacao.setCartaoFinal("1111");
        locacao.setCartaoTitular("ANA SILVA");
        locacao.setStatus(Locacao.Status.ATIVA);
        return locacao;
    }

    private static LocacaoResponseDTO response(String status, BigDecimal valor) {
        return new LocacaoResponseDTO(7L, 4L, "Ana Silva", "BK01", LocalDateTime.now(), LocalDateTime.now().plusHours(3),
                null, "1111", valor, status, null);
    }
}
