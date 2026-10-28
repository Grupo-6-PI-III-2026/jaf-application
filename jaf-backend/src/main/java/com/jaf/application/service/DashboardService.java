package com.jaf.application.service;

import com.jaf.application.dto.DashboardResponseDto;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.model.Gasto;
import com.jaf.application.model.Obra;
import com.jaf.application.repository.GastoRepository;
import com.jaf.application.repository.ObraRepository;
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

    private final GastoRepository gastoRepository;
    private final ObraRepository obraRepository;
    private final AcessoObraService acesso;
    private final ReembolsoService reembolsoService;

    public DashboardService(GastoRepository gastoRepository, ObraRepository obraRepository,
                            AcessoObraService acesso, ReembolsoService reembolsoService) {
        this.gastoRepository = gastoRepository;
        this.obraRepository = obraRepository;
        this.acesso = acesso;
        this.reembolsoService = reembolsoService;
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

        BigDecimal orcamento = parseOrcamento(obra.getOrcamento());

        List<Gasto> todosGastos = gastoRepository.findByObraId(obraId);
        List<Gasto> gastosPeriodo = todosGastos.stream()
                .filter(g -> dentroDoPeriodo(g.getDtGasto(), from, to))
                .toList();

        List<Gasto> gastosEtapaFiltrado = "TODAS".equals(etapa)
            ? gastosPeriodo
            : gastosPeriodo.stream().filter(g -> etapa.equals(g.getEtapa())).toList();

        // StatCard 1: gastos da etapa selecionada
        BigDecimal totalGastosEtapa = somarValores(gastosEtapaFiltrado);
        int progressoEtapa = calcularPercentual(totalGastosEtapa, orcamento);

        // Recebido e saldo acumulados de toda a obra, sem filtros de etapa ou período.
        // Parcelas não são novas despesas; orçamento não é recebido.
        BigDecimal totalGastosObra = somarValores(todosGastos);
        BigDecimal saldoRestante = obra.getTotalRecebido().subtract(totalGastosObra);
        int progressoSaldo = calcularPercentual(totalGastosObra, obra.getTotalRecebido());

        // StatCard 2: reembolsos pendentes (filtrado pela etapa e período)
        List<Gasto> reembolsos = gastosPeriodo.stream()
            .filter(g -> "REEMBOLSO".equals(g.getMetodoPagamento()))
            .filter(g -> "TODAS".equals(etapa) || etapa.equals(g.getEtapa()))
            .toList();

        Map<Long, BigDecimal> pagos = reembolsoService.totais(reembolsos);
        BigDecimal totalReembolsos = somarValores(reembolsos);
        BigDecimal totalPago = pagos.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal reembolsosPendentes = totalReembolsos.subtract(totalPago);

        // Pizza: proporção monetária paga vs pendente, incluindo pagamentos parciais.
        List<DashboardResponseDto.PieItemDto> pizza = List.of(
            new DashboardResponseDto.PieItemDto("Concluídos", calcularPercentual(totalPago, totalReembolsos)),
            new DashboardResponseDto.PieItemDto("Pendentes", totalReembolsos.signum() == 0 ? 0 : 100 - calcularPercentual(totalPago, totalReembolsos)));

        // Linha: gastos imprevistos agrupados por mês
        List<DashboardResponseDto.LineItemDto> linha = calcularLinha(gastosEtapaFiltrado);

        // Barra: gastos por categoria (filtrado pela etapa selecionada)
        List<DashboardResponseDto.BarItemDto> barra = calcularBarra(gastosEtapaFiltrado);

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

    private List<DashboardResponseDto.LineItemDto> calcularLinha(List<Gasto> todosGastos) {
        Map<YearMonth, BigDecimal> porMes = new TreeMap<>();
        todosGastos.stream()
                .filter(g -> g.getCategoria() != null && CATEGORIAS_IMPREVISTOS.contains(g.getCategoria()))
                .forEach(g -> porMes.merge(YearMonth.from(g.getDtGasto()), g.getValor(), BigDecimal::add));

        return porMes.entrySet().stream()
                .map(e -> {
                    String mes = e.getKey().getMonth()
                            .getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));
                    String mesCapitalizado = Character.toUpperCase(mes.charAt(0)) + mes.substring(1);
                    return new DashboardResponseDto.LineItemDto(mesCapitalizado, e.getValue().doubleValue());
                })
                .toList();
    }

    private List<DashboardResponseDto.BarItemDto> calcularBarra(List<Gasto> gastos) {
        Map<String, BigDecimal> porCategoria = new LinkedHashMap<>();
        gastos.forEach(g -> {
            String cat = g.getCategoria() != null ? g.getCategoria() : "Sem categoria";
            porCategoria.merge(cat, g.getValor(), BigDecimal::add);
        });

        return porCategoria.entrySet().stream()
                .map(e -> new DashboardResponseDto.BarItemDto(e.getKey(), e.getValue().doubleValue()))
                .toList();
    }

    private BigDecimal somarValores(List<Gasto> gastos) {
        return gastos.stream()
                .map(Gasto::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int calcularPercentual(BigDecimal parte, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) return 0;
        return parte.multiply(BigDecimal.valueOf(100))
                .divide(total, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private BigDecimal parseOrcamento(String orcamento) {
        try {
            return new BigDecimal(orcamento);
        } catch (NumberFormatException | NullPointerException e) {
            return BigDecimal.ZERO;
        }
    }

    private boolean dentroDoPeriodo(LocalDate data, LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return true;
        }
        if (data == null) {
            return false;
        }
        if (from != null && data.isBefore(from)) {
            return false;
        }
        if (to != null && data.isAfter(to)) {
            return false;
        }
        return true;
    }
}
