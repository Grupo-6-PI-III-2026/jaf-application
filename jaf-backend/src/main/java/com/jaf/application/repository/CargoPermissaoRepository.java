package com.jaf.application.repository;

import com.jaf.application.enums.Cargo;
import com.jaf.application.model.CargoPermissao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CargoPermissaoRepository extends JpaRepository<CargoPermissao, Long> {
    List<CargoPermissao> findByCargo(Cargo cargo);

    void deleteByCargo(Cargo cargo);

    boolean existsByCargo(Cargo cargo);
}