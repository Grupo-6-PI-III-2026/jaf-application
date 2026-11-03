package com.jaf.application.repository;

import com.jaf.application.model.PagamentoReembolso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface PagamentoReembolsoRepository extends JpaRepository<PagamentoReembolso, Long> {
    List<PagamentoReembolso> findByGastoIdAndCanceladoEmIsNullOrderByDataPagamentoAscIdAsc(Long gastoId);
    List<PagamentoReembolso> findByGastoIdAndCanceladoEmIsNotNullOrderByDataPagamentoAscIdAsc(Long gastoId);
    List<PagamentoReembolso> findByGastoIdOrderByDataPagamentoAscIdAsc(Long gastoId);
    @Query("select coalesce(sum(p.valor), 0) from PagamentoReembolso p where p.gasto.id = :gastoId and p.canceladoEm is null")
    BigDecimal totalPago(Long gastoId);
    boolean existsByGastoId(Long gastoId);
    boolean existsByGastoIdAndDataPagamentoBeforeAndCanceladoEmIsNull(Long gastoId, LocalDate dataPagamento);
    boolean existsByGastoIdAndDataPagamentoBefore(Long gastoId, LocalDate dataPagamento);

    interface TotalPorGasto {
        Long getGastoId();
        BigDecimal getTotalPago();
    }
    @Query("select p.gasto.id as gastoId, sum(p.valor) as totalPago from PagamentoReembolso p where p.gasto.id in :ids and p.canceladoEm is null group by p.gasto.id")
    List<TotalPorGasto> totaisPorGasto(Collection<Long> ids);
}