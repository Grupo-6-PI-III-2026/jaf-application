package com.jaf.application.dto;

import java.math.BigDecimal;
import java.util.List;

public class DashboardResponseDto {

    private final BigDecimal gastosEtapa;
    private final int progressoEtapa;
    private final BigDecimal reembolsosPendentes;
    private final BigDecimal saldoRestante;
    private final int progressoSaldo;
    private final List<PieItemDto> reembolsosPizza;
    private final List<LineItemDto> gastosImprevistos;
    private final List<BarItemDto> gastosPorCategoria;

    public DashboardResponseDto(
            BigDecimal gastosEtapa,
            int progressoEtapa,
            BigDecimal reembolsosPendentes,
            BigDecimal saldoRestante,
            int progressoSaldo,
            List<PieItemDto> reembolsosPizza,
            List<LineItemDto> gastosImprevistos,
            List<BarItemDto> gastosPorCategoria) {
        this.gastosEtapa = gastosEtapa;
        this.progressoEtapa = progressoEtapa;
        this.reembolsosPendentes = reembolsosPendentes;
        this.saldoRestante = saldoRestante;
        this.progressoSaldo = progressoSaldo;
        this.reembolsosPizza = reembolsosPizza;
        this.gastosImprevistos = gastosImprevistos;
        this.gastosPorCategoria = gastosPorCategoria;
    }

    public BigDecimal getGastosEtapa()                  { return gastosEtapa; }
    public int getProgressoEtapa()                      { return progressoEtapa; }
    public BigDecimal getReembolsosPendentes()           { return reembolsosPendentes; }
    public BigDecimal getSaldoRestante()                 { return saldoRestante; }
    public int getProgressoSaldo()                      { return progressoSaldo; }
    public List<PieItemDto> getReembolsosPizza()        { return reembolsosPizza; }
    public List<LineItemDto> getGastosImprevistos()     { return gastosImprevistos; }
    public List<BarItemDto> getGastosPorCategoria()     { return gastosPorCategoria; }

    public record PieItemDto(String name, double value) {}
    public record LineItemDto(String mes, double valor) {}
    public record BarItemDto(String categoria, double valor) {}
}
