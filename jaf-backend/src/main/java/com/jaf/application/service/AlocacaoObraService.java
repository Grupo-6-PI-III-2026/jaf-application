//
package com.jaf.application.service;
import com.jaf.application.dto.AlocacaoObraDto;
import com.jaf.application.exceptions.Conflict;
import com.jaf.application.exceptions.NoContent;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.model.AlocacaoObra;
import com.jaf.application.model.Funcionario;
import com.jaf.application.model.Obra;
import com.jaf.application.repository.AlocacaoObraRepository;
import com.jaf.application.repository.FuncionarioRepository;
import com.jaf.application.repository.ObraRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
        Obra obra = obraRepository.findById(dto.getObraId())
                .orElseThrow(() -> new NotFoundException("Obra não encontrado."));

        AlocacaoObra alocacao = new AlocacaoObra();
        alocacao.setFuncionario(funcionario);
        alocacao.setObra(obra);
        alocacao.setCargo(dto.getCargoNaObra());

        return alocacaoObraRepository.save(alocacao);
    }

    public List<AlocacaoObra> listar() {
        if (alocacaoObraRepository == null){
            throw new NoContent("Lista de alocações vazia.");
        }
        return alocacaoObraRepository.findAll();
    }

    public AlocacaoObra buscarPorId(Long id) {
        return alocacaoObraRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Alocação não encontrada."));
    }

    public AlocacaoObra atualizar(Long id, AlocacaoObraDto dto) {
        AlocacaoObra existente = buscarPorId(id);

        Funcionario funcionario = funcionarioRepository.findById(dto.getFuncionarioId())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
        Obra obra = obraRepository.findById(dto.getObraId())
                .orElseThrow(() -> new NotFoundException("Obra não encontrada"));

        existente.setFuncionario(funcionario);
        existente.setObra(obra);
        existente.setCargo(dto.getCargoNaObra());

        return alocacaoObraRepository.save(existente);
    }

    public void deletar(Long id) {
        if (!alocacaoObraRepository.existsById(id)) {
            throw new NotFoundException("Alocação não encontrada.");
        }
        alocacaoObraRepository.deleteById(id);
    }
}
