package com.jaf.application.service;

import com.jaf.application.model.Funcionario;
import com.jaf.application.repository.FuncionarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Serviço de autenticação com suporte a BCrypt
 * CORREÇÃO DE SEGURANÇA A02: Implementação de criptografia BCrypt para comparação de senhas
 */
@Service
public class AuthService {
	private final FuncionarioRepository funcionarioRepository;
	private final PasswordEncoder passwordEncoder;

	public AuthService(FuncionarioRepository funcionarioRepository, PasswordEncoder passwordEncoder) {
		this.funcionarioRepository = funcionarioRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public Long autenticar(String email, String senha) {
		if (email == null || email.isBlank() || senha == null || senha.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email e password sao obrigatorios");
		}

		Funcionario funcionario = funcionarioRepository.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais invalidas"));

		// CORREÇÃO DE SEGURANÇA A02: Comparação usando BCrypt em vez de texto plano
		// Antes: if (!senha.equals(funcionario.getSenha()))
		// Agora: if (!passwordEncoder.matches(senha, funcionario.getSenha()))
		// Isso garante que senhas armazenadas como hash sejam comparadas corretamente
		if (!passwordEncoder.matches(senha, funcionario.getSenha())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais invalidas");
		}

		return funcionario.getId();
	}
}
