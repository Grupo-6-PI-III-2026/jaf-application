package com.jaf.application.service;

import com.jaf.application.model.Funcionario;
import com.jaf.application.repository.FuncionarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
	private final FuncionarioRepository funcionarioRepository;

	public AuthService(FuncionarioRepository funcionarioRepository) {
		this.funcionarioRepository = funcionarioRepository;
	}

	public Long autenticar(String email, String senha) {
		if (email == null || email.isBlank() || senha == null || senha.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email e password sao obrigatorios");
		}

		Funcionario funcionario = funcionarioRepository.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais invalidas"));

		if (!senha.equals(funcionario.getSenha())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais invalidas");
		}

		return funcionario.getId();
	}
}
