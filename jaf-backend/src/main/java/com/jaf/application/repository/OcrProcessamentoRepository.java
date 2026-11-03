package com.jaf.application.repository;

import com.jaf.application.model.OcrProcessamento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import com.jaf.application.enums.StatusOcr;

public interface OcrProcessamentoRepository extends JpaRepository<OcrProcessamento, String> {
    List<OcrProcessamento> findByStatusAndCriadoEmBefore(StatusOcr status, LocalDateTime limite);
    List<OcrProcessamento> findByStatusAndAtualizadoEmBefore(StatusOcr status, LocalDateTime limite);
}
