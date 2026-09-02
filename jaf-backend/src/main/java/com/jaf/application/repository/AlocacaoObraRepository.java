//ENTITY = REPRESENTA AS TABELAS NO BD
package com.jaf.application.repository;
import com.jaf.application.model.AlocacaoObra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlocacaoObraRepository extends JpaRepository<AlocacaoObra, Long> {
    boolean existsByFuncionarioIdAndObraId(Long funcionarioId, Long obraId);
    List<AlocacaoObra> findByFuncionarioId(Long funcionarioId);
    List<AlocacaoObra> findByObraId(Long obraId);
    List<AlocacaoObra> findByFuncionarioIdAndObraId(Long funcionarioId, Long obraId);
}
