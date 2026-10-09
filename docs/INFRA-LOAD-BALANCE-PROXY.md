# Infraestrutura com Load Balance e Proxy Reverso

## Resumo das Mudanças

Este documento descreve as mudanças implementadas para adicionar **Load Balance** e **Proxy Reverso** à infraestrutura do projeto JAF.

---

## 1. Proxy Reverso no Nginx (Frontend)

### Arquivo Modificado
- `jaf-frontend/react/nginx.conf`

### Melhorias Implementadas
- **Timeouts e Buffers**: Configurações otimizadas para performance
- **Headers de Segurança**: X-Frame-Options, X-Content-Type-Options, X-XSS-Protection, Referrer-Policy
- **Compressão Gzip**: Ativado para reduzir tamanho de respostas
- **Health Check Endpoint**: Novo endpoint `/health` para monitoramento
- **Logs**: Configuração explícita de access e error logs
- **Headers de Proxy Adicionais**: Upgrade, X-Forwarded-Host, X-Forwarded-Port
- **Cache**: Estratégia de cache para arquivos estáticos (1h) e uploads (30d)
- **Connection Header**: `Connection ""` para HTTP/1.1 keep-alive

### Comportamento
- `/api/*` → Proxy para backend (porta 8080)
- `/uploads/*` → Proxy para backend (porta 8080)
- `/*` → Arquivos estáticos do React
- `/health` → Health check endpoint (retorna "healthy")

---

## 2. AWS Application Load Balancer (ALB)

### Novo Módulo Criado
- `terraform/modules/alb/` (main.tf, variables.tf, outputs.tf)

### Recursos Criados
- **Security Group do ALB**: Permite HTTP/HTTPS da internet
- **Application Load Balancer**: ALB público na subnet pública
- **Target Group Frontend**: Para instâncias EC2 do frontend (porta 80)
- **Target Group Backend**: Para instâncias EC2 do backend (porta 8080)
- **Listener HTTP (Porta 80)**: Roteia tráfego baseado no path
- **Listener Rule `/api/*`**: Roteia requisições de API para o backend
- **Target Group Attachments**: Anexa instâncias EC2 aos target groups

### Roteamento do ALB
- `/api/*` → Backend Target Group (porta 8080)
- `/*` → Frontend Target Group (porta 80)

### Health Checks
- **Frontend**: GET `/` → espera HTTP 200
- **Backend**: GET `/actuator/health` → espera HTTP 200

---

## 3. Security Groups Atualizados

### Arquivo Modificado
- `terraform/modules/security_groups/main.tf`
- `terraform/modules/security_groups/outputs.tf`

### Mudanças
- **Novo SG ALB**: Aceita HTTP/HTTPS da internet
- **SG Frontend**: Permite acesso público (ALB) e SSH
- **SG Backend**: Permite acesso do Frontend E do ALB (para health checks)

---

## 4. Módulo VPC Atualizado

### Arquivo Modificado
- `terraform/modules/vpc/outputs.tf`

### Novo Output
- `public_subnet_ids`: Lista de IDs das subnets públicas (para ALB)

---

## 5. Integração ALB no Terraform

### Arquivo Modificado
- `terraform/main.tf`
- `terraform/outputs.tf`

### Novo Módulo
```hcl
module "alb" {
  source = "./modules/alb"
  project_name           = var.project_name
  environment            = var.environment
  vpc_id                 = module.vpc.vpc_id
  public_subnet_ids      = module.vpc.public_subnet_ids
  alb_security_group_id  = module.security_groups.sg_alb_id
  frontend_instance_id   = module.ec2.frontend_instance_id
  backend_instance_id    = module.ec2.backend_instance_id
}
```

### Novos Outputs
- `alb_dns_name`: DNS name do ALB
- `alb_url`: URL completa (http://alb-dns-name)

---

## 6. Spring Boot Actuator

### Arquivos Modificados
- `jaf-backend/pom.xml`
- `jaf-backend/src/main/resources/application.properties`

### Dependência Adicionada
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### Configuração Adicionada
```properties
# Actuator — health check para ALB
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=never
```

### Permissão de Segurança
- O endpoint `/actuator/*` já estava na lista `URLS_PERMITIDAS` em `SecurityConfiguracao.java`

---

## 7. Docker Compose (Ambiente Local)

### Situação Atual
- O `docker-compose.yml` **JÁ POSSUI** proxy reverso configurado via Nginx
- O frontend roteia `/api/*` para o backend usando nome de container `jaf-backend`
- **Nenhuma mudança necessária** para ambiente local

---

## Arquitetura Final

### Ambiente Local (Docker Compose)
```
Internet → Frontend (Nginx:80) → Backend (8080) → PostgreSQL (5432)
                  ↓
               OCR (8000)
```

### Ambiente AWS (com ALB)
```
Internet → ALB (HTTP:80) → Frontend EC2 (Nginx:80) → Backend EC2 (8080) → RDS
                  ↓                    ↓
               /api/* → Backend     OCR EC2 (8081)
               /* → React estático
```

---

## Como Usar

### Ambiente Local
```bash
# Subir infraestrutura local
docker-compose up -d

# Acessar aplicação
http://localhost
```

### Ambiente AWS
```bash
# Aplicar Terraform
cd terraform
terraform init
terraform apply

# Após apply, ver o output `alb_url`
# Exemplo: http://jaf-project-alb-1234567890.us-east-1.elb.amazonaws.com
```

### Acessar pela URL do ALB
```bash
# Frontend
http://<alb-dns-name>/

# API
http://<alb-dns-name>/api/*

# Health check
http://<alb-dns-name>/health
```

---

## Considerações Importantes

### 1. Health Check do Backend
- O ALB verifica `/actuator/health` no backend
- O endpoint retorna HTTP 200 quando o backend está saudável
- Se o backend cair, o ALB para de enviar tráfego para ele

### 2. Security Groups
- O SG Backend permite tráfego do ALB (porta 8080) para health checks
- O SG Frontend permite tráfego do ALB (porta 80) para health checks
- SSH ainda funciona via jump host (frontend)

### 3. Roteamento Duplo
- **ALB**: Roteia `/api/*` para backend diretamente
- **Nginx do Frontend**: Roteia `/api/*` para backend (caminho alternativo)
- Isso permite flexibilidade e redundância

### 4. SSL/HTTPS
- O ALB está preparado para HTTPS (porta 443 aberta)
- Para habilitar, adicionar certificado ACM e configurar listener HTTPS
- Atualmente opera apenas em HTTP

### 5. Escalabilidade
- Para escalar horizontalmente:
  1. Adicionar Auto Scaling Group para frontend
  2. Adicionar Auto Scaling Group para backend
  3. O ALB distribuirá tráfego automaticamente

---

## Testes Recomendados

### 1. Testar Proxy Reverso Local
```bash
# Subir docker-compose
docker-compose up -d

# Testar health check
curl http://localhost/health

# Testar API via proxy
curl http://localhost/api/funcionarios
```

### 2. Testar ALB (após deploy AWS)
```bash
# Testar health check
curl http://<alb-dns-name>/health

# Testar frontend
curl http://<alb-dns-name>/

# Testar API via ALB
curl http://<alb-dns-name>/api/actuator/health
```

### 3. Testar Health Check do Backend
```bash
# Verificar se endpoint responde
curl http://localhost:8080/actuator/health
```

---

## Rollback (Se Necessário)

### Para Reverter Mudanças Locais
```bash
# Reverter nginx.conf
git checkout jaf-frontend/react/nginx.conf

# Reverter application.properties
git checkout jaf-backend/src/main/resources/application.properties

# Reverter pom.xml
git checkout jaf-backend/pom.xml
```

### Para Reverter Mudanças Terraform
```bash
# Remover módulo ALB do main.tf
# Remover outputs do ALB de outputs.tf
# Reverter security_groups/main.tf
# Reverter vpc/outputs.tf

# Aplicar rollback
terraform apply
```

---

## Próximos Passos (Opcionais)

1. **Configurar HTTPS no ALB**:
   - Adicionar certificado ACM
   - Criar listener HTTPS (porta 443)
   - Redirecionar HTTP → HTTPS

2. **Adicionar Auto Scaling Groups**:
   - Criar ASG para frontend
   - Criar ASG para backend
   - Configurar políticas de escalamento

3. **Monitoramento**:
   - CloudWatch alarms para ALB
   - CloudWatch metrics para instâncias
   - CloudWatch logs para Nginx

4. **CDN (CloudFront)**:
   - Adicionar CloudFront na frente do ALB
   - Cache de arquivos estáticos
   - WAF (Web Application Firewall)

---

## Segurança

- ✅ Security groups restritivos (princípio de least privilege)
- ✅ Health checks não expõem informações sensíveis (`show-details=never`)
- ✅ Headers de segurança no Nginx
- ✅ Health check endpoint do Actuator já permitido no Spring Security
- ✅ ALB em subnet pública, instâncias em subnets privadas (backend/OCR)

---

## Impacto Zero

### Compatibilidade Mantida
- ✅ Docker Compose local funciona sem mudanças
- ✅ Script `frontend.sh` mantém compatibilidade (substitui `jaf-backend` por IP)
- ✅ Security groups mantêm acesso SSH existente
- ✅ RDS e OCR não foram afetados
- ✅ Variáveis de ambiente do backend não foram alteradas

### Sem Quebra de Funcionalidade
- ✅ Proxy reverso era parcialmente implementado (agora melhorado)
- ✅ Health checks são novos, não interferem em operação existente
- ✅ ALB adiciona camada de roteamento, não quebra comunicação direta
- ✅ Spring Security permite `/actuator/*` (já configurado)

---

## Conclusão

A infraestrutura agora possui:
1. **Proxy Reverso** otimizado no Nginx do frontend (melhores práticas)
2. **Load Balance** via AWS ALB (roteamento inteligente baseado em path)
3. **Health Checks** automáticos via Spring Boot Actuator
4. **Segurança** aprimorada com headers e security groups
5. **Escalabilidade** preparada para Auto Scaling futuro

Todas as mudanças são **seguras**, **backward compatible** e **não impactam** a operação atual.
