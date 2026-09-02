package com.jaf.application.dto;

import com.jaf.application.model.Funcionario;

public class FuncionarioMapper {

    public static Funcionario of(FuncionarioDto funcionarioDto){
        Funcionario funcionario = new Funcionario();

        funcionario.setNome(funcionarioDto.getNome());
        funcionario.setEmail(funcionarioDto.getEmail());;
        funcionario.setSenha(funcionarioDto.getSenha());;
        return funcionario;
    }

    public static Funcionario of(FuncionarioLoginDto funcionarioLoginDto) {
        Funcionario funcionario = new Funcionario();

        funcionario.setEmail(funcionarioLoginDto.getEmail());
        funcionario.setSenha(funcionarioLoginDto.getSenha());

        return funcionario;
    }

    public static FuncionarioTokenDto of(Funcionario funcionario, String token) {
        FuncionarioTokenDto funcionarioTokenDto = new FuncionarioTokenDto();

        funcionarioTokenDto.setId(funcionario.getId());
        funcionarioTokenDto.setEmail(funcionario.getEmail());
        funcionarioTokenDto.setNome(funcionario.getNome());
        funcionarioTokenDto.setToken(token);

        return funcionarioTokenDto;
    }

    public static FuncionarioListarDto of(Funcionario funcionario) {
        FuncionarioListarDto funcionarioListarDto = new FuncionarioListarDto();

        funcionarioListarDto.setId(funcionario.getId());
        funcionarioListarDto.setEmail(funcionario.getEmail());
        funcionarioListarDto.setNome(funcionario.getNome());

        return funcionarioListarDto;
    }
}
