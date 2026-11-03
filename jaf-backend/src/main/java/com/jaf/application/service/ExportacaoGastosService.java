package com.jaf.application.service;

import com.jaf.application.model.Gasto;
import com.jaf.application.model.NotaFiscal;
import com.jaf.application.model.Relatorio;
import com.jaf.application.repository.NotaFiscalRepository;
import com.jaf.application.repository.RelatorioRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExportacaoGastosService {
    private static final int LIMITE_LINHAS = 50_000;
    private static final String[] CABECALHO = {
            "Data", "Obra", "Etapa", "Categoria", "Descrição", "Método",
            "Valor", "Responsável", "Reembolso", "NF número", "NF CNPJ"
    };

    private final GastoService gastos;
    private final NotaFiscalRepository notas;
    private final RelatorioRepository relatorios;
    private final AcessoObraService acesso;

    public ExportacaoGastosService(GastoService gastos, NotaFiscalRepository notas,
                                   RelatorioRepository relatorios, AcessoObraService acesso) {
        this.gastos = gastos;
        this.notas = notas;
        this.relatorios = relatorios;
        this.acesso = acesso;
    }

    @Transactional
    public Exportacao exportar(Long obraId, LocalDate from, LocalDate to, String etapa,
                               String categoria, String metodoPagamento, Long funcionarioId,
                               com.jaf.application.enums.StatusReembolso statusReembolso,
                               String formato) {
        acesso.exigirObra(obraId);
        if (!"csv".equalsIgnoreCase(formato) && !"xlsx".equalsIgnoreCase(formato)) {
            throw new com.jaf.application.exceptions.BadRequest("Formato deve ser csv ou xlsx.");
        }
        List<Gasto> itens = gastos.listarFiltrado(obraId, funcionarioId, categoria, metodoPagamento,
                statusReembolso, from, to).stream()
                .filter(g -> etapa == null || etapa.equals(g.getEtapa()))
                .toList();
        if (itens.size() > LIMITE_LINHAS) {
            throw new com.jaf.application.exceptions.BadRequest("A exportação excede 50.000 linhas; informe filtros adicionais.");
        }
        byte[] conteudo = "csv".equalsIgnoreCase(formato) ? csv(itens) : xlsx(itens);
        registrar(obraId, from, to, etapa, categoria, metodoPagamento, formato);
        String extensao = formato.toLowerCase();
        return new Exportacao(conteudo, "gastos-obra-" + obraId + "." + extensao,
                "csv".equalsIgnoreCase(formato)
                        ? "text/csv; charset=UTF-8"
                        : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    private byte[] csv(List<Gasto> itens) {
        StringBuilder out = new StringBuilder("\uFEFF");
        out.append(String.join(";", CABECALHO)).append('\n');
        BigDecimal total = BigDecimal.ZERO;
        for (Gasto gasto : itens) {
            List<NotaFiscal> nfs = notas.findByGastoIdOrderByIdAsc(gasto.getId());
            String numero = nfs.stream().map(NotaFiscal::getNumero).filter(java.util.Objects::nonNull).findFirst().orElse("");
            String cnpj = nfs.stream().map(NotaFiscal::getCnpjEmitente).filter(java.util.Objects::nonNull).findFirst().orElse("");
            out.append(String.join(";", valores(gasto, numero, cnpj))).append('\n');
            total = total.add(gasto.getValor());
        }
        out.append("TOTAL;;;;;;").append(decimal(total)).append(";;;;\n");
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] xlsx(List<Gasto> itens) {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Gastos");
            Row header = sheet.createRow(0);
            for (int i = 0; i < CABECALHO.length; i++) header.createCell(i).setCellValue(CABECALHO[i]);
            BigDecimal total = BigDecimal.ZERO;
            int rowNumber = 1;
            for (Gasto gasto : itens) {
                List<NotaFiscal> nfs = notas.findByGastoIdOrderByIdAsc(gasto.getId());
                String numero = nfs.stream().map(NotaFiscal::getNumero).filter(java.util.Objects::nonNull).findFirst().orElse("");
                String cnpj = nfs.stream().map(NotaFiscal::getCnpjEmitente).filter(java.util.Objects::nonNull).findFirst().orElse("");
                Row row = sheet.createRow(rowNumber++);
                List<String> valores = valores(gasto, numero, cnpj);
                for (int i = 0; i < valores.size(); i++) row.createCell(i).setCellValue(valores.get(i));
                total = total.add(gasto.getValor());
            }
            Row totalRow = sheet.createRow(rowNumber);
            totalRow.createCell(0).setCellValue("TOTAL");
            totalRow.createCell(6).setCellValue(total.doubleValue());
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível gerar a planilha.", ex);
        }
    }

    private List<String> valores(Gasto gasto, String numero, String cnpj) {
        return List.of(
                texto(gasto.getDtGasto()), texto(gasto.getObra().getTitulo()), texto(gasto.getEtapa()),
                texto(gasto.getCategoria()), texto(gasto.getDescricao()), texto(gasto.getMetodoPagamento()),
                decimal(gasto.getValor()), texto(gasto.getFuncionario().getNome()),
                Boolean.TRUE.equals(gasto.getReembolsoConcluido()) ? "PAGO" : (gasto.isReembolsavel() ? "PENDENTE" : ""),
                texto(numero), texto(cnpj));
    }

    private void registrar(Long obraId, LocalDate from, LocalDate to, String etapa,
                           String categoria, String metodo, String formato) {
        Relatorio relatorio = new Relatorio();
        relatorio.setTitulo("Exportação de gastos obra " + obraId + " (" + formato.toLowerCase() + ")"
                + filtros(from, to, etapa, categoria, metodo));
        relatorio.setDtEmissao(LocalDate.now());
        relatorio.setFuncionarioResponsavel(acesso.atual());
        relatorios.save(relatorio);
    }

    private String filtros(LocalDate from, LocalDate to, String etapa, String categoria, String metodo) {
        List<String> valores = new ArrayList<>();
        if (from != null) valores.add("from=" + from);
        if (to != null) valores.add("to=" + to);
        if (etapa != null) valores.add("etapa=" + etapa);
        if (categoria != null) valores.add("categoria=" + categoria);
        if (metodo != null) valores.add("metodo=" + metodo);
        return valores.isEmpty() ? "" : " [" + String.join(",", valores) + "]";
    }

    private static String texto(Object value) {
        if (value == null) return "";
        String text = String.valueOf(value);
        return text.matches("^[=+\\-@].*") ? "'" + text : text;
    }
    private static String decimal(BigDecimal value) {
        return value == null ? "" : value.toPlainString().replace('.', ',');
    }
    public record Exportacao(byte[] conteudo, String nomeArquivo, String contentType) {}
}
