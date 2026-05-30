package com.jaf.application.config;

import com.jaf.application.service.AutenticacaoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfiguracaoTest {

    @Mock
    private AutenticacaoService autenticacaoService;

    @Mock
    private AutenticacaoEntryPoint autenticacaoEntryPoint;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SecurityConfiguracao securityConfiguracao;

    @Test
    void testPasswordEncoderBean() {
        PasswordEncoder encoder = securityConfiguracao.passwordEncoder();

        assertNotNull(encoder);
        assertTrue(encoder.matches("test", encoder.encode("test")));
    }

    @Test
    void testCorsConfigurationSource() {
        var corsConfigurationSource = securityConfiguracao.corsConfigurationSource();

        assertNotNull(corsConfigurationSource);
        var configuration = corsConfigurationSource.getCorsConfiguration(null);

        assertNotNull(configuration);
        assertTrue(configuration.getAllowedOrigins().contains("http://localhost:5173"));
        assertTrue(configuration.getAllowedOrigins().contains("http://localhost:3000"));
    }
}
