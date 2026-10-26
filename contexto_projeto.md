# Contexto atual — JAF na AWS



## Arquitetura



A aplicação JAF está sendo implantada na AWS (`us-east-1`) com Terraform, Docker e GitHub Actions.



VPC distribuída em múltiplas Availability Zones.
Subnets públicas e privadas.
ALB público para o frontend.
ALB interno para o backend.
Auto Scaling Groups para frontend, backend e OCR.
RDS PostgreSQL configurado para alta disponibilidade.
Frontend React servido por Nginx.
Backend Spring Boot 3.4.5 com Java 21.
Autenticação via JWT em cookie HttpOnly.


Imagens publicadas no GHCR:



```text

ghcr.io/grupo-6-pi-iii-2026/jaf-frontend:latest

ghcr.io/grupo-6-pi-iii-2026/jaf-backend:latest

```



## Problema original



O health check do backend usava:



```text

/actuator/health

```



As instâncias antigas retornavam `HTTP 500` e os logs indicavam:



```text

No static resource actuator/health.

```



A causa provável era uma imagem Docker antiga, criada antes de o Actuator estar disponível/configurado corretamente. O ALB considerava as instâncias não saudáveis porque o endpoint de health check não funcionava.



## Situação validada do backend



Uma nova imagem foi publicada e baixada por uma nova instância. O backend iniciou com sucesso e os logs confirmaram:



conexão com o RDS PostgreSQL;
validação das migrations do Flyway;
schema atualizado;
inicialização do JPA/Hibernate;
exposição do Actuator;
Tomcat escutando na porta `8080`;
execução do seed de dados.


Teste realizado na instância do backend:



```bash

curl -i http://localhost:8080/actuator/health

```



Resultado:



```http

HTTP/1.1 200

```



```json

{"status":"UP"}

```



O login direto no backend também foi validado:



```bash

curl -i -X POST http://localhost:8080/funcionarios/login \

  -H "Content-Type: application/json" \

  -d '{"email":"admin@gmail.com","senha":"<SENHA>"}'

```



Resultado:



```http

HTTP/1.1 200

```



O backend retornou os dados do funcionário e enviou um cookie JWT. O token não deve ser compartilhado ou registrado neste documento.



Conclusão: o backend, o banco, o Actuator e o login direto estão funcionando.



## Problema atual



Pelo navegador, o frontend ainda apresenta:



```text

Failed to load resource: the server responded with a status of 403

AxiosError: Request failed with status code 403

```



A requisição esperada é:



```http

POST /api/funcionarios/login

```



O fluxo completo é:



```text

Navegador

  -> ALB público do frontend

  -> Nginx

  -> ALB interno do backend

  -> POST /funcionarios/login

```



Como o login direto no backend retorna `200`, o problema está provavelmente no caminho frontend/Nginx/ALB ou na validação de origem CORS.



## Correção de CORS



O backend originalmente permitia apenas origens locais:



```text

http://localhost:5173

http://localhost:3000

```



Foi alterado `SecurityConfiguracao.java` para usar `allowedOriginPatterns` e permitir também domínios de ALB:



```java

configuracao.setAllowedOriginPatterns(List.of(

        "http://localhost:5173",

        "http://localhost:3000",

        "http://*.elb.amazonaws.com",

        "https://*.elb.amazonaws.com"

));

```



O uso de `allowedOriginPatterns` mantém compatibilidade com:



```java

configuracao.setAllowCredentials(true);

```



A nova imagem precisa conter essa alteração. Publicar uma nova imagem com a tag `latest` não atualiza containers existentes automaticamente.



## Atualização das instâncias



O `Instance Refresh` não pôde ser usado porque o AWS Learner Lab bloqueou:



```text

autoscaling:StartInstanceRefresh

```



O bloqueio veio de uma Service Control Policy organizacional (`explicit deny`). Essa restrição não pode ser contornada pelo Terraform ou por permissões comuns da role.



A alternativa é substituir as instâncias do ASG manualmente, uma por vez:



Abrir o ASG correspondente.
Entrar em **Gerenciamento de instâncias**.
Encerrar uma instância.
Aguardar o ASG criar outra.
Aguardar o Target Group indicar `healthy`.
Repetir somente depois que a nova instância estiver saudável.


Não executar `terraform destroy`.



## Verificações pendentes



Na instância do frontend, verificar se o Nginx aponta para o ALB interno:



```bash

sudo docker exec jaf-frontend grep -n "proxy_pass" /etc/nginx/conf.d/default.conf

```



O resultado esperado deve apontar para um DNS semelhante a:



```nginx

proxy_pass http://internal-jaf-backend-xxxxx.us-east-1.elb.amazonaws.com:8080;

```



Se aparecer `jaf-backend:8080`, a instância está usando a configuração local do Docker Compose.



Ainda na instância do frontend, testar o fluxo completo:



```bash

curl -i -X POST http://localhost/api/funcionarios/login \

  -H "Content-Type: application/json" \

  -d '{"email":"admin@gmail.com","senha":"<SENHA>"}'

```



Interpretação:



`200`: Nginx, ALB e backend funcionam; investigar CORS ou cache do navegador.
`403`: bloqueio de segurança/CORS.
`404`: rewrite ou rota incorreta.
`502`: Nginx não alcança o ALB interno.
`504`: timeout de rede.
`401`: credenciais inválidas.


Para acompanhar o Nginx:



```bash

sudo docker exec jaf-frontend tail -f /var/log/nginx/access.log

sudo docker exec jaf-frontend tail -f /var/log/nginx/error.log

```



Para acompanhar o backend:



```bash

sudo docker logs -f --tail 100 jaf-backend

```



## Acesso à instância frontend



O acesso via EC2 Instance Connect ao frontend ainda não foi possível. Para a conexão direta funcionar, a instância precisa ter:



IP público;
subnet pública;
rota para o Internet Gateway;
porta TCP `22` liberada no Security Group;
serviço SSH ativo;
usuário `ec2-user`;
permissões IAM compatíveis.


O funcionamento da porta `80` pelo ALB não garante que a porta `22` esteja acessível diretamente.



## Resumo final



### Confirmado



Terraform válido.
Infraestrutura multi-AZ configurada.
Backend atualizado em uma nova instância.
RDS acessível.
Flyway funcionando.
Actuator disponível.
`/actuator/health` retorna `HTTP 200`.
Login direto no backend retorna `HTTP 200`.
JWT é gerado corretamente.


### Pendente



Confirmar o login passando pelo frontend.
Confirmar a configuração efetiva do Nginx.
Confirmar o ALB interno usado pelo frontend.
Confirmar CORS com a origem real do navegador.
Conseguir acesso à instância frontend.
Atualizar todas as instâncias antigas do frontend/backend quando necessário.


## Cuidados



Não executar `terraform destroy`.
Não compartilhar senhas, tokens JWT, tokens GHCR ou segredos.
O JWT usado nos testes deve ser considerado exposto e não deve ser reutilizado.
O acesso público temporário do backend deve ser desabilitado após o diagnóstico:


```hcl

backend_public_access = false

```  