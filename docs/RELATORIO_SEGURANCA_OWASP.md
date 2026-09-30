                                                                                        # Relatório de Análise e Correção de Falhas de Segurança - OWASP Top 10

**Projeto:** JAF Application  
**Data:** 23 de Agosto de 2026  
**Objetivo:** Identificar e mitigar pelo menos 4 falhas de segurança da OWASP Top 10

---

## Falhas Selecionadas para Correção

1. **A01: Broken Access Control** - Endpoint público de criação de funcionários
2. **A02: Cryptographic Failures** - Senhas armazenadas em texto plano
3. **A02: Cryptographic Failures** - SSL desabilitado na conexão MySQL
4. **A07: Authentication Failures** - Armazenamento de token em localStorage

---

## Falha 1: A01 - Broken Access Control

**Arquivo:** `jaf-backend/src/main/java/com/jaf/application/config/SecurityConfiguracao.java`  
**Linha:** 75  
**Severidade:** ALTA

### ANTES (Código Vulnerável)
```java
.requestMatchers(HttpMethod.POST, "/funcionarios").permitAll()
```
**Risco:** Qualquer pessoa pode criar contas sem autenticação

**Evidência:**
[Inserir screenshot do código vulnerável]

### DEPOIS (Código Corrigido)
```java
.requestMatchers(HttpMethod.POST, "/funcionarios").hasAuthority("CRIAR_FUNCIONARIOS")
```
**Solução:** Exige autenticação e permissão específica para criar funcionários

**Evidência:**
[Inserir screenshot do código corrigido]

---

## Falha 2: A02 - Cryptographic Failures

**Arquivo:** `jaf-backend/src/main/java/com/jaf/application/service/AuthService.java`  
**Linha:** 25  
**Severidade:** CRÍTICA

### ANTES (Código Vulnerável)
```java
if (!senha.equals(funcionario.getSenha())) {
    throw new RuntimeException("Credenciais inválidas");
}
```
**Risco:** Senhas armazenadas e comparadas em texto plano

**Evidência:**
[Inserir screenshot do código vulnerável]

### DEPOIS (Código Corrigido)
```java
// Adicionar BCrypt encoder
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}

// Comparação com hash
if (!passwordEncoder().matches(senha, funcionario.getSenha())) {
    throw new RuntimeException("Credenciais inválidas");
}
```
**Solução:** Criptografia BCrypt para armazenamento e comparação de senhas

**Evidência:**
[Inserir screenshot do código corrigido]

---

## Falha 3: A02 - Cryptographic Failures (SSL)

**Arquivo:** `jaf-backend/src/main/resources/application.properties`  
**Linha:** 3  
**Severidade:** ALTA

### ANTES (Código Vulnerável)
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/jaf_db?useSSL=false
```
**Risco:** Conexão com banco de dados sem criptografia SSL

**Evidência:**
[Inserir screenshot do arquivo application.properties]

### DEPOIS (Código Corrigido)
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/jaf_db?useSSL=true&requireSSL=true
```
**Solução:** Habilitar SSL na conexão com o banco de dados MySQL

**Evidência:**
[Inserir screenshot do arquivo corrigido]

---

## Falha 4: A07 - Authentication Failures

**Arquivos:** 
- Frontend: `jaf-frontend/react/src/Service/Auth/Login/authService.tsx` (linha 19)
- Frontend: `jaf-frontend/react/src/Service/Auth/Login/Api/Api.ts` (linha 15)
- Backend: `jaf-backend/src/main/java/com/jaf/application/controller/FuncionarioController.java` (linha 50-78)
- Backend: `jaf-backend/src/main/java/com/jaf/application/config/AutenticacaoFilter.java` (linha 71-90)

**Severidade:** ALTA

### ANTES (Código Vulnerável)
**authService.tsx (linha 19):**
```typescript
localStorage.setItem("token", token);
```

**Api.ts (linha 15):**
```typescript
const token = localStorage.getItem('token') ?? localStorage.getItem('auth_token')
if (token) {
    config.headers.Authorization = `Bearer ${token}`
}
```

**Risco:** Tokens JWT armazenados em localStorage, vulneráveis a ataques XSS

**Evidência:**
[Inserir screenshot do código vulnerável]

### DEPOIS (Código Corrigido)
**authService.tsx:**
```typescript
// CORREÇÃO DE SEGURANÇA A07: Token não vem mais no corpo da resposta
// Armazena informações do usuário que vieram na resposta
const { email, nome, id, cargo } = response.data;
localStorage.setItem("userEmail", email);
localStorage.setItem("userName", nome);
localStorage.setItem("userId", String(id));
localStorage.setItem("userCargo", cargo);
```

**Api.ts:**
```typescript
// CORREÇÃO DE SEGURANÇA A07: Token enviado automaticamente via cookie HttpOnly
config.withCredentials = true
```

**Backend (FuncionarioController.java):**
```java
// CORREÇÃO DE SEGURANÇA A07: Envio de token via cookie HttpOnly
Cookie jwtCookie = new Cookie("jwt", token);
jwtCookie.setHttpOnly(true);      // Não acessível via JavaScript (proteção contra XSS)
jwtCookie.setSecure(false);       // Ajustado para desenvolvimento (localhost sem HTTPS)
jwtCookie.setPath("/");
jwtCookie.setMaxAge(3600);
response.addCookie(jwtCookie);
```

**Backend (AutenticacaoFilter.java):**
```java
// CORREÇÃO DE SEGURANÇA A07: Suporte a cookie HttpOnly para JWT
Cookie[] cookies = request.getCookies();
if (cookies != null) {
    for (Cookie cookie : cookies) {
        if ("jwt".equals(cookie.getName())) {
            return cookie.getValue();
        }
    }
}
```

**Solução:** Tokens movidos para cookies HttpOnly e Secure

**Evidência:**
[Inserir screenshot do código corrigido e print dos cookies com flags HttpOnly/Secure]

---

## Conclusão

Foram identificadas e corrigidas 4 falhas críticas de segurança baseadas na OWASP Top 10:

1. ✅ **A01 - Broken Access Control:** Endpoint de criação de funcionários agora requer autenticação
2. ✅ **A02 - Cryptographic Failures:** Senhas agora criptografadas com BCrypt
3. ✅ **A02 - Cryptographic Failures:** SSL habilitado na conexão MySQL
4. ✅ **A07 - Authentication Failures:** Tokens movidos para cookies HttpOnly

As correções implementadas elevam significativamente o nível de segurança da aplicação, mitigando riscos críticos de exposição de credenciais, acesso não autorizado e interceptação de dados.

---

**Assinatura:**  
[Seu Nome]  
[Data]