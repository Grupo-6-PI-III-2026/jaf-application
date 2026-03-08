package sptech.school.jaf.controller;
import org.springframework.http.ResponseEntity;
import sptech.school.jaf.dto.Obra;

import org.springframework.web.bind.annotation.*;
import sptech.school.jaf.service.ObraService;

import java.util.ArrayList;
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
    public ResponseEntity<Obra> buscarObra(@PathVariable int id) {
        Obra obra = obraService.buscarPorId(id);

        if (obra != null) {
            return ResponseEntity.status(200).body(obra);
        }
        return ResponseEntity.status(404).build();
    }

}