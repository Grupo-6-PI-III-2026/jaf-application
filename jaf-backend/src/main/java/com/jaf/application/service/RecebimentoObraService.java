package com.jaf.application.service;

import com.jaf.application.dto.RecebimentoObraRequest;
import com.jaf.application.exceptions.*;
import com.jaf.application.model.*;
import com.jaf.application.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class RecebimentoObraService {
    private final RecebimentoObraRepository recebimentos;
    private final ObraRepository obras;
    private final AcessoObraService acesso;
    private final Clock clock;
    public RecebimentoObraService(RecebimentoObraRepository recebimentos, ObraRepository obras,
                                  AcessoObraService acesso, Clock clock) {
        this.recebimentos = recebimentos; this.obras = obras; this.acesso = acesso; this.clock = clock;
    }
    @Transactional(readOnly = true)
    public List<RecebimentoObra> listar(Long obraId) { acesso.exigirAdmin(); obra(obraId); return recebimentos.findByObraIdOrderByDataAscIdAsc(obraId); }
    @Transactional
    public RecebimentoObra criar(Long obraId, RecebimentoObraRequest request) {
        acesso.exigirAdmin();
        Obra obra = obra(obraId);
        RecebimentoObra r = new RecebimentoObra();
        r.setObra(obra); r.setValor(request.valor()); r.setData(request.data()); r.setDescricao(request.descricao()); r.setCriadoPor(acesso.atual().getId());
        RecebimentoObra salvo = recebimentos.save(r);
        sincronizar(obra);
        return salvo;
    }
    @Transactional
    public void estornar(Long obraId, Long id) {
        acesso.exigirAdmin(); Obra obra = obra(obraId);
        RecebimentoObra original = recebimentos.findById(id).orElseThrow(() -> new NotFoundException("Recebimento não encontrado."));
        if (!original.getObra().getId().equals(obraId)) throw new NotFoundException("Recebimento não encontrado.");
        RecebimentoObra ajuste = new RecebimentoObra();
        ajuste.setObra(obra); ajuste.setValor(original.getValor().negate()); ajuste.setData(LocalDate.now(clock));
        ajuste.setDescricao("Estorno de recebimento #" + id); ajuste.setCriadoPor(acesso.atual().getId());
        recebimentos.save(ajuste); sincronizar(obra);
    }
    private Obra obra(Long id) { return obras.findById(id).orElseThrow(() -> new NotFoundException("Obra não encontrada.")); }
    private void sincronizar(Obra obra) { obra.setTotalRecebido(recebimentos.totalPorObra(obra.getId())); obras.save(obra); }
}
