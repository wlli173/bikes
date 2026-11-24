package br.edu.ifc.bikes.dto.mapper;

import br.edu.ifc.bikes.dto.LocacaoRelatorioDTO;
import br.edu.ifc.bikes.dto.LocacaoResponseDTO;
import br.edu.ifc.bikes.entity.Bicicleta;
import br.edu.ifc.bikes.entity.Cliente;
import br.edu.ifc.bikes.entity.Locacao;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LocacaoMapperTest {

    private static final LocalDateTime RETIRADA = LocalDateTime.of(2026, 3, 10, 8, 0);

    private final LocacaoMapper mapper = Mappers.getMapper(LocacaoMapper.class);

    @Test
    void toResponseAchataClienteEBicicleta() {
        Locacao locacao = locacao(RETIRADA.plusHours(2).plusMinutes(30));
        locacao.setValor(new BigDecimal("15.00"));
        locacao.setObservacao("Pneu furado");

        LocacaoResponseDTO response = mapper.toResponse(locacao);

        assertEquals(7L, response.id());
        assertEquals(4L, response.clienteId());
        assertEquals("Ana Silva", response.nomeCliente());
        assertEquals("BK01", response.codigoBicicleta());
        assertEquals(RETIRADA, response.dataRetirada());
        assertEquals(RETIRADA.plusHours(3), response.dataPrevistaDevolucao());
        assertEquals(RETIRADA.plusHours(2).plusMinutes(30), response.dataDevolucao());
        assertEquals("1111", response.cartaoFinal());
        assertEquals(new BigDecimal("15.00"), response.valor());
        assertEquals("FINALIZADA", response.status());
        assertEquals("Pneu furado", response.observacao());
    }

    @Test
    void toResponseDeLocacaoAtivaNaoPossuiDevolucaoNemValor() {
        Locacao locacao = locacao(null);

        LocacaoResponseDTO response = mapper.toResponse(locacao);

        assertNull(response.dataDevolucao());
        assertNull(response.valor());
        assertEquals("ATIVA", response.status());
    }

    @Test
    void toRelatorioCalculaDuracaoDaLocacaoFinalizada() {
        LocacaoRelatorioDTO relatorio = mapper.toRelatorio(locacao(RETIRADA.plusHours(2).plusMinutes(5)));

        assertEquals("Ana Silva", relatorio.nomeCliente());
        assertEquals("BK01", relatorio.codigoBicicleta());
        assertEquals(RETIRADA, relatorio.dataRetirada());
        assertEquals(RETIRADA.plusHours(2).plusMinutes(5), relatorio.dataDevolucao());
        assertEquals(125L, relatorio.duracaoMinutos());
        assertEquals("2h 05min", relatorio.duracao());
        assertEquals("FINALIZADA", relatorio.status());
    }

    @Test
    void toRelatorioDeLocacaoAtivaIndicaEmAndamento() {
        LocacaoRelatorioDTO relatorio = mapper.toRelatorio(locacao(null));

        assertNull(relatorio.dataDevolucao());
        assertNull(relatorio.duracaoMinutos());
        assertEquals("Em andamento", relatorio.duracao());
        assertEquals("ATIVA", relatorio.status());
    }

    @Test
    void toRelatorioMapeiaLista() {
        List<LocacaoRelatorioDTO> relatorio = mapper.toRelatorio(List.of(locacao(null), locacao(RETIRADA.plusMinutes(30))));

        assertEquals(2, relatorio.size());
        assertEquals("0h 30min", relatorio.get(1).duracao());
    }

    @Test
    void mapStatusNuloDevolveNulo() {
        assertNull(mapper.mapStatus(null));
    }

    @Test
    void equalsConsideraApenasId() {
        Locacao primeira = new Locacao();
        primeira.setId(1L);
        Locacao segunda = new Locacao();
        segunda.setId(1L);
        Locacao outra = new Locacao();
        outra.setId(2L);

        assertEquals(primeira, segunda);
        assertEquals(false, primeira.equals(outra));
        assertEquals(false, primeira.equals(null));
    }

    private static Locacao locacao(LocalDateTime devolucao) {
        Cliente cliente = new Cliente();
        cliente.setId(4L);
        cliente.setNome("Ana Silva");
        cliente.setCpf("12345678901");

        Bicicleta bicicleta = new Bicicleta();
        bicicleta.setId(2L);
        bicicleta.setCodigo("BK01");

        Locacao locacao = new Locacao();
        locacao.setId(7L);
        locacao.setCliente(cliente);
        locacao.setBicicleta(bicicleta);
        locacao.setDataRetirada(RETIRADA);
        locacao.setDataPrevistaDevolucao(RETIRADA.plusHours(3));
        locacao.setDataDevolucao(devolucao);
        locacao.setCartaoFinal("1111");
        locacao.setCartaoTitular("ANA SILVA");
        locacao.setStatus(devolucao == null ? Locacao.Status.ATIVA : Locacao.Status.FINALIZADA);
        return locacao;
    }
}
