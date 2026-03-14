package com.sptech.school.jaf.controller;
import org.springframework.http.ResponseEntity;
import com.sptech.school.jaf.dto.Obra;

import org.springframework.web.bind.annotation.*;
import com.sptech.school.jaf.service.ObraService;

import java.util.List;

@RestController
@RequestMapping("/jaf/obras")
public class ObraController {

    private final ObraService obraService;

    public ObraController(ObraService obraService) {
        this.obraService = obraService;
    }

    // ENDPOINT 1 - CADASTRAR OBRA
    @PostMapping
    public ResponseEntity<Obra> cadastrarObra(@RequestBody Obra novaObra) {
        Obra obraCadastrada = obraService.cadastrarObra(novaObra);

        return ResponseEntity.status(201).body(obraCadastrada);
    }

    // ENDPOINT 2 - LISTAR OBRAS
    @GetMapping
    public ResponseEntity<List<Obra>> listarObras() {
        List<Obra> lista = obraService.listarObras();

        if (lista.isEmpty()) {
            return ResponseEntity.status(204).build();
        }
        return  ResponseEntity.status(200).body(lista);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Obra> buscarObra(@PathVariable Integer id) {
        Obra obra = obraService.buscarPorId(id);

        if (obra != null) {
            return ResponseEntity.status(200).body(obra);
        }
        return ResponseEntity.status(404).build();
    }

    // ENDPOINT 4 - ATUALIZAR OBRA
    @PutMapping("/{id}")
    public ResponseEntity<Obra> atualizarObra(@PathVariable Integer id, @RequestBody Obra obraAtualizada) {
        Obra atualizada = obraService.atualizarObra(id, obraAtualizada);

        if (atualizada != null) {
            return ResponseEntity.status(200).body(atualizada);
        }
        return ResponseEntity.status(404).build();
    }

    // ENDPOINT 5 - DELETAR OBRA
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarObra(@PathVariable Integer id) {
        boolean foiDeletado = obraService.deletarObra(id);

        if (foiDeletado) {

            return ResponseEntity.status(204).build();
        }
        return ResponseEntity.status(404).build();
    }

}