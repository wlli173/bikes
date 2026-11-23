package br.edu.ifc.bikes.repository;

import br.edu.ifc.bikes.entity.Bicicleta;
import br.edu.ifc.bikes.entity.Cliente;
import br.edu.ifc.bikes.entity.Locacao;
import br.edu.ifc.bikes.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class LocacaoRepositoryIT {

    @Autowired
    private LocacaoRepository locacaoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private BicicletaRepository bicicletaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void saveAtribuiIdEPermiteConsultarLocacaoAtivaDaBicicletaEDoDono() {
        Usuario usuario = usuarioRepository.saveAndFlush(novoUsuario());
        Cliente cliente = clienteRepository.saveAndFlush(novoCliente(usuario));
        Bicicleta bicicleta = bicicletaRepository.saveAndFlush(novaBicicleta("BK01"));

        Locacao salva = locacaoRepository.saveAndFlush(novaLocacao(cliente, bicicleta, LocalDateTime.now()));

        assertTrue(salva.getId() != null && salva.getId() > 0);
        assertEquals(Locacao.Status.ATIVA, salva.getStatus());
        assertTrue(locacaoRepository.existsByBicicletaIdAndStatus(bicicleta.getId(), Locacao.Status.ATIVA));
        assertFalse(locacaoRepository.existsByBicicletaIdAndStatus(bicicleta.getId(), Locacao.Status.FINALIZADA));
        assertTrue(locacaoRepository.existsByIdAndClienteUsuarioId(salva.getId(), usuario.getId()));
        assertFalse(locacaoRepository.existsByIdAndClienteUsuarioId(salva.getId(), usuario.getId() + 1));
    }

    @Test
    void relatorioOrdenaPelaRetiradaMaisRecente() {
        Usuario usuario = usuarioRepository.saveAndFlush(novoUsuario());
        Cliente cliente = clienteRepository.saveAndFlush(novoCliente(usuario));
        Bicicleta bicicleta = bicicletaRepository.saveAndFlush(novaBicicleta("BK01"));
        LocalDateTime agora = LocalDateTime.now();
        Locacao antiga = locacaoRepository.saveAndFlush(novaLocacao(cliente, bicicleta, agora.minusDays(2)));
        Locacao recente = locacaoRepository.saveAndFlush(novaLocacao(cliente, bicicleta, agora.minusHours(1)));

        List<Locacao> relatorio = locacaoRepository.findAllByOrderByDataRetiradaDesc();

        assertEquals(List.of(recente.getId(), antiga.getId()), relatorio.stream().map(Locacao::getId).toList());
    }

    @Test
    void locacaoSemClienteOuBicicletaViolaRestricao() {
        Usuario usuario = usuarioRepository.saveAndFlush(novoUsuario());
        Cliente cliente = clienteRepository.saveAndFlush(novoCliente(usuario));
        Bicicleta bicicleta = bicicletaRepository.saveAndFlush(novaBicicleta("BK01"));

        Locacao semCliente = novaLocacao(null, bicicleta, LocalDateTime.now());
        assertThrows(DataIntegrityViolationException.class, () -> locacaoRepository.saveAndFlush(semCliente));

        Locacao semBicicleta = novaLocacao(cliente, null, LocalDateTime.now());
        assertThrows(DataIntegrityViolationException.class, () -> locacaoRepository.saveAndFlush(semBicicleta));
    }

    private static Usuario novoUsuario() {
        Usuario usuario = new Usuario();
        usuario.setUsername("repo-" + UUID.randomUUID() + "@bikes.com");
        usuario.setPassword("123456");
        return usuario;
    }

    private static Cliente novoCliente(Usuario usuario) {
        Cliente cliente = new Cliente();
        cliente.setNome("Ana Silva");
        cliente.setCpf("12345678901");
        cliente.setUsuario(usuario);
        return cliente;
    }

    private static Bicicleta novaBicicleta(String codigo) {
        Bicicleta bicicleta = new Bicicleta();
        bicicleta.setCodigo(codigo);
        bicicleta.setModelo("Caloi Elite");
        bicicleta.setStatus(Bicicleta.Status.ALUGADA);
        return bicicleta;
    }

    private static Locacao novaLocacao(Cliente cliente, Bicicleta bicicleta, LocalDateTime retirada) {
        Locacao locacao = new Locacao();
        locacao.setCliente(cliente);
        locacao.setBicicleta(bicicleta);
        locacao.setDataRetirada(retirada);
        locacao.setDataPrevistaDevolucao(retirada.plusHours(2));
        locacao.setCartaoFinal("1111");
        locacao.setCartaoTitular("ANA SILVA");
        return locacao;
    }
}
