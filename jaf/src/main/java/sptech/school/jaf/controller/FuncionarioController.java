package sptech.school.jaf.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sptech.school.jaf.dto.Funcionario;
import sptech.school.jaf.service.FuncionarioService;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/jaf/funcionarios")
public class FuncionarioController {
    private final FuncionarioService service;

    public FuncionarioController(FuncionarioService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Funcionario>> listar() {
        List<Funcionario> lista = service.listarTodos();

        if (lista.isEmpty()) {
            return ResponseEntity.status(204).build(); // 204 No Content
        }
        return ResponseEntity.status(200).body(lista); // 200 OK
    }

    // Buscando um funcionário específico pela URL (ex: /jaf/funcionarios/1)
    @GetMapping("/{id}")
    public ResponseEntity<Funcionario> buscar(@PathVariable int id) {
        Funcionario func = service.buscarPorId(id);

        if (func != null) {
            return ResponseEntity.status(200).body(func);
        }
        return ResponseEntity.status(404).build(); // 404 Not Found
    }
}
