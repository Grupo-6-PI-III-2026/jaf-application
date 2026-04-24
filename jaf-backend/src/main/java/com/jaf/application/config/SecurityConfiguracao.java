package com.jaf.application.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.jaf.application.service.AutenticacaoService;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguracao {

    @Autowired
    private AutenticacaoService autenticacaoService;

    // AutenticacaoEntryPoint é registrado como @Component, o Spring injeta automaticamente
    @Autowired
    private AutenticacaoEntryPoint autenticacaoJwtEntryPoint;

    private static final String[] URLS_PERMITIDAS = {
            "/",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/swagger-resources",
            "/swagger-resources/**",
            "/configuration/ui",
            "/configuration/security",
            "/api/public/**",
            "/api/public/authenticate",
            "/webjars/**",
            "/v3/api-docs/**",
            "/actuator/*",
            "/funcionarios/login",
            "/h2-console/**",
            "/h2-console/*/**",
            "/error/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AutenticacaoFilter autenticacaoFilter) throws Exception {
        http.headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))

                // Habilita CORS com a configuração definida em corsConfigurationSource()
                .cors(Customizer.withDefaults())


                .csrf(CsrfConfigurer<HttpSecurity>::disable)

                // Define quais URLs são públicas e quais exigem autenticação
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(URLS_PERMITIDAS).permitAll()  // rotas públicas
                        .requestMatchers(HttpMethod.POST, "/funcionarios/login").permitAll()  // login público
                        .anyRequest().authenticated()                  // todas as outras exigem token
                )

                // Configura o handler para erros de autenticação (token ausente/inválido → 401/403)
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(autenticacaoJwtEntryPoint))

                .sessionManagement(management -> management
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(autenticacaoFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.authenticationProvider(
                new AutenticacaoProvider(autenticacaoService, passwordEncoder()));
        return authenticationManagerBuilder.build();
    }

    @Bean
    public AutenticacaoFilter jwtAuthenticationFilterBean(GerenciadorTokenJwt gerenciadorTokenJwt) {
        return new AutenticacaoFilter(autenticacaoService, gerenciadorTokenJwt);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracao = new CorsConfiguration();

        // Origens permitidas — deve ser explícita quando allowCredentials=true
        // Em produção: List.of("https://meuapp.com.br")
        configuracao.setAllowedOrigins(List.of(
                "http://localhost:5173",  // Vite dev server
                "http://localhost:3000"   // Create React App (alternativa)
        ));

        // Necessário para que o browser envie/receba cookies nas requisições cross-origin
        configuracao.setAllowCredentials(true);

        configuracao.setAllowedMethods(Arrays.asList(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name(),
                HttpMethod.HEAD.name(),
                HttpMethod.TRACE.name()
        ));

        // Permite todos os headers de requisição (Content-Type, Authorization etc.)
        configuracao.setAllowedHeaders(List.of("*"));

        // Expõe o header Content-Disposition para download de arquivos
        configuracao.setExposedHeaders(List.of(HttpHeaders.CONTENT_DISPOSITION));

        UrlBasedCorsConfigurationSource origem = new UrlBasedCorsConfigurationSource();
        origem.registerCorsConfiguration("/**", configuracao);

        return origem;
    }
}
