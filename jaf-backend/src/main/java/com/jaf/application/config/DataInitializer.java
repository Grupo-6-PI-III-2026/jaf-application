package com.jaf.application.config;

import com.jaf.application.enums.Cargo;
import com.jaf.application.model.Funcionario;
import com.jaf.application.repository.FuncionarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(FuncionarioRepository funcionarioRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Verifica se já existe algum usuário no banco
            if (funcionarioRepository.count() == 0) {
                // Cria um usuário administrador padrão
                Funcionario admin = new Funcionario();
                admin.setNome("Administrador");
                admin.setEmail("admin@jaf.com");
                admin.setSenha(passwordEncoder.encode("Admin@123"));
                admin.setCargoGlobal(Cargo.GESTOR_OBRA);
                
                funcionarioRepository.save(admin);
                
                System.out.println("========================================");
                System.out.println("Usuário administrador criado com sucesso!");
                System.out.println("Email: admin@jaf.com");
                System.out.println("Senha: Admin@123");
                System.out.println("========================================");
            }
        };
    }
}
