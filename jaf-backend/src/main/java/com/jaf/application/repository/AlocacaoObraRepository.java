//ENTITY = REPRESENTA AS TABELAS NO BD
package com.jaf.application.repository;
import com.jaf.application.model.AlocacaoObra;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlocacaoObraRepository extends JpaRepository<AlocacaoObra, Long> {
    boolean existsByFuncionarioIdAndObraId(Long funcionarioId, Long obraId);
}
