package com.jaf.application.repository;

import com.jaf.application.model.Obra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ObraRepository extends JpaRepository<Obra, Long> {
    boolean existsByTitulo (String titulo);
    List<Obra> findByIdIn(Collection<Long> ids);
}
