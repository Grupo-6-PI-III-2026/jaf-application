# 📋 Instruções para Montar o Relatório de Segurança OWASP

## 🎯 Objetivo
Montar o relatório final com evidências ANTES e DEPOIS das 4 correções de segurança implementadas.

## 📁 Estrutura de Pastas

```
EVIDENCIAS_RELATORIO/
├── README_PARA_COLEGA.md          (este arquivo)
├── RELATORIO_SEGURANCA_OWASP.html (relatório em HTML para abrir no Word)
├── INSTRUCOES_CODIGOS.md          (localização exata dos códigos)
├── ANTES/                         (placeholders para prints ANTES)
└── DEPOIS/                        (placeholders para prints DEPOIS)
```

## 🔍 Análise das Correções Implementadas

### 1. A01 - Broken Access Control
**Arquivo:** `jaf-backend/src/main/java/com/jaf/application/config/SecurityConfiguracao.java`
**Linha:** 75
**Antes:** `.requestMatchers(HttpMethod.POST, "/funcionarios").permitAll()`
**Depois:** Linha removida, agora exige autenticação

### 2. A02 - Cryptographic Failures (BCrypt)
**Arquivo:** `jaf-backend/src/main/java/com/jaf/application/service/AuthService.java`
**Linha:** 25-36
**Antes:** `if (!senha.equals(funcionario.getSenha()))`
**Depois:** `if (!passwordEncoder.matches(senha, funcionario.getSenha()))`

### 3. A02 - Cryptographic Failures (SSL)
**Arquivo:** `jaf-backend/src/main/resources/application.properties`
**Linha:** 3 e 15
**Antes:** `useSSL=false`
**Depois:** `useSSL=true&requireSSL=true`

### 4. A07 - Authentication Failures (Cookies HttpOnly)
**Arquivos:**
- `jaf-frontend/react/src/Service/Auth/Login/authService.tsx` (linha 19)
- `jaf-frontend/react/src/Service/Auth/Login/Api/Api.ts` (linha 15)
- `jaf-backend/src/main/java/com/jaf/application/controller/FuncionarioController.java` (linha 50-78)
- `jaf-backend/src/main/java/com/jaf/application/config/AutenticacaoFilter.java` (linha 71-90)

## 📸 Como Gerar Evidências

### Evidências ANTES (prints que o Luis deve ter fornecido)
Se os prints ANTES não estiverem na pasta `ANTES/`, peça ao Luis para fornecer:
1. Print do SecurityConfiguracao.java linha 75
2. Print do AuthService.java linha 25
3. Print do application.properties linha 3
4. Print do authService.tsx linha 19 e Api.ts linha 15

### Evidências DEPOIS (você vai gerar agora)

#### 1. A01 - Broken Access Control
**Abra:** `jaf-backend/src/main/java/com/jaf/application/config/SecurityConfiguracao.java`
**Procure:** Linha 75 (deve ter comentário de correção)
**Print:** Capture a região da linha 75-80
**Salve em:** `DEPOIS/1-securityconfig-corrigido.png`

#### 2. A02 - Cryptographic Failures (BCrypt)
**Abra:** `jaf-backend/src/main/java/com/jaf/application/service/AuthService.java`
**Procure:** Linhas 32-36 (deve ter comentário de correção BCrypt)
**Print:** Capture o método autenticar completo
**Salve em:** `DEPOIS/2-authservice-corrigido.png`

#### 3. A02 - Cryptographic Failures (SSL)
**Abra:** `jaf-backend/src/main/resources/application.properties`
**Procure:** Linhas 3-6 e 14-15 (devem ter comentários de correção SSL)
**Print:** Capture as linhas com useSSL=true
**Salve em:** `DEPOIS/3-applicationproperties-corrigido.png`

#### 4. A07 - Authentication Failures
**Abra:** `jaf-frontend/react/src/Service/Auth/Login/authService.tsx`
**Procure:** Linhas 17-25 (login method)
**Print:** Capture o método login
**Salve em:** `DEPOIS/4a-authservice-frontend-corrigido.png`

**Abra:** `jaf-frontend/react/src/Service/Auth/Login/Api/Api.ts`
**Procure:** Linhas 13-27 (interceptors)
**Print:** Capture a configuração withCredentials
**Salve em:** `DEPOIS/4b-api-corrigido.png`

**Abra:** `jaf-backend/src/main/java/com/jaf/application/controller/FuncionarioController.java`
**Procure:** Linhas 58-67 (cookie setup)
**Print:** Capture a configuração do cookie HttpOnly
**Salve em:** `DEPOIS/4c-funcionariocontroller-corrigido.png`

**Abra:** `jaf-backend/src/main/java/com/jaf/application/config/AutenticacaoFilter.java`
**Procure:** Linhas 71-90 (cookie extraction)
**Print:** Capture o método extrairToken
**Salve em:** `DEPOIS/4d-autenticaofilter-corrigido.png`

## 📝 Como Montar o Relatório Final

1. **Abra o arquivo `RELATORIO_SEGURANCA_OWASP.html` no Word**
   - Clique duplo no arquivo
   - Copie o conteúdo e cole no Word
   - Ou use: Arquivo → Abrir → selecionar o arquivo HTML

2. **Insira as evidências ANTES**
   - Localize os campos azuis `[Inserir screenshot...]`
   - Insira os prints fornecidos pelo Luis

3. **Insira as evidências DEPOIS**
   - Insira os prints que você gerou na pasta `DEPOIS/`
   - Siga a ordem das falhas no relatório

4. **Ajuste a formatação** (se necessário)
   - Redimensione as imagens para caber bem
   - Ajuste o espaçamento
   - Verifique se todos os campos de evidência estão preenchidos

5. **Salve como PDF ou DOCX**
   - Arquivo → Salvar como → PDF ou DOCX

## 🔧 Comandos Git Úteis (para análise)

```bash
# Ver o último commit (correções de segurança)
git show HEAD

# Ver diferenças ANTES/DEPOIS
git diff HEAD~1 HEAD

# Ver mudanças em arquivo específico
git diff HEAD~1 HEAD -- jaf-backend/src/main/java/com/jaf/application/config/SecurityConfiguracao.java

# Ver histórico de commits
git log --oneline -5
```

## ⚠️ Observações Importantes

1. **Cookie Secure=false:** No código está `setSecure(false)` porque é desenvolvimento (localhost sem HTTPS). Em produção deve ser `true`. No relatório, explique isso se necessário.

2. **Compatibilidade mantida:** O filtro ainda suporta header Authorization para Swagger/Postman. Isso é uma boa prática.

3. **Comentários de documentação:** Todos os códigos corrigidos têm comentários explicativos que servem como documentação.

## 📞 Dúvidas

Se tiver dúvidas sobre alguma correção específica, pode:
- Analisar os comentários no código (todos explicam ANTES/DEPOIS)
- Ver o diff do git para entender as mudanças
- Entrar em contato com o Luis para esclarecimentos

---

**Boa sorte com o relatório! 🚀**