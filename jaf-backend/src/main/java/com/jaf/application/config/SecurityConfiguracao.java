package com.jaf.application.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.jaf.application.service.AutenticacaoService;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguracao {

    private final AutenticacaoService autenticacaoService;
    private final AutenticacaoEntryPoint autenticacaoJwtEntryPoint;
    private final AccessDeniedJsonHandler accessDeniedJsonHandler;
    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    public SecurityConfiguracao(AutenticacaoService autenticacaoService,
                                AutenticacaoEntryPoint autenticacaoJwtEntryPoint,
                                AccessDeniedJsonHandler accessDeniedJsonHandler) {
        this.autenticacaoService = autenticacaoService;
        this.autenticacaoJwtEntryPoint = autenticacaoJwtEntryPoint;
        this.accessDeniedJsonHandler = accessDeniedJsonHandler;
    }

    private static final String[] URLS_PERMITIDAS = {
            "/api/public/**",
            "/api/public/authenticate",
            "/actuator/health",
            "/actuator/health/**",
            "/funcionarios/login",
            "/funcionarios/logout", // CORREÇÃO DE SEGURANÇA A07: Endpoint de logout público
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/webjars/**",
            "/error/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AutenticacaoFilter autenticacaoFilter,
                                           com.jaf.application.service.IdempotencyService idempotencyService) throws Exception {
        http.headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).preload(true).maxAgeInSeconds(31536000))
                        .referrerPolicy(referrer -> referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))

                // Habilita CORS com a configuração definida em corsConfigurationSource()
                .cors(Customizer.withDefaults())


                // Define quais URLs são públicas e quais exigem autenticação
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(URLS_PERMITIDAS).permitAll()  // rotas públicas
                        .requestMatchers(HttpMethod.POST, "/funcionarios/login").permitAll()  // login público
                        .requestMatchers("/actuator/prometheus").hasRole("ADMIN")  // métricas só para ADMIN
                        // CORREÇÃO DE SEGURANÇA A01: Removido acesso público ao cadastro de funcionários
                        // Antes: .requestMatchers(HttpMethod.POST, "/funcionarios").permitAll()
                        // Agora: exige autenticação e permissão específica (ver @PreAuthorize no controller)
                        .anyRequest().authenticated()                  // todas as outras exigem token
                )

                // Configura o handler para erros de autenticação (token ausente/inválido → 401/403)
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(autenticacaoJwtEntryPoint)
                        .accessDeniedHandler(accessDeniedJsonHandler))

                .sessionManagement(management -> management
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                .ignoringRequestMatchers(new AntPathRequestMatcher(
                        "/funcionarios/login", HttpMethod.POST.name())));

        http.addFilterBefore(autenticacaoFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(new CsrfCookieFilter(), UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(new IdempotencyFilter(idempotencyService), CsrfCookieFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(autenticacaoService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
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

        List<String> origens = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        if (origens.isEmpty()) return new UrlBasedCorsConfigurationSource();
        configuracao.setAllowedOrigins(origens);

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
                HttpMethod.HEAD.name()
        ));

        // Permite todos os headers de requisição (Content-Type, Authorization etc.)
        configuracao.setAllowedHeaders(List.of("Content-Type", "Accept", "X-XSRF-TOKEN", "Authorization", "Idempotency-Key"));

        // Expõe o header Content-Disposition para download de arquivos
        configuracao.setExposedHeaders(List.of(HttpHeaders.CONTENT_DISPOSITION, "X-Truncated", "X-Request-Id", AccessDeniedJsonHandler.CSRF_HEADER));

        UrlBasedCorsConfigurationSource origem = new UrlBasedCorsConfigurationSource();
        origem.registerCorsConfiguration("/**", configuracao);

        return origem;
    }
}
