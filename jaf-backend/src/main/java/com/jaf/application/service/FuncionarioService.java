package com.jaf.application.service;

import com.jaf.application.dto.FuncionarioDto;
import com.jaf.application.dto.FuncionarioResponseDto;
import com.jaf.application.model.Funcionario;
import com.jaf.application.repository.FuncionarioRepository;
import com.jaf.application.exceptions.Conflict;
import com.jaf.application.exceptions.NoContent;
import com.jaf.application.exceptions.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FuncionarioService {
    private final FuncionarioRepository funcionarioRepository;

    public FuncionarioService(FuncionarioRepository funcionarioRepository) {
        this.funcionarioRepository = funcionarioRepository;
    }

    public FuncionarioResponseDto criar(FuncionarioDto dto) {
        if (funcionarioRepository.existsByNome(dto.getNome())){
            throw new Conflict("Usuário já existe.");
        }
        Funcionario funcionario = new Funcionario();
        funcionario.setNome(dto.getNome());
        funcionario.setEmail(dto.getEmail());
        funcionario.setSenha(dto.getSenha());
        funcionario.setCargoGlobal(dto.getCargo());
        return new FuncionarioResponseDto(funcionarioRepository.save(funcionario));
    }

    public List<FuncionarioResponseDto> listar() {
                if (funcionarioRepository == null){
                    throw new NoContent("Lista de funcionários vazia.");
                }
        return funcionarioRepository.findAll()
                .stream()
                .map(FuncionarioResponseDto::new)
                .toList();
    }

    public FuncionarioResponseDto buscarPorId(Long id) {
        Funcionario funcionario = funcionarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
        return new FuncionarioResponseDto(funcionario);
    }

    public FuncionarioResponseDto atualizar(Long id, FuncionarioDto dto) {
        Funcionario existente = funcionarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));

        existente.setNome(dto.getNome());
        existente.setEmail(dto.getEmail());
        existente.setCargoGlobal(dto.getCargo());

        return new FuncionarioResponseDto(funcionarioRepository.save(existente));
    }

    public void deletar(Long id) {
        if (!funcionarioRepository.existsById(id)) {
            throw new NotFoundException("Usuário não encontrado.");
        }
        funcionarioRepository.deleteById(id);
    }
}
