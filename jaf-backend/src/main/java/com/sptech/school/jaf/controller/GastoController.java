package com.sptech.school.jaf.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.sptech.school.jaf.dto.Gasto;
import com.sptech.school.jaf.service.GastoService;

import java.util.List;

@RestController
@RequestMapping("/jaf/gastos")
public class GastoController {

    private final GastoService gastoService;

    public GastoController(GastoService gastoService) {
        this.gastoService = gastoService;
    }

    @PostMapping("/funcionario/{idFuncionario}/obra/{idObra}")
    public ResponseEntity<Gasto> adicionarGasto(@PathVariable Integer idFuncionario, @PathVariable Integer idObra, @RequestBody Gasto novoGasto) {
        Gasto salvo = gastoService.registrarGasto(idFuncionario, idObra, novoGasto);

        if (salvo == null) {
            return ResponseEntity.status(404).build();
        }
        return  ResponseEntity.status(201).body(salvo);
    }

    // ENDPOINT 2 - Listar os gastos de UM funcionário específico
    @GetMapping("/funcionario/{idFuncionario}")
    public ResponseEntity<List<Gasto>> listarGastosFuncionario(@PathVariable int idFuncionario) {
        List<Gasto> lista = gastoService.listarGastosDoFuncionario(idFuncionario);

        if (lista.isEmpty()) {
            return ResponseEntity.status(204).build(); // 204 se o cara for pão-duro e não gastou nada
        }
        return ResponseEntity.status(200).body(lista);
    }

    // ENDPOINT 3 - Listar todos os gastos do sistema
    @GetMapping
    public ResponseEntity<List<Gasto>> listarTodosOsGastos() {
        List<Gasto> lista = gastoService.listarTodos();

        if (lista.isEmpty()) {
            return ResponseEntity.status(204).build();
        }
        return ResponseEntity.status(200).body(lista);
    }
}
