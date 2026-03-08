package sptech.school.jaf.service;

import org.springframework.stereotype.Service;
import sptech.school.jaf.dto.Obra;

import java.util.ArrayList;
import java.util.List;

@Service
public class ObraService {

    private List<Obra> obras = new ArrayList<>();
    private Integer proximoId = 1;

    public Obra cadastrarObra(Obra novaObra) {
        novaObra.setId(proximoId++);
        obras.add(novaObra);
        return novaObra;
    }

    public List<Obra> listarObras() {
        return obras;
    }

    public Obra buscarPorId(Integer id) {
        for (Obra obra : obras) {
            if (obra.getId() != null && obra.getId() == id) {
                return obra;
            }
        }

        return null;
    }
}
