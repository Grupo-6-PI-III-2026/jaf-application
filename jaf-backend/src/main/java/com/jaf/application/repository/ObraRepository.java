package com.jaf.application.repository;

import com.jaf.application.model.Obra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ObraRepository extends JpaRepository<Obra, Long> {
    boolean existsByTitulo (String titulo);
    List<Obra> findByIdIn(Collection<Long> ids);
    Page<Obra> findByIdIn(Collection<Long> ids, Pageable pageable);
    Page<Obra> findByIdInAndStatus(Collection<Long> ids, String status, Pageable pageable);
    Page<Obra> findAllByStatus(String status, Pageable pageable);
}
