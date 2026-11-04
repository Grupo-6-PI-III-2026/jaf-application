package com.jaf.application.service;

import com.jaf.application.dto.DashboardResponseDto;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.model.Obra;
import com.jaf.application.repository.GastoRepository;
import com.jaf.application.repository.ObraRepository;
import com.jaf.application.repository.PagamentoReembolsoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;

@Service
public class DashboardService {

    private static final Set<String> CATEGORIAS_IMPREVISTOS = Set.of("IMPREVISTO", "Custos extras");

    // Limites usados no lugar de datas nulas para manter as consultas independentes de parâmetros opcionais.
    private static final LocalDate DATA_MINIMA = LocalDate.of(1900, 1, 1);
    private static final LocalDate DATA_MAXIMA = LocalDate.of(9999, 12, 31);

    private final GastoRepository gastoRepository;
    private final ObraRepository obraRepository;
    private final AcessoObraService acesso;
    private final PagamentoReembolsoRepository pagamentos;

    public DashboardService(GastoRepository gastoRepository, ObraRepository obraRepository,
                            AcessoObraService acesso, PagamentoReembolsoRepository pagamentos) {
        this.gastoRepository = gastoRepository;
        this.obraRepository = obraRepository;
        this.acesso = acesso;
        this.pagamentos = pagamentos;
    }

    public DashboardResponseDto buscarStats(Long obraId, String etapa) {
        return buscarStats(obraId, etapa, null, null);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public DashboardResponseDto buscarStats(Long obraId, String etapa, LocalDate from, LocalDate to) {
        acesso.exigirAdmin();
        if (from != null && to != null && to.isBefore(from))
            throw new com.jaf.application.exceptions.BadRequest("Data final anterior à inicial.");
        Obra obra = obraRepository.findById(obraId)
                .orElseThrow(() -> new NotFoundException("Obra não encontrada."));

        BigDecimal orcamento = obra.getOrcamento();

        LocalDate de = from != null ? from : DATA_MINIMA;
        LocalDate ate = to != null ? to : DATA_MAXIMA;
        boolean todas = "TODAS".equals(etapa);
        String etapaFiltro = todas ? "" : etapa;

        // StatCard 1: gastos da etapa selecionada
        BigDecimal totalGastosEtapa = gastoRepository.somarPeriodoEtapa(obraId, de, ate, todas, etapaFiltro);
        int progressoEtapa = calcularPercentual(totalGastosEtapa, orcamento);

        // Recebido e saldo acumulados de toda a obra, sem filtros de etapa ou período.
        // Parcelas não são novas despesas; orçamento não é recebido.
        BigDecimal totalGastosObra = gastoRepository.somarPorObra(obraId);
        BigDecimal saldoRestante = obra.getTotalRecebido().subtract(totalGastosObra);
        int progressoSaldo = calcularPercentual(totalGastosObra, obra.getTotalRecebido());

        // StatCard 2: reembolsos pendentes (filtrado pela etapa e período)
        BigDecimal totalReembolsos = gastoRepository.somarReembolsaveis(obraId, de, ate, todas, etapaFiltro);
        BigDecimal totalPago = pagamentos.totalPagoDosReembolsaveis(obraId, de, ate, todas, etapaFiltro);
        BigDecimal reembolsosPendentes = totalReembolsos.subtract(totalPago);

        // Pizza: proporção monetária paga vs pendente, incluindo pagamentos parciais.
        List<DashboardResponseDto.PieItemDto> pizza = List.of(
            new DashboardResponseDto.PieItemDto("Concluídos", calcularPercentual(totalPago, totalReembolsos)),
            new DashboardResponseDto.PieItemDto("Pendentes", totalReembolsos.signum() == 0 ? 0 : 100 - calcularPercentual(totalPago, totalReembolsos)));

        // Linha: gastos imprevistos agrupados por mês
        List<DashboardResponseDto.LineItemDto> linha = calcularLinha(
                gastoRepository.totaisPorData(obraId, de, ate, todas, etapaFiltro, CATEGORIAS_IMPREVISTOS));

        // Barra: gastos por categoria (filtrado pela etapa selecionada)
        List<DashboardResponseDto.BarItemDto> barra = calcularBarra(
                gastoRepository.totaisPorCategoria(obraId, de, ate, todas, etapaFiltro));

        return new DashboardResponseDto(
                totalGastosEtapa,
                progressoEtapa,
                reembolsosPendentes,
                saldoRestante,
                progressoSaldo,
                pizza,
                linha,
                barra
        );
    }
    private List<DashboardResponseDto.LineItemDto> calcularLinha(List<GastoRepository.TotalPorData> totais) {
        Map<YearMonth, BigDecimal> porMes = new TreeMap<>();
        totais.forEach(t -> porMes.merge(YearMonth.from(t.getDtGasto()), t.getTotal(), BigDecimal::add));

        return porMes.entrySet().stream()
                .map(e -> {
                    String mes = e.getKey().getMonth()
                            .getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));
                    String mesCapitalizado = Character.toUpperCase(mes.charAt(0)) + mes.substring(1);
                    return new DashboardResponseDto.LineItemDto(mesCapitalizado, e.getValue().doubleValue());
                })
                .toList();
    }

    private List<DashboardResponseDto.BarItemDto> calcularBarra(List<GastoRepository.TotalPorCategoria> totais) {
        Map<String, BigDecimal> porCategoria = new LinkedHashMap<>();
        totais.forEach(t -> porCategoria.merge(t.getCategoria() != null ? t.getCategoria() : "Sem categoria",
                t.getTotal(), BigDecimal::add));

        return porCategoria.entrySet().stream()
                .map(e -> new DashboardResponseDto.BarItemDto(e.getKey(), e.getValue().doubleValue()))
                .toList();
    }
    private int calcularPercentual(BigDecimal parte, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) return 0;
        return parte.multiply(BigDecimal.valueOf(100))
                .divide(total, 0, RoundingMode.HALF_UP)
                .intValue();
    }
}
