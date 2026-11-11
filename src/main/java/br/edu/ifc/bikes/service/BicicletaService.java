package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.dto.BicicletaRequestDTO;
import br.edu.ifc.bikes.dto.BicicletaResponseDTO;
import br.edu.ifc.bikes.dto.mapper.BicicletaMapper;
import br.edu.ifc.bikes.entity.Bicicleta;
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
}
