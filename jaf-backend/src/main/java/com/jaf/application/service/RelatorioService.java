package com.jaf.application.service;

import com.jaf.application.dto.RelatorioDto;
import com.jaf.application.exceptions.NoContent;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.model.Funcionario;
import com.jaf.application.model.Relatorio;
import com.jaf.application.repository.FuncionarioRepository;
import com.jaf.application.repository.RelatorioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class RelatorioService {

    private final RelatorioRepository relatorioRepository;
    private final FuncionarioRepository funcionarioRepository;

    public RelatorioService(RelatorioRepository relatorioRepository,
                            FuncionarioRepository funcionarioRepository) {
        this.relatorioRepository = relatorioRepository;
        this.funcionarioRepository = funcionarioRepository;
    }

    public Relatorio criar(RelatorioDto dto) {
        Funcionario funcionario = funcionarioRepository.findById(dto.getFuncionarioResponsavelId())
                .orElseThrow(() -> new NotFoundException("Funcionario nao encontrado"));

        Relatorio relatorio = new Relatorio();
        relatorio.setTitulo(dto.getTitulo());
        relatorio.setDtEmissao(dto.getDtEmissao());
        relatorio.setFuncionarioResponsavel(funcionario);

        return relatorioRepository.save(relatorio);
    }

    public List<Relatorio> listar() {
        if (relatorioRepository == null){
            throw new NoContent("Lista de Relatórios vazia.");
        }
        return relatorioRepository.findAll();
    }

    public Relatorio buscarPorId(Long id) {
        return relatorioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Relatorio nao encontrado"));
    }

    public Relatorio atualizar(Long id, RelatorioDto dto) {
        Relatorio existente = buscarPorId(id);
        Funcionario funcionario = funcionarioRepository.findById(dto.getFuncionarioResponsavelId())
                .orElseThrow(() -> new NotFoundException("Funcionario nao encontrado"));

        existente.setTitulo(dto.getTitulo());
        existente.setDtEmissao(dto.getDtEmissao());
        existente.setFuncionarioResponsavel(funcionario);

        return relatorioRepository.save(existente);
    }

    public void deletar(Long id) {
        if (!relatorioRepository.existsById(id)) {
            throw new NotFoundException("Relatorio nao encontrado");
        }
        relatorioRepository.deleteById(id);
    }
}
