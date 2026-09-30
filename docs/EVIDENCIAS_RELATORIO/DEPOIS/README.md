# 📸 Pasta para Evidências DEPOIS

Esta pasta deve conter os prints dos códigos CORRIGIDOS (após as correções).

## 📋 Arquivos a serem criados (você vai gerar agora):

### 1. A01 - Broken Access Control
**1-securityconfig-corrigido.png**
- Arquivo: `jaf-backend/src/main/java/com/jaf/application/config/SecurityConfiguracao.java`
- Linhas: 72-80
- Deve mostrar o comentário de correção e a ausência do `.permitAll()`

### 2. A02 - Cryptographic Failures (BCrypt)
**2-authservice-corrigido.png**
- Arquivo: `jaf-backend/src/main/java/com/jaf/application/service/AuthService.java`
- Linhas: 10-41 (método autenticar completo)
- Deve mostrar os comentários de correção BCrypt

### 3. A02 - Cryptographic Failures (SSL)
**3-applicationproperties-corrigido.png**
- Arquivo: `jaf-backend/src/main/resources/application.properties`
- Linhas: 3-6 e 14-15
- Deve mostrar `useSSL=true&requireSSL=true` com comentários

### 4. A07 - Authentication Failures
**4a-authservice-frontend-corrigido.png**
- Arquivo: `jaf-frontend/react/src/Service/Auth/Login/authService.tsx`
- Linhas: 10-27 (método login)

**4b-api-corrigido.png**
- Arquivo: `jaf-frontend/react/src/Service/Auth/Login/Api/Api.ts`
- Linhas: 13-44 (interceptors)

**4c-funcionariocontroller-corrigido.png**
- Arquivo: `jaf-backend/src/main/java/com/jaf/application/controller/FuncionarioController.java`
- Linhas: 50-78 (método login com cookie)

**4d-autenticaofilter-corrigido.png**
- Arquivo: `jaf-backend/src/main/java/com/jaf/application/config/AutenticacaoFilter.java`
- Linhas: 71-90 (método extrairToken)

## 🎯 Dicas para Prints de Qualidade:

1. **Use o VS Code** para abrir os arquivos
2. **Capture contexto suficiente** (mostre os comentários de correção)
3. **Mostre números de linha** se possível
4. **Use zoom adequado** para legibilidade
5. **Salve em PNG** para melhor qualidade

## 📋 Checklist:

- [ ] 1-securityconfig-corrigido.png
- [ ] 2-authservice-corrigido.png
- [ ] 3-applicationproperties-corrigido.png
- [ ] 4a-authservice-frontend-corrigido.png
- [ ] 4b-api-corrigido.png
- [ ] 4c-funcionariocontroller-corrigido.png
- [ ] 4d-autenticaofilter-corrigido.png

**Total: 7 prints para evidências DEPOIS**