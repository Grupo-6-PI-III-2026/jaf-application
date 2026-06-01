package com.jaf.application.repository;

import com.jaf.application.enums.CargoNaObra;
import com.jaf.application.enums.TipoFuncionario;
import com.jaf.application.model.AlocacaoObra;
import com.jaf.application.model.Funcionario;
import com.jaf.application.model.Obra;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class AlocacaoObraRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AlocacaoObraRepository alocacaoObraRepository;

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private ObraRepository obraRepository;

    private Funcionario funcionario;
    private Obra obra;
    private AlocacaoObra alocacao;

    @BeforeEach
    void setUp() {
        funcionario = new Funcionario();
        funcionario.setNome("João Silva");
        funcionario.setEmail("joao@test.com");
        funcionario.setTipoFuncionario(TipoFuncionario.INTERNO);
        funcionario = entityManager.persist(funcionario);

        obra = new Obra();
        obra.setTitulo("Obra Teste");
        obra.setStatus("EM_ANDAMENTO");
        obra = entityManager.persist(obra);

        alocacao = new AlocacaoObra();
        alocacao.setFuncionario(funcionario);
        alocacao.setObra(obra);
        alocacao.setCargo(CargoNaObra.PEDREIRO);
        alocacao = entityManager.persist(alocacao);
    }

    @Test
    void testFindByFuncionarioId() {
        List<AlocacaoObra> alocacoes = alocacaoObraRepository.findByFuncionarioId(funcionario.getId());

        assertNotNull(alocacoes);
        assertEquals(1, alocacoes.size());
        assertEquals(funcionario.getId(), alocacoes.get(0).getFuncionario().getId());
    }

    @Test
    void testFindByObraId() {
        List<AlocacaoObra> alocacoes = alocacaoObraRepository.findByObraId(obra.getId());

        assertNotNull(alocacoes);
        assertEquals(1, alocacoes.size());
        assertEquals(obra.getId(), alocacoes.get(0).getObra().getId());
    }

    @Test
    void testExistsByFuncionarioIdAndObraId() {
        boolean existe = alocacaoObraRepository.existsByFuncionarioIdAndObraId(funcionario.getId(), obra.getId());

        assertTrue(existe);
    }

    @Test
    void testExistsByFuncionarioIdAndObraId_NaoExiste() {
        boolean existe = alocacaoObraRepository.existsByFuncionarioIdAndObraId(999L, 999L);

        assertFalse(existe);
    }

    @Test
    void testFindByFuncionarioIdAndObraId() {
        List<AlocacaoObra> alocacoes = alocacaoObraRepository.findByFuncionarioIdAndObraId(
                funcionario.getId(), obra.getId());

        assertNotNull(alocacoes);
        assertEquals(1, alocacoes.size());
    }

    @Test
    void testFindByFuncionarioIdAndObraId_Vazio() {
        List<AlocacaoObra> alocacoes = alocacaoObraRepository.findByFuncionarioIdAndObraId(
                funcionario.getId(), 999L);

        assertNotNull(alocacoes);
        assertTrue(alocacoes.isEmpty());
    }

    @Test
    void testDeleteById() {
        Long id = alocacao.getId();
        alocacaoObraRepository.deleteById(id);

        assertFalse(alocacaoObraRepository.existsById(id));
    }

    @Test
    void testFindAll() {
        List<AlocacaoObra> todasAlocacoes = alocacaoObraRepository.findAll();

        assertNotNull(todasAlocacoes);
        assertFalse(todasAlocacoes.isEmpty());
    }
}
