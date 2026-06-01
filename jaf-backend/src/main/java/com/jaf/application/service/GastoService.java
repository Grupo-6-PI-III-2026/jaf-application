package com.jaf.application.service;

import com.jaf.application.dto.GastoDto;
import com.jaf.application.enums.Cargo;
import com.jaf.application.enums.TipoFuncionario;
import com.jaf.application.exceptions.Forbidden;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.model.AlocacaoObra;
import com.jaf.application.model.Funcionario;
import com.jaf.application.model.Gasto;
import com.jaf.application.model.Obra;
import com.jaf.application.repository.AlocacaoObraRepository;
import com.jaf.application.repository.FuncionarioRepository;
import com.jaf.application.repository.GastoRepository;
import com.jaf.application.repository.ObraRepository;
import org.springframework.stereotype.Service;

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

    public GastoService(GastoRepository gastoRepository,
                        FuncionarioRepository funcionarioRepository,
                        ObraRepository obraRepository,
                        AlocacaoObraRepository alocacaoObraRepository) {
        this.gastoRepository = gastoRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.obraRepository = obraRepository;
        this.alocacaoObraRepository = alocacaoObraRepository;
    }

    public Gasto criar(GastoDto dto) {
        Funcionario funcionario = funcionarioRepository.findById(dto.getFuncionarioId())
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado."));
        Obra obra = obraRepository.findById(dto.getObraId())
                .orElseThrow(() -> new NotFoundException("Obra nao encontrada."));

        // Funcionários externos não precisam estar alocados para ter gastos registrados
        if (funcionario.getTipoFuncionario() != TipoFuncionario.EXTERNO) {
            if (!alocacaoObraRepository.existsByFuncionarioIdAndObraId(funcionario.getId(), obra.getId())) {
                throw new Forbidden("Funcionario nao esta alocado nesta obra e nao pode registrar gastos.");
            }
        }

        Gasto gasto = new Gasto();
        gasto.setDescricao(dto.getDescricao());
        gasto.setCategoria(dto.getCategoria());
        gasto.setMetodoPagamento(dto.getMetodoPagamento());
        gasto.setEtapa(dto.getEtapa());
        gasto.setValor(dto.getValor());
        gasto.setDtGasto(dto.getDtGasto());
        gasto.setFuncionario(funcionario);
        gasto.setObra(obra);
        return gastoRepository.save(gasto);
    }

    public List<Gasto> listarPorUsuario(String email, Long obraId) {
        Funcionario funcionario = funcionarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado."));

        if (funcionario.getTipoFuncionario() == TipoFuncionario.EXTERNO) {
            throw new Forbidden("Funcionarios externos nao podem visualizar gastos.");
        }

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

    public Gasto buscarPorId(Long id) {
        return gastoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Gasto nao encontrado."));
    }

    public Gasto buscarPorIdComEscopo(Long id, String email) {
        Funcionario funcionario = funcionarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado."));
        Gasto gasto = buscarPorId(id);

        if (funcionario.getTipoFuncionario() == TipoFuncionario.EXTERNO) {
            throw new Forbidden("Funcionarios externos nao podem visualizar gastos.");
        }

        if (funcionario.getCargoGlobal() == Cargo.ADMIN) {
            return gasto;
        }

        boolean alocado = alocacaoObraRepository.existsByFuncionarioIdAndObraId(funcionario.getId(), gasto.getObra().getId());
        if (!alocado) {
            throw new Forbidden("Funcionario nao possui acesso a este gasto.");
        }
        return gasto;
    }

    public Gasto atualizar(Long id, GastoDto dto) {
        Gasto existente = buscarPorId(id);
        Funcionario funcionario = funcionarioRepository.findById(dto.getFuncionarioId())
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado."));
        Obra obra = obraRepository.findById(dto.getObraId())
                .orElseThrow(() -> new NotFoundException("Obra nao encontrada."));

        // Funcionários externos não precisam estar alocados para ter gastos registrados
        if (funcionario.getTipoFuncionario() != TipoFuncionario.EXTERNO) {
            if (!alocacaoObraRepository.existsByFuncionarioIdAndObraId(funcionario.getId(), obra.getId())) {
                throw new Forbidden("Funcionario nao esta alocado nesta obra.");
            }
        }

        existente.setDescricao(dto.getDescricao());
        existente.setCategoria(dto.getCategoria());
        existente.setMetodoPagamento(dto.getMetodoPagamento());
        existente.setEtapa(dto.getEtapa());
        existente.setValor(dto.getValor());
        existente.setDtGasto(dto.getDtGasto());
        existente.setFuncionario(funcionario);
        existente.setObra(obra);
        return gastoRepository.save(existente);
    }

    public void deletar(Long id) {
        if (!gastoRepository.existsById(id)) {
            throw new NotFoundException("Gasto nao encontrado");
        }
        gastoRepository.deleteById(id);
    }
}
