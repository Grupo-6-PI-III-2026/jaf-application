package com.jaf.application.service;

import com.jaf.application.dto.ObraDto;
import com.jaf.application.exceptions.Conflict;
import com.jaf.application.exceptions.NoContent;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.model.Funcionario;
import com.jaf.application.model.Obra;
import com.jaf.application.repository.ObraRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ObraService {
    private final ObraRepository obraRepository;

    public ObraService(ObraRepository obraRepository) {
        this.obraRepository = obraRepository;
    }

    public Obra criar(ObraDto dto) {
        if (obraRepository.existsByTitulo(dto.getTitulo())){
            throw new Conflict("Obra já existente.");
        }
        Obra obra = new Obra();
        obra.setTitulo(dto.getTitulo());
        obra.setOrcamento(dto.getOrcamento());
        obra.setStatus(dto.getStatus());
        obra.setDtInicio(dto.getDtInicio());
        obra.setDtTerminoPrevisto(dto.getDtTerminoPrevisto());
        return obraRepository.save(obra);
    }

    public List<Obra> listar() {
        if (obraRepository == null){
            throw new NoContent("Lista de Obras vazia");
        }
        return obraRepository.findAll();
    }

    public Obra buscarPorId(Long id) {
        return obraRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Obra não encontrada."));
    }

    public Obra atualizar(Long id, ObraDto dto) {

        Obra existente = obraRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Obra não encontrada."));

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
