package sptech.school.jaf.service;

import org.springframework.stereotype.Service;
import sptech.school.jaf.dto.Funcionario;

import java.util.ArrayList;
import java.util.List;

@Service
public class FuncionarioService {

    private List<Funcionario> funcionarios = new ArrayList<>(
            List.of(
                    new Funcionario(0, "Rafael"),
                    new Funcionario(1, "João"),
                    new Funcionario(2, "Duílio"),
                    new Funcionario(3, "Gabriel")
            )
    );

    public List<Funcionario> listarTodos() {
        return funcionarios;
    }

    public Funcionario buscarPorId(int id) {
        if (id >= 0 && id < funcionarios.size()) {
            return funcionarios.get(id);
        }
        return null;
    }
}
