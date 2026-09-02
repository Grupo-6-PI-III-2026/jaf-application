# 📸 Pasta para Evidências ANTES

Esta pasta deve conter os prints dos códigos VULNERÁVEIS (antes das correções).

## 📋 Arquivos necessários (fornecidos pelo Luis):

1. **1-antes-securityconfig.png** - SecurityConfiguracao.java linha 75
   - Deve mostrar: `.requestMatchers(HttpMethod.POST, "/funcionarios").permitAll()`

2. **2-antes-authservice.png** - AuthService.java linha 25
   - Deve mostrar: `if (!senha.equals(funcionario.getSenha()))`

3. **3-antes-applicationproperties.png** - application.properties linha 3
   - Deve mostrar: `useSSL=false`

4. **4-antes-authservice-frontend.png** - authService.tsx linha 19 e Api.ts linha 15
   - Deve mostrar: `localStorage.setItem("token", token)` e leitura do token

## ⚠️ Se estes arquivos não estiverem aqui:

- Peça ao Luis para fornecer os prints ANTES
- Ou verifique se ele os salvou em outra localização
- Sem as evidências ANTES, o relatório fica incompleto

## 📝 Como foram obtidos (se precisar recriar):

Esses prints foram tirados antes das correções serem implementadas. Se precisar recriar:
- Use `git diff HEAD~1 HEAD` para ver o código ANTES
- Ou use `git show HEAD~1:nome_do_arquivo` para ver a versão anterior