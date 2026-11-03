package com.jaf.application.config;

import com.jaf.application.enums.Cargo;
import com.jaf.application.model.Funcionario;
import com.jaf.application.repository.FuncionarioRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapAdminRunner implements ApplicationRunner {
    private final Environment environment;
    private final FuncionarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public BootstrapAdminRunner(Environment environment, FuncionarioRepository repository,
                                PasswordEncoder passwordEncoder) {
        this.environment = environment;
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = environment.getProperty("APP_BOOTSTRAP_ADMIN_EMAIL");
        String password = environment.getProperty("APP_BOOTSTRAP_ADMIN_PASSWORD");
        if (email == null || email.isBlank() || password == null || password.isBlank()
                || repository.findAll().stream().anyMatch(f -> f.getCargoGlobal() == Cargo.ADMIN)) return;
        if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$")) {
            throw new IllegalStateException("APP_BOOTSTRAP_ADMIN_PASSWORD não atende à política de senha");
        }
        Funcionario admin = new Funcionario();
        admin.setNome("Administrador");
        admin.setEmail(email.trim().toLowerCase(java.util.Locale.ROOT));
        admin.setSenha(passwordEncoder.encode(password));
        admin.setCargoGlobal(Cargo.ADMIN);
        repository.save(admin);
    }
}
