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
            if (obra.getId() != null && obra.getId().equals(id)) {
                return obra;
            }
        }

        return null;
    }

    public Obra atualizarObra(Integer id, Obra obraAtualizada) {
        for (Integer i = 0; i < obras.size(); i++) {
            Obra obraAtual = obras.get(i);

            if (obraAtual.getId() != null && obraAtual.getId().equals(id)) {
                obraAtualizada.setId(id);
                obras.set(i, obraAtualizada);
                return obraAtualizada;
            }
        }
        return null;
    }

    public boolean deletarObra(Integer id) {
        return obras.removeIf(obra -> obra.getId() != null && obra.getId().equals(id));
    }
}
