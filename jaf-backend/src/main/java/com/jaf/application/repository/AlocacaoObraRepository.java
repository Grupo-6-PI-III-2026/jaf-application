//ENTITY = REPRESENTA AS TABELAS NO BD
package com.jaf.application.repository;
import com.jaf.application.model.AlocacaoObra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlocacaoObraRepository extends JpaRepository<AlocacaoObra, Long> {
    boolean existsByFuncionarioIdAndObraId(Long funcionarioId, Long obraId);
    @Query("""
            select count(a) > 0 from AlocacaoObra a
            where a.funcionario.id = :funcionarioId and a.obra.id = :obraId
              and (a.dtInicio is null or a.dtInicio <= :data)
              and (a.dtFim is null or a.dtFim >= :data)
            """)
    boolean existsAtiva(@Param("funcionarioId") Long funcionarioId,
                        @Param("obraId") Long obraId,
                        @Param("data") LocalDate data);
    @Query("""
            select count(a) > 0 from AlocacaoObra a
            where a.funcionario.id = :funcionarioId and a.obra.id = :obraId
              and a.id <> coalesce(:id, -1)
              and (a.dtFim is null or a.dtFim >= :inicio)
              and (:fim is null or a.dtInicio is null or a.dtInicio <= :fim)
            """)
    boolean existsSobreposta(@Param("funcionarioId") Long funcionarioId,
                             @Param("obraId") Long obraId,
                             @Param("inicio") LocalDate inicio,
                             @Param("fim") LocalDate fim,
                             @Param("id") Long id);
    boolean existsByObraId(Long obraId);
    boolean existsByFuncionarioId(Long funcionarioId);
    List<AlocacaoObra> findByFuncionarioId(Long funcionarioId);
    List<AlocacaoObra> findByObraId(Long obraId);
    List<AlocacaoObra> findByFuncionarioIdAndObraId(Long funcionarioId, Long obraId);
}
