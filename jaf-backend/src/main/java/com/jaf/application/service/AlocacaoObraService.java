package com.jaf.application.service;

import com.jaf.application.dto.AlocacaoObraDto;
import com.jaf.application.enums.Cargo;
import com.jaf.application.exceptions.Conflict;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.model.AlocacaoObra;
import com.jaf.application.model.Funcionario;
import com.jaf.application.model.Obra;
import com.jaf.application.repository.AlocacaoObraRepository;
import com.jaf.application.repository.FuncionarioRepository;
import com.jaf.application.repository.ObraRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlocacaoObraService {
    private final AlocacaoObraRepository alocacaoObraRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ObraRepository obraRepository;

    public AlocacaoObraService(AlocacaoObraRepository alocacaoObraRepository,
                               FuncionarioRepository funcionarioRepository,
                               ObraRepository obraRepository) {
        this.alocacaoObraRepository = alocacaoObraRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.obraRepository = obraRepository;
    }

    public AlocacaoObra criar(AlocacaoObraDto dto) {
        Funcionario funcionario = funcionarioRepository.findById(dto.getFuncionarioId())
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado."));
        Obra obra = obraRepository.findById(dto.getObraId())
                .orElseThrow(() -> new NotFoundException("Obra nao encontrada."));

        if (alocacaoObraRepository.existsByFuncionarioIdAndObraId(dto.getFuncionarioId(), dto.getObraId())) {
            throw new Conflict("Funcionario ja esta alocado nesta obra.");
        }

        AlocacaoObra alocacao = new AlocacaoObra();
        alocacao.setFuncionario(funcionario);
        alocacao.setObra(obra);
        alocacao.setCargo(dto.getCargoNaObra());
        return alocacaoObraRepository.save(alocacao);
    }

    public List<AlocacaoObra> listarPorUsuario(String email) {
        Funcionario funcionario = funcionarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado."));
        if (funcionario.getCargoGlobal() == Cargo.ADMIN) {
            return alocacaoObraRepository.findAll();
        }
        return alocacaoObraRepository.findByFuncionarioId(funcionario.getId());
    }

    public List<AlocacaoObra> listarPorObra(Long obraId) {
        return alocacaoObraRepository.findByObraId(obraId);
    }

    public List<AlocacaoObra> listarPorFuncionario(Long funcionarioId) {
        return alocacaoObraRepository.findByFuncionarioId(funcionarioId);
    }

    public AlocacaoObra buscarPorId(Long id) {
        return alocacaoObraRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Alocacao nao encontrada."));
    }

    public AlocacaoObra atualizar(Long id, AlocacaoObraDto dto) {
        AlocacaoObra existente = buscarPorId(id);

        Funcionario funcionario = funcionarioRepository.findById(dto.getFuncionarioId())
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado"));
        Obra obra = obraRepository.findById(dto.getObraId())
                .orElseThrow(() -> new NotFoundException("Obra nao encontrada"));

        existente.setFuncionario(funcionario);
        existente.setObra(obra);
        existente.setCargo(dto.getCargoNaObra());
        return alocacaoObraRepository.save(existente);
    }

    public void deletar(Long id) {
        if (!alocacaoObraRepository.existsById(id)) {
            throw new NotFoundException("Alocacao nao encontrada.");
        }
        alocacaoObraRepository.deleteById(id);
    }
}
