package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.dto.ClienteResponseDTO;
import br.edu.ifc.bikes.exception.RelatorioException;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.design.JRDesignField;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JasperServiceTest {

    @Test
    void recordsSaoConvertidosEmMapas() throws JRException {
        JRDataSource dataSource = JasperService.toDataSource(List.of(
                new ClienteResponseDTO(4L, "Ana Silva", "12345678901"),
                new ClienteResponseDTO(5L, "Bruno Lima", "10987654321")));

        assertInstanceOf(JRMapCollectionDataSource.class, dataSource);
        assertTrue(dataSource.next());
        assertEquals(4L, dataSource.getFieldValue(campo("id")));
        assertEquals("Ana Silva", dataSource.getFieldValue(campo("nome")));
        assertEquals("12345678901", dataSource.getFieldValue(campo("cpf")));
        assertTrue(dataSource.next());
        assertEquals("Bruno Lima", dataSource.getFieldValue(campo("nome")));
        assertFalse(dataSource.next());
    }

    @Test
    void listaVaziaEBeansUsamDataSourceDeBeans() {
        assertInstanceOf(JRBeanCollectionDataSource.class, JasperService.toDataSource(List.of()));
        assertInstanceOf(JRBeanCollectionDataSource.class, JasperService.toDataSource(List.of(new Object())));
    }

    @Test
    void recursoInexistenteLancaRelatorioException() {
        JasperService service = new JasperService(new DefaultResourceLoader());

        RelatorioException ex = assertThrows(RelatorioException.class,
                () -> service.generatePdf("nao-existe", "Título", "Subtítulo", List.of()));

        assertEquals("Erro ao gerar o relatório 'nao-existe'", ex.getMessage());
    }

    private static JRField campo(String nome) {
        JRDesignField field = new JRDesignField();
        field.setName(nome);
        return field;
    }
}
