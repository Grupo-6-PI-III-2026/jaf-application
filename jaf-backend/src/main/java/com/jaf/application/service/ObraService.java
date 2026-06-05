package com.jaf.application.service;

import com.jaf.application.dto.ObraDto;
import com.jaf.application.enums.Cargo;
import com.jaf.application.exceptions.Conflict;
import com.jaf.application.exceptions.Forbidden;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.model.AlocacaoObra;
import com.jaf.application.model.Funcionario;
import com.jaf.application.model.Obra;
import com.jaf.application.repository.AlocacaoObraRepository;
import com.jaf.application.repository.FuncionarioRepository;
import com.jaf.application.repository.ObraRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ObraService {
    private final ObraRepository obraRepository;
    private final AlocacaoObraRepository alocacaoObraRepository;
    private final FuncionarioRepository funcionarioRepository;

    public ObraService(ObraRepository obraRepository,
                       AlocacaoObraRepository alocacaoObraRepository,
                       FuncionarioRepository funcionarioRepository) {
        this.obraRepository = obraRepository;
        this.alocacaoObraRepository = alocacaoObraRepository;
        this.funcionarioRepository = funcionarioRepository;
    }

    public Obra criar(ObraDto dto) {
        if (obraRepository.existsByTitulo(dto.getTitulo())) {
            throw new Conflict("Obra ja existente.");
        }
        Obra obra = new Obra();
        obra.setTitulo(dto.getTitulo());
        obra.setOrcamento(dto.getOrcamento());
        obra.setStatus(dto.getStatus());
        obra.setDtInicio(dto.getDtInicio());
        obra.setDtTerminoPrevisto(dto.getDtTerminoPrevisto());
        return obraRepository.save(obra);
    }

    public List<Obra> listarPorUsuario(String email) {
        Funcionario funcionario = funcionarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado."));

        if (funcionario.getCargoGlobal() == Cargo.ADMIN) {
            return obraRepository.findAll();
        }

        List<AlocacaoObra> alocacoes = alocacaoObraRepository.findByFuncionarioId(funcionario.getId());
        if (alocacoes.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> obraIds = new HashSet<>();
        alocacoes.forEach(alocacao -> obraIds.add(alocacao.getObra().getId()));
        return obraRepository.findByIdIn(obraIds);
    }

    public Obra buscarPorIdComEscopo(Long id, String email) {
        Funcionario funcionario = funcionarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado."));
        Obra obra = buscarPorId(id);

        if (funcionario.getCargoGlobal() == Cargo.ADMIN) {
            return obra;
        }

        boolean alocado = alocacaoObraRepository.existsByFuncionarioIdAndObraId(funcionario.getId(), id);
        if (!alocado) {
            throw new Forbidden("Funcionario nao esta alocado nesta obra.");
        }
        return obra;
    }

    public Obra buscarPorId(Long id) {
        return obraRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Obra nao encontrada."));
    }

    public Obra atualizar(Long id, ObraDto dto) {
        Obra existente = obraRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Obra nao encontrada."));

        existente.setOrcamento(dto.getOrcamento());
        existente.setStatus(dto.getStatus());
        existente.setDtInicio(dto.getDtInicio());
        existente.setDtTerminoPrevisto(dto.getDtTerminoPrevisto());
        return obraRepository.save(existente);
    }

    public void deletar(Long id) {
        if (!obraRepository.existsById(id)) {
            throw new NotFoundException("Obra nao encontrada");
        }
        obraRepository.deleteById(id);
    }
}
