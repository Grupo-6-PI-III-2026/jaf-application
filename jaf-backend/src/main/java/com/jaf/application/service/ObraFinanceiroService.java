package com.jaf.application.service;

import com.jaf.application.dto.ObraFinanceiroDto;
import com.jaf.application.dto.ObraFinanceiroRequest;
import com.jaf.application.exceptions.BadRequest;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.model.Obra;
import com.jaf.application.repository.GastoRepository;
import com.jaf.application.repository.ObraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
public class ObraFinanceiroService {
    private final ObraRepository obras;
    private final GastoRepository gastos;
    private final AcessoObraService acesso;

    public ObraFinanceiroService(ObraRepository obras, GastoRepository gastos, AcessoObraService acesso) {
        this.obras = obras; this.gastos = gastos; this.acesso = acesso;
    }

    @Transactional(readOnly = true)
    public ObraFinanceiroDto buscar(Long id) {
        acesso.exigirAdmin();
        return resposta(obra(id));
    }

    @Transactional
    public ObraFinanceiroDto atualizar(Long id, ObraFinanceiroRequest request) {
        acesso.exigirAdmin();
        BigDecimal valor = request.totalRecebido();
        if (valor == null || valor.signum() < 0 || valor.scale() > 2 || valor.precision() - valor.scale() > 17)
            throw new BadRequest("Total recebido deve ser não negativo e ter até duas casas decimais.");
        Obra obra = obra(id);
        obra.setTotalRecebido(valor);
        obras.save(obra);
        return resposta(obra);
    }

    private Obra obra(Long id) {
        return obras.findById(id).orElseThrow(() -> new NotFoundException("Obra não encontrada."));
    }

    private ObraFinanceiroDto resposta(Obra obra) {
        BigDecimal total = gastos.findByObraId(obra.getId()).stream()
                .map(g -> g.getValor()).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ObraFinanceiroDto(obra.getTotalRecebido(), total, obra.getTotalRecebido().subtract(total));
    }
}