package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.exception.RelatorioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JRTemplate;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.xml.JRXmlTemplateLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
@Service
public class JasperService {

    public static final String JASPER_DIR = "classpath:reports/";
    public static final String STYLES_FILE = JASPER_DIR + "styles/bikes-styles.jrtx";
    public static final String LOGO_FILE = JASPER_DIR + "images/bikes-logo.png";
    public static final Locale REPORT_LOCALE = Locale.of("pt", "BR");

    private final ResourceLoader resourceLoader;

    //os .jrxml são compilados na primeira utilização e reaproveitados nas próximas
    private final Map<String, JasperReport> relatoriosCompilados = new ConcurrentHashMap<>();

    public byte[] generatePdf(String relatorio, String titulo, String subtitulo, Collection<?> dados) {
        log.debug("Gerando relatório {} com {} registro(s)", relatorio, dados.size());
        try (InputStream logo = abrir(LOGO_FILE)) {
            Map<String, Object> params = parametros(titulo, subtitulo, logo);
            JasperReport report = compilar(relatorio);
            JasperPrint print = JasperFillManager.fillReport(report, params, toDataSource(dados));
            byte[] bytes = JasperExportManager.exportReportToPdf(print);
            log.info("Relatório gerado: {} ({} registro(s), {} página(s), {} bytes)",
                    relatorio, dados.size(), print.getPages().size(), bytes.length);
            return bytes;
        } catch (JRException | IOException | RuntimeException ex) {
            log.error("Erro ao gerar relatório {}", relatorio, ex);
            throw new RelatorioException(String.format("Erro ao gerar o relatório '%s'", relatorio), ex);
        }
    }

    private Map<String, Object> parametros(String titulo, String subtitulo, InputStream logo) throws JRException, IOException {
        Map<String, Object> params = new HashMap<>();
        params.put(JRParameter.REPORT_LOCALE, REPORT_LOCALE);
        params.put("REPORT_STYLES", carregarEstilos());
        params.put("IMG_LOGO", logo);
        params.put("REPORT_TITLE", titulo);
        params.put("REPORT_SUBTITLE", subtitulo);
        params.put("GERADO_POR", usuarioAutenticado());
        return params;
    }

    //arquivo de estilização compartilhado por todos os relatórios (cores, fontes, zebra, bordas)
    private JRTemplate carregarEstilos() throws JRException, IOException {
        try (InputStream estilos = abrir(STYLES_FILE)) {
            return JRXmlTemplateLoader.load(estilos);
        }
    }

    private JasperReport compilar(String relatorio) throws JRException {
        JasperReport compilado = relatoriosCompilados.get(relatorio);
        if (compilado != null) {
            return compilado;
        }
        try (InputStream jrxml = abrir(JASPER_DIR + relatorio + ".jrxml")) {
            compilado = JasperCompileManager.compileReport(jrxml);
            log.info("Relatório compilado: {}", relatorio);
        } catch (IOException ex) {
            throw new JRException(String.format("Relatório '%s' não encontrado", relatorio), ex);
        }
        relatoriosCompilados.put(relatorio, compilado);
        return compilado;
    }

    private InputStream abrir(String caminho) throws IOException {
        Resource resource = resourceLoader.getResource(caminho);
        if (!resource.exists()) {
            throw new IOException(String.format("Recurso '%s' não encontrado", caminho));
        }
        return resource.getInputStream();
    }

    //records (os DTOs do projeto) não possuem getters no padrão JavaBean exigido pelo JRBeanCollectionDataSource,
    //por isso são convertidos em mapas; demais objetos seguem pelo data source de beans
    static JRDataSource toDataSource(Collection<?> dados) {
        if (dados.isEmpty() || !dados.iterator().next().getClass().isRecord()) {
            return new JRBeanCollectionDataSource(dados);
        }
        List<Map<String, ?>> linhas = new ArrayList<>(dados.size());
        for (Object dado : dados) {
            linhas.add(toMap(dado));
        }
        return new JRMapCollectionDataSource(linhas);
    }

    private static Map<String, Object> toMap(Object record) {
        Map<String, Object> linha = new LinkedHashMap<>();
        for (RecordComponent componente : record.getClass().getRecordComponents()) {
            try {
                linha.put(componente.getName(), componente.getAccessor().invoke(record));
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(String.format("Não foi possível ler o campo '%s'", componente.getName()), ex);
            }
        }
        return linha;
    }

    private static String usuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }
}
