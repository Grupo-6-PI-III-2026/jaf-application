# 📍 Localização Exata dos Códigos para Evidências

## 1. A01 - Broken Access Control

### 📁 Arquivo
`jaf-backend/src/main/java/com/jaf/application/config/SecurityConfiguracao.java`

### 🔍 Linhas Específicas
**Linha 75-80:** Região da correção
```java
// CORREÇÃO DE SEGURANÇA A01: Removido acesso público ao cadastro de funcionários
// Antes: .requestMatchers(HttpMethod.POST, "/funcionarios").permitAll()
// Agora: exige autenticação e permissão específica (ver @PreAuthorize no controller)
.requestMatchers("/uploads/**").permitAll()  // fotos públicas
```

### 📸 O que printar
- Capture as linhas 72-80 para mostrar o contexto
- Destaque a linha 75 onde estava o `.permitAll()` removido

---

## 2. A02 - Cryptographic Failures (BCrypt)

### 📁 Arquivo
`jaf-backend/src/main/java/com/jaf/application/service/AuthService.java`

### 🔍 Linhas Específicas
**Linha 10-13:** JavaDoc e import do PasswordEncoder
```java
/**
 * Serviço de autenticação com suporte a BCrypt
 * CORREÇÃO DE SEGURANÇA A02: Implementação de criptografia BCrypt para comparação de senhas
 */
```

**Linha 17:** Campo passwordEncoder
```java
private final PasswordEncoder passwordEncoder;
```

**Linha 19-22:** Construtor com injeção
```java
public AuthService(FuncionarioRepository funcionarioRepository, PasswordEncoder passwordEncoder) {
    this.funcionarioRepository = funcionarioRepository;
    this.passwordEncoder = passwordEncoder;
}
```

**Linha 32-38:** Correção principal
```java
// CORREÇÃO DE SEGURANÇA A02: Comparação usando BCrypt em vez de texto plano
// Antes: if (!senha.equals(funcionario.getSenha()))
// Agora: if (!passwordEncoder.matches(senha, funcionario.getSenha()))
// Isso garante que senhas armazenadas como hash sejam comparadas corretamente
if (!passwordEncoder.matches(senha, funcionario.getSenha())) {
    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais invalidas");
}
```

### 📸 O que printar
- Capture o método `autenticar` completo (linhas 24-41)
- Mostre os comentários de correção

---

## 3. A02 - Cryptographic Failures (SSL)

### 📁 Arquivo
`jaf-backend/src/main/resources/application.properties`

### 🔍 Linhas Específicas
**Linha 3-6:** Datasource com SSL habilitado
```properties
# CORREÇÃO DE SEGURANÇA A02: Habilitar SSL na conexão com MySQL
# Antes: useSSL=false (dados transmitidos sem criptografia)
# Agora: useSSL=true&requireSSL=true (dados criptografados em trânsito)
spring.datasource.url=jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:jaf_db}?createDatabaseIfNotExist=true&useSSL=true&requireSSL=true&allowPublicKeyRetrieval=true
```

**Linha 14-15:** Flyway com SSL habilitado
```properties
# CORREÇÃO DE SEGURANÇA A02: Habilitar SSL também na conexão do Flyway
spring.flyway.url=jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:jaf_db}?createDatabaseIfNotExist=true&useSSL=true&requireSSL=true&allowPublicKeyRetrieval=true
```

### 📸 O que printar
- Capture as linhas 3-6 (datasource)
- Capture as linhas 14-15 (flyway)
- Mostre os comentários explicativos

---

## 4. A07 - Authentication Failures (Cookies HttpOnly)

### 📁 Arquivo 1: Frontend Login Service
`jaf-frontend/react/src/Service/Auth/Login/authService.tsx`

### 🔍 Linhas Específicas
**Linha 17-25:** Método login corrigido
```typescript
// CORREÇÃO DE SEGURANÇA A07: Token não vem mais no corpo da resposta
// Antes: const { token } = response.data;
// Agora: Token é enviado via cookie HttpOnly pelo backend (proteção contra XSS)
// Armazena informações do usuário que vieram na resposta
const { email, nome, id, cargo } = response.data;
localStorage.setItem("userEmail", email);
localStorage.setItem("userName", nome);
localStorage.setItem("userId", String(id));
localStorage.setItem("userCargo", cargo);
```

**Linha 44-58:** Método logout corrigido
```typescript
logout: async () => {
    // CORREÇÃO DE SEGURANÇA A07: Chama endpoint de logout do backend para limpar o cookie
    try {
      await api.post("/funcionarios/logout");
    } catch (error) {
      console.error("Erro ao fazer logout:", error);
    }
    
    // Limpa dados do usuário do localStorage
    localStorage.removeItem("userEmail");
    localStorage.removeItem("userName");
    localStorage.removeItem("userId");
    localStorage.removeItem("userCargo");
    window.location.href = "/";
  },
```

### 📸 O que printar
- Capture o método `login` (linhas 10-27)
- Capture o método `logout` (linhas 44-58)

---

### 📁 Arquivo 2: Frontend API Config
`jaf-frontend/react/src/Service/Auth/Login/Api/Api.ts`

### 🔍 Linhas Específicas
**Linha 13-27:** Interceptors corrigidos
```typescript
api.interceptors.request.use(
    (config) => {
    // CORREÇÃO DE SEGURANÇA A07: Removida leitura de token do localStorage
    // Antes: const token = localStorage.getItem('token') ?? localStorage.getItem('auth_token')
    //        if (token) { config.headers.Authorization = `Bearer ${token}` }
    // Agora: Token é enviado automaticamente via cookie HttpOnly pelo browser
    // withCredentials: true deve ser configurado para enviar cookies
    config.withCredentials = true
    
        return config;
    },
    (error) => {
        return Promise.reject(error)
    }
)
```

**Linha 29-44:** Response interceptor corrigido
```typescript
api.interceptors.response.use(
  (response) => response,
  (error) => {
   
    if (error.response?.status === 401) {
      // CORREÇÃO DE SEGURANÇA A07: Limpa dados do usuário do localStorage
      // Token é gerenciado via cookie HttpOnly pelo backend
      localStorage.removeItem('userEmail');
      localStorage.removeItem('userName');
      localStorage.removeItem('userId');
      localStorage.removeItem('userCargo');
      window.location.href = '/';
    }
    return Promise.reject(error);
  }
);
```

### 📸 O que printar
- Capture os interceptors (linhas 13-44)
- Destaque o `config.withCredentials = true`

---

### 📁 Arquivo 3: Backend Controller
`jaf-backend/src/main/java/com/jaf/application/controller/FuncionarioController.java`

### 🔍 Linhas Específicas
**Linha 14-15:** Imports adicionados
```java
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
```

**Linha 50-78:** Método login corrigido
```java
@PostMapping("/login")
public ResponseEntity<FuncionarioTokenDto> login(@Valid @RequestBody FuncionarioLoginDto loginDto, HttpServletResponse response) {
    Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(loginDto.getEmail(), loginDto.getSenha())
    );
    
    SecurityContextHolder.getContext().setAuthentication(authentication);
    String token = gerenciadorTokenJwt.generateToken(authentication);
    
    // CORREÇÃO DE SEGURANÇA A07: Envio de token via cookie HttpOnly em vez de no corpo da resposta
    // Antes: return ResponseEntity.ok(new FuncionarioTokenDto(loginDto.getEmail(), token));
    // Agora: Token enviado via cookie com flags de segurança
    Cookie jwtCookie = new Cookie("jwt", token);
    jwtCookie.setHttpOnly(true);      // Não acessível via JavaScript (proteção contra XSS)
    // Secure deve ser true em produção (HTTPS), false em desenvolvimento (HTTP)
    jwtCookie.setSecure(false);       // Ajustado para desenvolvimento (localhost sem HTTPS)
    jwtCookie.setPath("/");            // Disponível em todo o domínio
    jwtCookie.setMaxAge(3600);         // 1 hora de validade (mesma configuração do JWT)
    response.addCookie(jwtCookie);
    
    // Busca informações do funcionário para retornar no DTO (sem o token)
    var funcionario = funcionarioService.buscarPorEmail(loginDto.getEmail());
    FuncionarioTokenDto dto = new FuncionarioTokenDto();
    dto.setEmail(funcionario.getEmail());
    dto.setNome(funcionario.getNome());
    dto.setId(funcionario.getId());
    dto.setCargo(funcionario.getCargoGlobal());
    dto.setToken(null); // Token está no cookie, não no corpo da resposta
    
    return ResponseEntity.ok(dto);
}
```

**Linha 81-93:** Método logout adicionado
```java
@PostMapping("/logout")
public ResponseEntity<Void> logout(HttpServletResponse response) {
    // CORREÇÃO DE SEGURANÇA A07: Limpa o cookie HttpOnly no logout
    Cookie jwtCookie = new Cookie("jwt", "");
    jwtCookie.setHttpOnly(true);
    jwtCookie.setSecure(false);       // Ajustado para desenvolvimento (localhost sem HTTPS)
    jwtCookie.setPath("/");
    jwtCookie.setMaxAge(0); // Remove o cookie imediatamente
    response.addCookie(jwtCookie);
    
    SecurityContextHolder.clearContext();
    return ResponseEntity.noContent().build();
}
```

### 📸 O que printar
- Capture o método `login` (linhas 50-78)
- Capture o método `logout` (linhas 81-93)
- Destaque a configuração do cookie HttpOnly

---

### 📁 Arquivo 4: Backend Filter
`jaf-backend/src/main/java/com/jaf/application/config/AutenticacaoFilter.java`

### 🔍 Linhas Específicas
**Linha 71-90:** Método extrairToken corrigido
```java
private String extrairToken(HttpServletRequest request) {
    // CORREÇÃO DE SEGURANÇA A07: Suporte a cookie HttpOnly para JWT
    // Prioridade 1: cookie "jwt" HttpOnly (browser - proteção contra XSS)
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
        for (Cookie cookie : cookies) {
            if ("jwt".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
    }

    // Prioridade 2: header Authorization: Bearer (Swagger, Postman, server-to-server)
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        return authHeader.substring(7);
    }

    return null;
}
```

### 📸 O que printar
- Capture o método `extrairToken` (linhas 71-90)
- Mostre a lógica de prioridade (cookie primeiro, header depois)

---

## 🎯 Dicas para Prints de Qualidade

1. **Use o VS Code** para abrir os arquivos - tem syntax highlighting
2. **Capture contexto suficiente** (5-10 linhas antes/depois)
3. **Mostre os números de linha** se possível
4. **Destaque os comentários de correção** (eles explicam ANTES/DEPOIS)
5. **Use zoom adequado** para que o código seja legível

## 📋 Checklist de Evidências DEPOIS

- [ ] SecurityConfiguracao.java (linha 75-80)
- [ ] AuthService.java (método autenticar completo)
- [ ] application.properties (linhas 3-6 e 14-15)
- [ ] authService.tsx (método login)
- [ ] authService.tsx (método logout)
- [ ] Api.ts (interceptors)
- [ ] FuncionarioController.java (método login)
- [ ] FuncionarioController.java (método logout)
- [ ] AutenticacaoFilter.java (método extrairToken)

---

**Total: 9 prints para evidências DEPOIS**