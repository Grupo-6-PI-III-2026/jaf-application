package sptech.school.jaf.service;

import org.springframework.stereotype.Service;
import sptech.school.jaf.dto.Funcionario;
import sptech.school.jaf.dto.Gasto;
import sptech.school.jaf.dto.Obra;

import java.util.ArrayList;
import java.util.List;

@Service
public class GastoService {

    private List<Gasto> gastos = new ArrayList<>();
    private Integer proximoId = 1;

    private final FuncionarioService funcionarioService;
    private final ObraService obraService;

    public GastoService(FuncionarioService funcionarioService, ObraService obraService) {
        this.funcionarioService = funcionarioService;
        this.obraService = obraService;
    }

    public Gasto registrarGasto(Integer idFuncionario, Integer idObra, Gasto novoGasto) {

        Funcionario func = funcionarioService.buscarPorId(idFuncionario);
        Obra obra = obraService.buscarPorId(idObra);

        if (func == null || obra == null) {
            return null;
        }

        novoGasto.setId(proximoId++);
        novoGasto.setFuncionario(func);
        novoGasto.setObra(obra);

        gastos.add(novoGasto);
        return novoGasto;
    }

    public List<Gasto> listarGastosDoFuncionario(Integer idFuncionario) {
        List<Gasto> gastosDoFuncionario = new ArrayList<>();

        for (Gasto gasto : gastos) {

            if (gasto.getFuncionario().getId().equals(idFuncionario)) {
                gastosDoFuncionario.add(gasto);
            }
        }
        return gastosDoFuncionario;
    }

    public List<Gasto> listarTodos() {
        return gastos;
    }
}
