package com.jaf.application.service;

import com.jaf.application.exceptions.BadRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Locale;
import java.util.Set;

public final class PaginacaoUtil {
    private PaginacaoUtil() {}

    public static Pageable validar(int page, int size, String sortBy, String sortDir,
                                   Set<String> permitidos, int maxSize) {
        if (page < 0 || size < 1 || size > maxSize || !permitidos.contains(sortBy)
                || (!"asc".equalsIgnoreCase(sortDir) && !"desc".equalsIgnoreCase(sortDir))) {
            throw new BadRequest("Parâmetros de paginação inválidos.");
        }
        Sort.Direction direction = Sort.Direction.fromString(sortDir.toLowerCase(Locale.ROOT));
        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }
}
