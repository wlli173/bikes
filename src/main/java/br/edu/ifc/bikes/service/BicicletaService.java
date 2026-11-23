package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.dto.BicicletaRequestDTO;
import br.edu.ifc.bikes.dto.BicicletaResponseDTO;
import br.edu.ifc.bikes.dto.mapper.BicicletaMapper;
import br.edu.ifc.bikes.entity.Bicicleta;
import br.edu.ifc.bikes.exception.BicicletaIndisponivelException;
import br.edu.ifc.bikes.exception.BicicletaUniqueViolationException;
import br.edu.ifc.bikes.exception.EntityNotFoundException;
import br.edu.ifc.bikes.repository.BicicletaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class BicicletaService {

    private final BicicletaRepository bicicletaRepository;
    private final BicicletaMapper bicicletaMapper;

    public BicicletaResponseDTO create(BicicletaRequestDTO bicicletaRequestDTO) {
        if (bicicletaRepository.existsByCodigo(bicicletaRequestDTO.codigo())) {
            log.warn("Código de bicicleta já cadastrado: codigo={}", bicicletaRequestDTO.codigo());
            throw new BicicletaUniqueViolationException(String.format("Código '%s' já cadastrado", bicicletaRequestDTO.codigo()));
        }

        Bicicleta bicicleta = bicicletaMapper.toBicicleta(bicicletaRequestDTO);
        try {
            Bicicleta salva = bicicletaRepository.save(bicicleta);
            log.info("Bicicleta criada: id={} codigo={}", salva.getId(), salva.getCodigo());
            return bicicletaMapper.toResponse(salva);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Violação de unicidade ao cadastrar bicicleta: codigo={}", bicicletaRequestDTO.codigo());
            throw new BicicletaUniqueViolationException(String.format("Código '%s' já cadastrado", bicicletaRequestDTO.codigo()));
        }
    }

    @Transactional
    public BicicletaResponseDTO getByCodigo(String codigo) {
        log.debug("Buscando bicicleta codigo={}", codigo);
        return bicicletaMapper.toResponse(bicicletaRepository.findByCodigo(codigo).orElseThrow(
                () -> {
                    log.warn("Bicicleta não encontrada: codigo={}", codigo);
                    return new EntityNotFoundException(String.format("Bicicleta com código '%s' não encontrada", codigo));
                }
        ));
    }

    public List<BicicletaResponseDTO> getAll() {
        List<Bicicleta> bicicletas = bicicletaRepository.findAll();
        log.debug("Listagem de bicicletas: {} registro(s)", bicicletas.size());
        return bicicletaMapper.toResponse(bicicletas);
    }

    @Transactional
    public BicicletaResponseDTO updateStatus(String codigo, String status) {
        Bicicleta bicicleta = buscarPorCodigo(codigo);
        if (bicicleta.getStatus() == Bicicleta.Status.ALUGADA) {
            log.warn("Tentativa de alterar status de bicicleta alugada: codigo={}", codigo);
            throw new BicicletaIndisponivelException(String.format("Bicicleta com código '%s' está alugada e só será liberada na devolução", codigo));
        }
        return bicicletaMapper.toResponse(alterarStatus(bicicleta, Bicicleta.Status.valueOf(status)));
    }

    @Transactional
    public Bicicleta alterarStatus(Bicicleta bicicleta, Bicicleta.Status status) {
        bicicleta.setStatus(status);
        Bicicleta salva = bicicletaRepository.save(bicicleta);
        log.info("Status da bicicleta alterado: codigo={} status={}", salva.getCodigo(), status);
        return salva;
    }

    @Transactional
    public Bicicleta buscarPorCodigo(String codigo) {
        return bicicletaRepository.findByCodigo(codigo).orElseThrow(
                () -> new EntityNotFoundException(String.format("Bicicleta com código '%s' não encontrada", codigo))
        );
    }

    //busca para locação: bloqueia a linha e garante que a bicicleta esteja disponível
    @Transactional
    public Bicicleta buscarDisponivelParaLocacao(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return bicicletaRepository.findFirstByStatusOrderByIdAsc(Bicicleta.Status.DISPONIVEL).orElseThrow(
                    () -> {
                        log.warn("Solicitação de locação sem bicicletas disponíveis");
                        return new EntityNotFoundException("Nenhuma bicicleta disponível para locação");
                    }
            );
        }
        Bicicleta bicicleta = bicicletaRepository.findByCodigoParaLocacao(codigo).orElseThrow(
                () -> new EntityNotFoundException(String.format("Bicicleta com código '%s' não encontrada", codigo))
        );
        if (bicicleta.getStatus() != Bicicleta.Status.DISPONIVEL) {
            log.warn("Bicicleta indisponível para locação: codigo={} status={}", codigo, bicicleta.getStatus());
            throw new BicicletaIndisponivelException(String.format("Bicicleta com código '%s' não está disponível (status %s)", codigo, bicicleta.getStatus()));
        }
        return bicicleta;
    }
}
