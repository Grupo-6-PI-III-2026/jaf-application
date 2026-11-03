package com.jaf.application.repository;

import com.jaf.application.model.RecebimentoObra;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;

public interface RecebimentoObraRepository extends JpaRepository<RecebimentoObra, Long> {
    List<RecebimentoObra> findByObraIdOrderByDataAscIdAsc(Long obraId);
    @Query("select coalesce(sum(r.valor), 0) from RecebimentoObra r where r.obra.id = :obraId")
    BigDecimal totalPorObra(@Param("obraId") Long obraId);
}
