package com.jaf.application.service;

import com.jaf.application.dto.PagamentoReembolsoRequest;
import com.jaf.application.dto.ReembolsoResponse;
import com.jaf.application.dto.ReembolsoResumo;
import com.jaf.application.enums.StatusReembolso;
import com.jaf.application.exceptions.BadRequest;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.model.Gasto;
import com.jaf.application.model.PagamentoReembolso;
import com.jaf.application.repository.GastoRepository;
import com.jaf.application.repository.PagamentoReembolsoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class ReembolsoService {
    private final GastoRepository gastos;
    private final PagamentoReembolsoRepository pagamentos;
    private final AcessoObraService acesso;

    public ReembolsoService(GastoRepository gastos, PagamentoReembolsoRepository pagamentos, AcessoObraService acesso) {
        this.gastos = gastos; this.pagamentos = pagamentos; this.acesso = acesso;
    }

    @Transactional(readOnly = true)
    public ReembolsoResponse buscar(Long id) {
        Gasto gasto = gastos.findById(id).orElseThrow(() -> new NotFoundException("Gasto não encontrado."));
        acesso.exigirObra(gasto.getObra().getId());
        exigirReembolso(gasto);
        return resposta(gasto);
    }

    @Transactional
    public ReembolsoResponse registrar(Long id, PagamentoReembolsoRequest request) {
        // Todos os escritores (incluindo edição e exclusão do gasto) usam o mesmo lock da linha pai.
        Gasto gasto = gastos.buscarParaAtualizar(id).orElseThrow(() -> new NotFoundException("Gasto não encontrado."));
        acesso.exigirRegistroReembolso(gasto.getObra().getId());
        exigirReembolso(gasto);
        BigDecimal valor = request.valor();
        if (valor == null || valor.signum() <= 0 || valor.scale() > 2 || valor.precision() - valor.scale() > 17)
            throw new BadRequest("Pagamento deve ser positivo, com até duas casas decimais.");
        if (request.dataPagamento() == null || request.dataPagamento().isAfter(LocalDate.now())
                || (gasto.getDtGasto() != null && request.dataPagamento().isBefore(gasto.getDtGasto())))
            throw new BadRequest("Data de pagamento deve estar entre a data do gasto e hoje.");
        if (request.observacao() != null && request.observacao().length() > 1000)
            throw new BadRequest("Observação deve ter no máximo 1000 caracteres.");
        BigDecimal novoTotal = pagamentos.totalPago(id).add(valor);
        if (novoTotal.compareTo(gasto.getValor()) > 0) throw new BadRequest("Pagamento excede o saldo pendente.");

        PagamentoReembolso pagamento = new PagamentoReembolso();
        pagamento.setGasto(gasto);
        pagamento.setValor(valor);
        pagamento.setDataPagamento(request.dataPagamento());
        pagamento.setObservacao(request.observacao());
        pagamentos.saveAndFlush(pagamento);
        gasto.setReembolsoConcluido(novoTotal.compareTo(gasto.getValor()) == 0);
        gastos.save(gasto);
        return resposta(gasto);
    }

    @Transactional(readOnly = true)
    public List<ReembolsoResumo> resumo(Long obraId) {
        acesso.exigirObra(obraId);
        List<Gasto> lista = gastos.findByObraIdAndMetodoPagamento(obraId, "REEMBOLSO");
        Map<Long, BigDecimal> pagos = totais(lista);
        Map<Long, List<Gasto>> porPessoa = lista.stream().collect(Collectors.groupingBy(g -> g.getFuncionario().getId(), TreeMap::new, Collectors.toList()));
        return porPessoa.entrySet().stream().map(entry -> {
            BigDecimal total = entry.getValue().stream().map(Gasto::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal pago = entry.getValue().stream().map(g -> pagos.getOrDefault(g.getId(), BigDecimal.ZERO)).reduce(BigDecimal.ZERO, BigDecimal::add);
            return new ReembolsoResumo(entry.getKey(), entry.getValue().getFirst().getFuncionario().getNome(), total, pago, total.subtract(pago));
        }).toList();
    }

    public Map<Long, BigDecimal> totais(List<Gasto> lista) {
        if (lista.isEmpty()) return Map.of();
        return pagamentos.totaisPorGasto(lista.stream().map(Gasto::getId).toList()).stream()
                .collect(Collectors.toMap(PagamentoReembolsoRepository.TotalPorGasto::getGastoId,
                        PagamentoReembolsoRepository.TotalPorGasto::getTotalPago));
    }

    public static StatusReembolso status(BigDecimal valor, BigDecimal pago) {
        if (pago.signum() == 0) return StatusReembolso.PENDENTE;
        return pago.compareTo(valor) >= 0 ? StatusReembolso.PAGO : StatusReembolso.PARCIAL;
    }

    private void exigirReembolso(Gasto gasto) {
        if (!"REEMBOLSO".equals(gasto.getMetodoPagamento())) throw new BadRequest("Gasto não é um reembolso.");
    }

    private ReembolsoResponse resposta(Gasto gasto) {
        var lista = pagamentos.findByGastoIdOrderByDataPagamentoAscIdAsc(gasto.getId());
        BigDecimal pago = lista.stream().map(PagamentoReembolso::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ReembolsoResponse(gasto.getId(), gasto.getValor(), pago, gasto.getValor().subtract(pago), status(gasto.getValor(), pago),
                lista.stream().map(p -> new ReembolsoResponse.Pagamento(p.getId(), p.getValor(), p.getDataPagamento(), p.getObservacao())).toList());
    }
}