package com.jaf.application.service;

import com.jaf.application.dto.FuncionarioDto;
import com.jaf.application.model.Funcionario;
import com.jaf.application.repository.FuncionarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class FuncionarioService {
    private final FuncionarioRepository funcionarioRepository;

    public FuncionarioService(FuncionarioRepository funcionarioRepository) {
        this.funcionarioRepository = funcionarioRepository;
    }

    public Funcionario criar(FuncionarioDto dto) {
        Funcionario funcionario = new Funcionario();
        funcionario.setNome(dto.getNome());
        funcionario.setEmail(dto.getEmail());
        funcionario.setSenha(dto.getSenha());
        funcionario.setCargoGlobal(dto.getCargo());
        return funcionarioRepository.save(funcionario);
    }

    public List<Funcionario> listar() {
        return funcionarioRepository.findAll();
    }

    public Funcionario buscarPorId(Long id) {
        return funcionarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionario nao encontrado"));
    }

    public Funcionario atualizar(Long id, FuncionarioDto dto) {
        Funcionario existente = buscarPorId(id);
        existente.setNome(dto.getNome());
        existente.setEmail(dto.getEmail());
        existente.setSenha(dto.getSenha());
        existente.setCargoGlobal(dto.getCargo());
        return funcionarioRepository.save(existente);
    }

    public void deletar(Long id) {
        if (!funcionarioRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionario nao encontrado");
        }
        funcionarioRepository.deleteById(id);
    }
}
