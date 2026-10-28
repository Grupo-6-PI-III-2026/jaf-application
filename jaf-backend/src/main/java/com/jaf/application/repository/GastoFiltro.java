package com.jaf.application.repository;

import com.jaf.application.enums.StatusReembolso;
import com.jaf.application.model.Gasto;
import com.jaf.application.model.PagamentoReembolso;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Set;

public final class GastoFiltro {
    private GastoFiltro() {}
    public static Specification<Gasto> criar(Set<Long> obrasPermitidas, Long obraId, Long funcionarioId,
            String categoria, String metodo, StatusReembolso status) {
        return (root, query, cb) -> {
            var filtros = new ArrayList<Predicate>();
            if (obrasPermitidas != null) filtros.add(obrasPermitidas.isEmpty() ? cb.disjunction() : root.get("obra").get("id").in(obrasPermitidas));
            if (obraId != null) filtros.add(cb.equal(root.get("obra").get("id"), obraId));
            if (funcionarioId != null) filtros.add(cb.equal(root.get("funcionario").get("id"), funcionarioId));
            if (categoria != null && !categoria.isBlank()) filtros.add(cb.equal(root.get("categoria"), categoria));
            if (metodo != null && !metodo.isBlank()) filtros.add(cb.equal(root.get("metodoPagamento"), metodo));
            if (status != null) {
                filtros.add(cb.equal(root.get("metodoPagamento"), "REEMBOLSO"));
                var soma = query.subquery(BigDecimal.class);
                var pagamento = soma.from(PagamentoReembolso.class);
                soma.select(cb.coalesce(cb.sum(pagamento.<BigDecimal>get("valor")), BigDecimal.ZERO));
                soma.where(cb.equal(pagamento.get("gasto").get("id"), root.get("id")));
                filtros.add(switch (status) {
                    case PENDENTE -> cb.equal(soma, BigDecimal.ZERO);
                    case PARCIAL -> cb.and(cb.greaterThan(soma, BigDecimal.ZERO), cb.lessThan(soma, root.<BigDecimal>get("valor")));
                    case PAGO -> cb.greaterThanOrEqualTo(soma, root.<BigDecimal>get("valor"));
                });
            }
            return cb.and(filtros.toArray(Predicate[]::new));
        };
    }
}