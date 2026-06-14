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
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;

@Service
public class DashboardService {

    private static final String CATEGORIA_IMPREVISTOS = "Custos extras";

    private final GastoRepository gastoRepository;
    private final ObraRepository obraRepository;

    public DashboardService(GastoRepository gastoRepository, ObraRepository obraRepository) {
        this.gastoRepository = gastoRepository;
        this.obraRepository = obraRepository;
    }

    public DashboardResponseDto buscarStats(Long obraId, String etapa) {
        Obra obra = obraRepository.findById(obraId)
                .orElseThrow(() -> new NotFoundException("Obra não encontrada."));

        BigDecimal orcamento = parseOrcamento(obra.getOrcamento());

        List<Gasto> todosGastos = gastoRepository.findByObraId(obraId);

        List<Gasto> gastosEtapaFiltrado = "TODAS".equals(etapa)
                ? todosGastos
                : todosGastos.stream().filter(g -> etapa.equals(g.getEtapa())).toList();

        // StatCard 1: gastos da etapa selecionada
        BigDecimal totalGastosEtapa = somarValores(gastosEtapaFiltrado);
        int progressoEtapa = calcularPercentual(totalGastosEtapa, orcamento);

        // StatCard 3: saldo restante (escopo da obra inteira)
        BigDecimal totalGastosObra = somarValores(todosGastos);
        BigDecimal saldoRestante = orcamento.subtract(totalGastosObra);
        int progressoSaldo = calcularPercentual(totalGastosObra, orcamento);

        // StatCard 2: reembolsos pendentes (escopo da obra inteira)
        List<Gasto> reembolsos = gastoRepository.findByObraIdAndReembolsoConcluidoIsNotNull(obraId);

        BigDecimal reembolsosPendentes = reembolsos.stream()
                .filter(g -> !Boolean.TRUE.equals(g.getReembolsoConcluido()))
                .map(Gasto::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Pizza: % concluídos vs pendentes
        List<DashboardResponseDto.PieItemDto> pizza = calcularPizza(reembolsos);

        // Linha: gastos imprevistos agrupados por mês
        List<DashboardResponseDto.LineItemDto> linha = calcularLinha(todosGastos);

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

    private List<DashboardResponseDto.PieItemDto> calcularPizza(List<Gasto> reembolsos) {
        if (reembolsos.isEmpty()) {
            return List.of(
                    new DashboardResponseDto.PieItemDto("Concluídos", 0),
                    new DashboardResponseDto.PieItemDto("Pendentes", 0)
            );
        }
        long total = reembolsos.size();
        long concluidos = reembolsos.stream().filter(g -> Boolean.TRUE.equals(g.getReembolsoConcluido())).count();
        long pendentes = total - concluidos;
        return List.of(
                new DashboardResponseDto.PieItemDto("Concluídos", Math.round((double) concluidos / total * 100)),
                new DashboardResponseDto.PieItemDto("Pendentes", Math.round((double) pendentes / total * 100))
        );
    }

    private List<DashboardResponseDto.LineItemDto> calcularLinha(List<Gasto> todosGastos) {
        Map<YearMonth, BigDecimal> porMes = new TreeMap<>();
        todosGastos.stream()
                .filter(g -> CATEGORIA_IMPREVISTOS.equals(g.getCategoria()))
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
}
