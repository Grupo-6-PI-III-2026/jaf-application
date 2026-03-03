package sptech.school.jaf.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sptech.school.jaf.dto.Funcionario;
import sptech.school.jaf.service.FuncionarioService;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/jaf/funcionarios")
public class FuncionarioController {
    FuncionarioService service;
    private List<Funcionario> funcionarios = new ArrayList<>(
            List.of(
                    new Funcionario("Rafael"),
                    new Funcionario("João"),
                    new Funcionario("Gabriel"),
                    new Funcionario("Duílio")
            )
    );
}
