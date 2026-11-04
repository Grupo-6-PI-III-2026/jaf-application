package com.jaf.application.repository;

import com.jaf.application.model.Gasto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface GastoRepository extends JpaRepository<Gasto, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Gasto> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select g from Gasto g where g.id = :id")
    java.util.Optional<Gasto> buscarParaAtualizar(Long id);
    // Evita uma consulta por funcionário/obra ao montar as listas (N+1).
    @EntityGraph(attributePaths = {"funcionario", "obra"})
    List<Gasto> findByObraId(Long obraId);
    boolean existsByFuncionarioId(Long funcionarioId);
    @EntityGraph(attributePaths = {"funcionario", "obra"})
    List<Gasto> findByObraIdIn(Collection<Long> obraIds);

    @Override
    @EntityGraph(attributePaths = {"funcionario", "obra"})
    List<Gasto> findAll(Specification<Gasto> spec);

    @Override
    @EntityGraph(attributePaths = {"funcionario", "obra"})
    Page<Gasto> findAll(Specification<Gasto> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"funcionario", "obra"})
    List<Gasto> findAll();
    List<Gasto> findByObraIdAndEtapa(Long obraId, String etapa);
    List<Gasto> findByObraIdAndMetodoPagamento(Long obraId, String metodoPagamento);
    List<Gasto> findByObraIdAndReembolsavelTrue(Long obraId);
    List<Gasto> findByObraIdAndReembolsoConcluidoIsNotNull(Long obraId);

    @EntityGraph(attributePaths = {"funcionario", "obra"})
    Page<Gasto> findByObraId(Long obraId, Pageable pageable);
    @EntityGraph(attributePaths = {"funcionario", "obra"})
    Page<Gasto> findByObraIdIn(Collection<Long> obraIds, Pageable pageable);
    Page<Gasto> findByObraIdAndCategoria(Long obraId, String categoria, Pageable pageable);
    Page<Gasto> findByObraIdAndMetodoPagamento(Long obraId, String metodoPagamento, Pageable pageable);

    interface TotalPorCategoria {
        String getCategoria();
        BigDecimal getTotal();
    }

    interface TotalPorData {
        LocalDate getDtGasto();
        BigDecimal getTotal();
    }

    String FILTRO_PERIODO_ETAPA = """
            g.obra.id = :obraId and g.dtGasto >= :de and g.dtGasto <= :ate
            and (:todas = true or g.etapa = :etapa)
            """;

    @org.springframework.data.jpa.repository.Query("select coalesce(sum(g.valor), 0) from Gasto g where g.obra.id = :obraId")
    BigDecimal somarPorObra(@Param("obraId") Long obraId);

    @org.springframework.data.jpa.repository.Query("select coalesce(sum(g.valor), 0) from Gasto g where " + FILTRO_PERIODO_ETAPA)
    BigDecimal somarPeriodoEtapa(@Param("obraId") Long obraId, @Param("de") LocalDate de, @Param("ate") LocalDate ate,
                                 @Param("todas") boolean todas, @Param("etapa") String etapa);

    @org.springframework.data.jpa.repository.Query("""
            select coalesce(sum(g.valor), 0) from Gasto g where
            """ + FILTRO_PERIODO_ETAPA + """
            and (g.reembolsavel = true or upper(trim(g.metodoPagamento)) = 'REEMBOLSO')
            """)
    BigDecimal somarReembolsaveis(@Param("obraId") Long obraId, @Param("de") LocalDate de, @Param("ate") LocalDate ate,
                                  @Param("todas") boolean todas, @Param("etapa") String etapa);

    @org.springframework.data.jpa.repository.Query("""
            select g.categoria as categoria, sum(g.valor) as total from Gasto g where
            """ + FILTRO_PERIODO_ETAPA + """
            group by g.categoria order by min(g.id)
            """)
    List<TotalPorCategoria> totaisPorCategoria(@Param("obraId") Long obraId, @Param("de") LocalDate de, @Param("ate") LocalDate ate,
                                               @Param("todas") boolean todas, @Param("etapa") String etapa);

    @org.springframework.data.jpa.repository.Query("""
            select g.dtGasto as dtGasto, sum(g.valor) as total from Gasto g where
            """ + FILTRO_PERIODO_ETAPA + """
            and g.categoria in :categorias
            group by g.dtGasto
            """)
    List<TotalPorData> totaisPorData(@Param("obraId") Long obraId, @Param("de") LocalDate de, @Param("ate") LocalDate ate,
                                     @Param("todas") boolean todas, @Param("etapa") String etapa,
                                     @Param("categorias") Collection<String> categorias);
    interface TotalPorObra {
        Long getObraId();
        BigDecimal getTotal();
    }

    interface PessoaDoGasto {
        Long getId();
        String getNome();
    }

    @org.springframework.data.jpa.repository.Query("""
            select g.obra.id as obraId, sum(g.valor) as total from Gasto g
            where g.obra.id in :obraIds group by g.obra.id
            """)
    List<TotalPorObra> totaisPorObra(@Param("obraIds") Collection<Long> obraIds);

    @org.springframework.data.jpa.repository.Query("""
            select distinct g.funcionario.id as id, g.funcionario.nome as nome from Gasto g
            where g.obra.id = :obraId
            """)
    List<PessoaDoGasto> pessoasComGastoNaObra(@Param("obraId") Long obraId);
}