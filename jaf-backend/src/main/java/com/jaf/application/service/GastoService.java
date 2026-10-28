package com.jaf.application.service;

import com.jaf.application.dto.GastoDto;
import com.jaf.application.enums.Cargo;
import com.jaf.application.enums.StatusReembolso;
import com.jaf.application.exceptions.BadRequest;
import com.jaf.application.exceptions.Conflict;
import com.jaf.application.exceptions.Forbidden;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.model.AlocacaoObra;
import com.jaf.application.model.Funcionario;
import com.jaf.application.model.Gasto;
import com.jaf.application.model.Obra;
import com.jaf.application.repository.AlocacaoObraRepository;
import com.jaf.application.repository.FuncionarioRepository;
import com.jaf.application.repository.GastoRepository;
import com.jaf.application.repository.GastoFiltro;
import com.jaf.application.repository.PagamentoReembolsoRepository;
import com.jaf.application.repository.ObraRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class GastoService {
    private final GastoRepository gastoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ObraRepository obraRepository;
    private final AlocacaoObraRepository alocacaoObraRepository;
    private final AcessoObraService acesso;
    private final PagamentoReembolsoRepository pagamentos;

    public GastoService(GastoRepository gastoRepository,
                        FuncionarioRepository funcionarioRepository,
                        ObraRepository obraRepository,
                        AlocacaoObraRepository alocacaoObraRepository,
                        AcessoObraService acesso,
                        PagamentoReembolsoRepository pagamentos) {
        this.gastoRepository = gastoRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.obraRepository = obraRepository;
        this.alocacaoObraRepository = alocacaoObraRepository;
        this.acesso = acesso;
        this.pagamentos = pagamentos;
    }

    @Transactional
    public Gasto criar(GastoDto dto) {
        acesso.exigirObra(dto.getObraId());
        validarFlag(dto, BigDecimal.ZERO);
        Funcionario funcionario = funcionarioRepository.findById(dto.getFuncionarioId())
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado."));
        Obra obra = obraRepository.findById(dto.getObraId())
                .orElseThrow(() -> new NotFoundException("Obra nao encontrada."));

        if (!alocacaoObraRepository.existsByFuncionarioIdAndObraId(funcionario.getId(), obra.getId())) {
            throw new Forbidden("Funcionario nao esta alocado nesta obra e nao pode registrar gastos.");
        }

        Gasto gasto = new Gasto();
        gasto.setDescricao(dto.getDescricao());
        gasto.setCategoria(dto.getCategoria());
        gasto.setMetodoPagamento(dto.getMetodoPagamento());
        gasto.setEtapa(dto.getEtapa());
        gasto.setValor(dto.getValor());
        gasto.setDtGasto(dto.getDtGasto());
        gasto.setReembolsoConcluido("REEMBOLSO".equals(dto.getMetodoPagamento()) ? false : null);
        gasto.setFuncionario(funcionario);
        gasto.setObra(obra);
        return gastoRepository.save(gasto);
    }

    public List<Gasto> listarPorUsuario(String email, Long obraId) {
        Funcionario funcionario = funcionarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado."));

        if (funcionario.getCargoGlobal() == Cargo.ADMIN) {
            if (obraId != null) {
                return gastoRepository.findByObraId(obraId);
            }
            return gastoRepository.findAll();
        }

        List<AlocacaoObra> alocacoes = alocacaoObraRepository.findByFuncionarioId(funcionario.getId());
        if (alocacoes.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> obraIds = new HashSet<>();
        alocacoes.forEach(alocacao -> obraIds.add(alocacao.getObra().getId()));
        if (obraId != null) {
            if (!obraIds.contains(obraId)) {
                return Collections.emptyList();
            }
            return gastoRepository.findByObraId(obraId);
        }
        return gastoRepository.findByObraIdIn(obraIds);
    }

    public Page<Gasto> listarPaginadoPorUsuario(String email, Long obraId, int page, int size, String sortBy, String sortDir, String categoria, String metodoPagamento) {
        return listarPaginadoFiltrado(obraId, null, categoria, metodoPagamento, null, page, size, sortBy, sortDir);
    }

    public Gasto buscarPorId(Long id) {
        return gastoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Gasto nao encontrado."));
    }

    public Gasto buscarPorIdComEscopo(Long id, String email) {
        Funcionario funcionario = funcionarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado."));
        Gasto gasto = buscarPorId(id);

        if (funcionario.getCargoGlobal() == Cargo.ADMIN) {
            return gasto;
        }

        boolean alocado = alocacaoObraRepository.existsByFuncionarioIdAndObraId(funcionario.getId(), gasto.getObra().getId());
        if (!alocado) {
            throw new Forbidden("Funcionario nao possui acesso a este gasto.");
        }
        return gasto;
    }

    @Transactional
    public Gasto atualizar(Long id, GastoDto dto) {
        Gasto existente = gastoRepository.buscarParaAtualizar(id)
                .orElseThrow(() -> new NotFoundException("Gasto nao encontrado."));
        acesso.exigirObra(existente.getObra().getId());
        acesso.exigirObra(dto.getObraId());
        BigDecimal pago = pagamentos.totalPago(id);
        if (pagamentos.existsByGastoId(id)) {
            if (!existente.getFuncionario().getId().equals(dto.getFuncionarioId())
                    || !existente.getObra().getId().equals(dto.getObraId())
                    || !existente.getMetodoPagamento().equals(dto.getMetodoPagamento()))
                throw new Conflict("Gasto com pagamentos não permite alterar dono, obra ou método.");
            if (dto.getValor().compareTo(pago) < 0)
                throw new Conflict("Valor do gasto não pode ser menor que o total pago.");
            if (pagamentos.existsByGastoIdAndDataPagamentoBefore(id, dto.getDtGasto())) {
                throw new Conflict("Data do gasto não pode ser posterior a um pagamento já registrado.");
            }
        }
        validarFlag(dto, pago);
        Funcionario funcionario = funcionarioRepository.findById(dto.getFuncionarioId())
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado."));
        Obra obra = obraRepository.findById(dto.getObraId())
                .orElseThrow(() -> new NotFoundException("Obra nao encontrada."));

        if (!alocacaoObraRepository.existsByFuncionarioIdAndObraId(funcionario.getId(), obra.getId())) {
            throw new Forbidden("Funcionario nao esta alocado nesta obra.");
        }

        existente.setDescricao(dto.getDescricao());
        existente.setCategoria(dto.getCategoria());
        existente.setMetodoPagamento(dto.getMetodoPagamento());
        existente.setEtapa(dto.getEtapa());
        existente.setValor(dto.getValor());
        existente.setDtGasto(dto.getDtGasto());
        existente.setReembolsoConcluido("REEMBOLSO".equals(dto.getMetodoPagamento()) ? pago.compareTo(dto.getValor()) == 0 : null);
        existente.setFuncionario(funcionario);
        existente.setObra(obra);
        return gastoRepository.save(existente);
    }

    @Transactional
    public void deletar(Long id) {
        Gasto gasto = gastoRepository.buscarParaAtualizar(id)
                .orElseThrow(() -> new NotFoundException("Gasto nao encontrado."));
        acesso.exigirObra(gasto.getObra().getId());
        if (pagamentos.existsByGastoId(id))
            throw new Conflict("Gasto possui pagamentos; histórico não pode ser excluído.");
        gastoRepository.deleteById(id);
    }

    private void validarFlag(GastoDto dto, BigDecimal pago) {
        boolean concluido = "REEMBOLSO".equals(dto.getMetodoPagamento()) && pago.compareTo(dto.getValor()) == 0;
        if (Boolean.TRUE.equals(dto.getReembolsoConcluido()) && !concluido)
            throw new BadRequest("Registre a quitação pelo endpoint de reembolsos, não pela flag legada.");
    }

    private Specification<Gasto> filtro(Long obraId, Long funcionarioId,
            String categoria, String metodo, StatusReembolso status) {
        Funcionario usuario = acesso.atual();
        Set<Long> permitidas = usuario.getCargoGlobal() == Cargo.ADMIN ? null :
                alocacaoObraRepository.findByFuncionarioId(usuario.getId()).stream()
                        .map(a -> a.getObra().getId()).collect(java.util.stream.Collectors.toSet());
        return GastoFiltro.criar(permitidas, obraId, funcionarioId, categoria, metodo, status);
    }

    public List<Gasto> listarFiltrado(Long obraId, Long funcionarioId, String categoria, String metodo,
            StatusReembolso status) {
        return gastoRepository.findAll(filtro(obraId, funcionarioId, categoria, metodo, status));
    }

    public Page<Gasto> listarPaginadoFiltrado(Long obraId, Long funcionarioId, String categoria, String metodo,
            StatusReembolso status, int page, int size, String sortBy, String sortDir) {
        if (page < 0 || size < 1 || size > 200 || !Set.of("id", "dtGasto", "valor", "descricao", "categoria", "metodoPagamento", "etapa").contains(sortBy)
                || !("asc".equalsIgnoreCase(sortDir) || "desc".equalsIgnoreCase(sortDir)))
            throw new BadRequest("Paginação ou ordenação inválida (size: 1 a 200).");
        return gastoRepository.findAll(filtro(obraId, funcionarioId, categoria, metodo, status),
                PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), sortBy)));
    }
}
