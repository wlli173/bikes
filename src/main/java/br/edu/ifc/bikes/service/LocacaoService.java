package br.edu.ifc.bikes.service;

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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class LocacaoService {

    //tarifa cobrada por hora (ou fração) de utilização
    public static final BigDecimal VALOR_HORA = new BigDecimal("5.00");
    public static final String RELATORIO_JASPER = "locacoes-bikes";

    private final LocacaoRepository locacaoRepository;
    private final LocacaoMapper locacaoMapper;
    private final ClienteService clienteService;
    private final BicicletaService bicicletaService;
    private final JasperService jasperService;

    @Transactional
    public LocacaoResponseDTO create(LocacaoRequestDTO locacaoRequestDTO) {
        Cliente cliente = clienteService.buscarClienteAutenticado();
        Bicicleta bicicleta = bicicletaService.buscarDisponivelParaLocacao(locacaoRequestDTO.codigoBicicleta());

        if (locacaoRepository.existsByBicicletaIdAndStatus(bicicleta.getId(), Locacao.Status.ATIVA)) {
            log.warn("Bicicleta já possui locação ativa: codigo={}", bicicleta.getCodigo());
            throw new BicicletaIndisponivelException(String.format("Bicicleta com código '%s' já possui uma locação ativa", bicicleta.getCodigo()));
        }

        Locacao locacao = new Locacao();
        locacao.setCliente(cliente);
        locacao.setBicicleta(bicicleta);
        locacao.setDataRetirada(LocalDateTime.now());
        locacao.setDataPrevistaDevolucao(locacaoRequestDTO.dataPrevistaDevolucao());
        locacao.setCartaoFinal(ultimosDigitos(locacaoRequestDTO.cartao().numero()));
        locacao.setCartaoTitular(locacaoRequestDTO.cartao().titular());
        locacao.setStatus(Locacao.Status.ATIVA);

        bicicletaService.alterarStatus(bicicleta, Bicicleta.Status.ALUGADA);
        Locacao salva = locacaoRepository.save(locacao);
        log.info("Locação criada: id={} clienteId={} bicicleta={}", salva.getId(), cliente.getId(), bicicleta.getCodigo());
        return locacaoMapper.toResponse(salva);
    }

    @Transactional
    public LocacaoResponseDTO devolver(Long id, DevolucaoRequestDTO devolucaoRequestDTO) {
        Locacao locacao = buscarPorId(id);
        if (locacao.getStatus() != Locacao.Status.ATIVA) {
            log.warn("Devolução de locação já finalizada: id={}", id);
            throw new LocacaoInvalidaException(String.format("Locação com id=%d já foi finalizada", id));
        }

        LocalDateTime dataDevolucao = LocalDateTime.now();
        locacao.setDataDevolucao(dataDevolucao);
        locacao.setValor(calcularValor(locacao.getDataRetirada(), dataDevolucao));
        locacao.setStatus(Locacao.Status.FINALIZADA);

        boolean problema = devolucaoRequestDTO != null && devolucaoRequestDTO.problema();
        if (problema) {
            locacao.setObservacao(devolucaoRequestDTO.descricaoProblema());
        }
        bicicletaService.alterarStatus(locacao.getBicicleta(),
                problema ? Bicicleta.Status.MANUTENCAO : Bicicleta.Status.DISPONIVEL);

        Locacao salva = locacaoRepository.save(locacao);
        log.info("Devolução registrada: id={} valor={} bicicleta={} problema={}",
                salva.getId(), salva.getValor(), salva.getBicicleta().getCodigo(), problema);
        return locacaoMapper.toResponse(salva);
    }

    @Transactional
    public LocacaoResponseDTO getById(Long id) {
        log.debug("Buscando locação id={}", id);
        return locacaoMapper.toResponse(buscarPorId(id));
    }

    @Transactional
    public List<LocacaoResponseDTO> getAll() {
        List<Locacao> locacoes = locacaoRepository.findAll();
        log.debug("Listagem de locações: {} registro(s)", locacoes.size());
        return locacaoMapper.toResponse(locacoes);
    }

    @Transactional
    public List<LocacaoRelatorioDTO> getRelatorio() {
        List<Locacao> locacoes = locacaoRepository.findAllByOrderByDataRetiradaDesc();
        log.debug("Relatório de locações: {} registro(s)", locacoes.size());
        return locacaoMapper.toRelatorio(locacoes);
    }

    @Transactional
    public byte[] getRelatorioPdf() {
        return jasperService.generatePdf(RELATORIO_JASPER, "Relatório de Locações e Devoluções",
                "Relação de todas as locações, com cliente, bicicleta, datas, duração e status", getRelatorio());
    }

    //cobra por hora iniciada: qualquer fração de hora conta como uma hora cheia (mínimo de 1 hora)
    public static BigDecimal calcularValor(LocalDateTime retirada, LocalDateTime devolucao) {
        long minutos = Math.max(0, Duration.between(retirada, devolucao).toMinutes());
        long horas = Math.max(1, (minutos + 59) / 60);
        return VALOR_HORA.multiply(BigDecimal.valueOf(horas)).setScale(2, RoundingMode.HALF_UP);
    }

    private Locacao buscarPorId(Long id) {
        return locacaoRepository.findById(id).orElseThrow(
                () -> {
                    log.warn("Locação não encontrada: id={}", id);
                    return new EntityNotFoundException(String.format("Locação com id=%d não encontrada", id));
                }
        );
    }

    private static String ultimosDigitos(String numero) {
        return numero.substring(numero.length() - 4);
    }
}
