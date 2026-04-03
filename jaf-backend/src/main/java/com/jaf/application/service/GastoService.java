package com.jaf.application.service;

import com.jaf.application.dto.GastoDto;
import com.jaf.application.model.Funcionario;
import com.jaf.application.model.Gasto;
import com.jaf.application.model.Obra;
import com.jaf.application.repository.FuncionarioRepository;
import com.jaf.application.repository.GastoRepository;
import com.jaf.application.repository.ObraRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class GastoService {
    private final GastoRepository gastoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ObraRepository obraRepository;

    public GastoService(GastoRepository gastoRepository,
                        FuncionarioRepository funcionarioRepository,
                        ObraRepository obraRepository) {
        this.gastoRepository = gastoRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.obraRepository = obraRepository;
    }

    public Gasto criar(GastoDto dto) {
        Funcionario funcionario = funcionarioRepository.findById(dto.getFuncionarioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionario nao encontrado"));
        Obra obra = obraRepository.findById(dto.getObraId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Obra nao encontrada"));

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

    public List<Gasto> listar() {
        return gastoRepository.findAll();
    }

    public Gasto buscarPorId(Long id) {
        return gastoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gasto nao encontrado"));
    }

    public Gasto atualizar(Long id, GastoDto dto) {
        Gasto existente = buscarPorId(id);

        Funcionario funcionario = funcionarioRepository.findById(dto.getFuncionarioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionario nao encontrado"));
        Obra obra = obraRepository.findById(dto.getObraId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Obra nao encontrada"));

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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Gasto nao encontrado");
        }
        gastoRepository.deleteById(id);
    }
}
