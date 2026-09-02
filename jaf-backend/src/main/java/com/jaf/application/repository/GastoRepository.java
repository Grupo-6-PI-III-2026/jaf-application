package com.jaf.application.repository;

import com.jaf.application.model.Gasto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface GastoRepository extends JpaRepository<Gasto, Long> {
    List<Gasto> findByObraId(Long obraId);
    List<Gasto> findByObraIdIn(Collection<Long> obraIds);
    List<Gasto> findByObraIdAndEtapa(Long obraId, String etapa);
    List<Gasto> findByObraIdAndMetodoPagamento(Long obraId, String metodoPagamento);
    List<Gasto> findByObraIdAndReembolsoConcluidoIsNotNull(Long obraId);
}
