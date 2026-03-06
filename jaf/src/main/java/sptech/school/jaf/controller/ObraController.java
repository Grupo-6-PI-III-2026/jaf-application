package sptech.school.jaf.controller;
import sptech.school.jaf.dto.Obra;

import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/obras")
public class ObraController {

    private List<Obra> listaObras = new ArrayList<>();


    // ENDPOINT 1 - CADASTRAR OBRA
    @PostMapping
    public String cadastrarObra(@RequestBody Obra novaObra) {
        listaObras.add(novaObra);
        return "Obra cadastrada com sucesso";
    }


    // ENDPOINT 2 - LISTAR OBRAS
    @GetMapping
    public List<Obra> listarObras() {
        return listaObras;
    }

}