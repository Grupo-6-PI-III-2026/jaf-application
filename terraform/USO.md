
## Gerar par de chaves SSH

```bash
ssh-keygen -t rsa -b 4096 -f ~/.ssh/id_rsa
cat ~/.ssh/id_rsa.pub
```

---

## Definir a senha do banco de dados

Edite `terraform/terraform.tfvars`:

```hcl
db_password = "MinhaSenh@Forte123!"
```

---

## Executar o Terraform

Os comandos abaixo são para Windows PowerShell. Sempre execute o `plan` e o
`apply` usando o mesmo workspace, o mesmo arquivo de variáveis e a mesma conta
AWS.

### Preparação comum

Execute uma vez, ou novamente caso o Terraform ainda não tenha sido inicializado:

```powershell
cd "C:\Users\guilherme\Documents\4-SEMESTRE\projeto para aws\jaf-application\terraform"
terraform init
terraform validate
```

Os arquivos de configuração são:

- AWS Student: `profiles\lab.tfvars` (crie a partir de `profiles\lab.tfvars.example`);
- Conta pessoal: `profiles\personal.tfvars`.

Não coloque senhas, tokens ou outras credenciais no Git.

### Opção 1 — AWS Student / Learner Lab

1. Abra o AWS Academy, entre no laboratório e acesse **AWS Details >
   Show**.
2. Copie as credenciais temporárias exibidas. Elas precisam ser renovadas
   quando expirarem.
3. No PowerShell, remova o perfil pessoal e carregue as credenciais:

```powershell
cd "C:\Users\guilherme\Documents\4-SEMESTRE\projeto para aws\jaf-application\terraform"

Remove-Item Env:AWS_PROFILE -ErrorAction SilentlyContinue
$env:AWS_ACCESS_KEY_ID = "SUA_ACCESS_KEY"
$env:AWS_SECRET_ACCESS_KEY = "SUA_SECRET_KEY"
$env:AWS_SESSION_TOKEN = "SEU_SESSION_TOKEN"

aws sts get-caller-identity
terraform workspace select default
terraform workspace show
```

O workspace esperado é `default`. Se o arquivo ainda não existir:

```powershell
Copy-Item "profiles\lab.tfvars.example" "profiles\lab.tfvars"
```

Depois, execute:

```powershell
terraform plan -var-file="profiles\lab.tfvars"
terraform apply -var-file="profiles\lab.tfvars"
```

No perfil Student, `use_lab_role = true` usa a `LabRole` fornecida pelo
Learner Lab. O perfil sobe 6 instâncias EC2 (limite do laboratório): 1 frontend,
2 backend (`t3.small`), 2 workers OCR e 1 RabbitMQ, além de RDS Multi-AZ e NAT
Gateway por AZ. O `plan` falha se a soma dos `max_size` dos ASGs mais o RabbitMQ
passar de `max_instancias_ec2`.

### Opção 2 — Conta AWS pessoal

Configure o perfil AWS CLI uma única vez:

```powershell
aws configure --profile pessoal
```

Depois, execute:

```powershell
cd "C:\Users\guilherme\Documents\4-SEMESTRE\projeto para aws\jaf-application\terraform"

# Remove credenciais temporárias do Student da sessão atual
Remove-Item Env:AWS_ACCESS_KEY_ID -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SECRET_ACCESS_KEY -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SESSION_TOKEN -ErrorAction SilentlyContinue

$env:AWS_PROFILE = "pessoal"

aws sts get-caller-identity --profile pessoal
terraform workspace select personal
terraform workspace show
```

O workspace esperado é `personal`. Em seguida:

```powershell
terraform plan -var-file="profiles\personal.tfvars"
terraform apply -var-file="profiles\personal.tfvars"
```

No perfil pessoal, `use_lab_role = false` faz o Terraform criar a role
necessária para as instâncias. Esse perfil também sobe 6 instâncias EC2
(1 frontend, 2 backend, 2 workers OCR e 1 RabbitMQ), com RDS Single-AZ e um único
NAT Gateway para reduzir custo.

### Conferência obrigatória antes do `apply`

Execute estes comandos e confirme:

```powershell
aws sts get-caller-identity
terraform workspace show
```

| Ambiente | Conta/credencial | Workspace | Arquivo |
| :--- | :--- | :--- | :--- |
| AWS Student | Credenciais temporárias do AWS Academy | `default` | `profiles\lab.tfvars` |
| Conta pessoal | Perfil AWS CLI `pessoal` | `personal` | `profiles\personal.tfvars` |

Nunca use `profiles\personal.tfvars` no workspace `default` e nunca use
`profiles\lab.tfvars` no workspace `personal`. O campo
`allowed_account_ids` do perfil pessoal também deve conter o Account ID correto
da conta pessoal.

### Voltar da conta pessoal para o AWS Student

Quando terminar o uso da conta pessoal, remova o perfil da sessão e carregue
novamente credenciais temporárias válidas do AWS Academy:

```powershell
Remove-Item Env:AWS_PROFILE -ErrorAction SilentlyContinue
$env:AWS_ACCESS_KEY_ID = "SUA_ACCESS_KEY"
$env:AWS_SECRET_ACCESS_KEY = "SUA_SECRET_KEY"
$env:AWS_SESSION_TOKEN = "SEU_SESSION_TOKEN"

terraform workspace select default
terraform plan -var-file="profiles\lab.tfvars"
terraform apply -var-file="profiles\lab.tfvars"
```

Não execute `terraform destroy` sem confirmar antes a conta e o workspace:

```powershell
aws sts get-caller-identity
terraform workspace show
```

---

## Outputs apos o apply

Para exibir novamente: `terraform output`

---

## Acesso SSH

### Frontend (direto)
```bash
ssh -i ~/.ssh/id_rsa ec2-user@FRONTEND_PUBLIC_IP
```

### Backend via jump host
```bash
ssh -J ec2-user@FRONTEND_PUBLIC_IP ec2-user@BACKEND_PRIVATE_IP
```

### OCR via jump host
```bash
ssh -J ec2-user@FRONTEND_PUBLIC_IP ec2-user@OCR_PRIVATE_IP
```

---

## Docker Compose por instancia

### EC2 Frontend

```yaml
version: "3.9"
services:
  frontend:
    image: seu-registry/jaf-frontend:latest
    ports:
      - "80:3000"
    environment:
      REACT_APP_API_URL: "http://BACKEND_PRIVATE_IP:8080"
    restart: unless-stopped
```

### EC2 Backend

```yaml
version: "3.9"
services:
  backend:
    image: seu-registry/jaf-backend:latest
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: "jdbc:postgresql://RDS_HOST:5432/appdb"
      SPRING_DATASOURCE_USERNAME: "appuser"
      SPRING_DATASOURCE_PASSWORD: "MinhaSenh@Forte123!"
      OCR_SERVICE_URL: "http://OCR_PRIVATE_IP:8081"
      SPRING_PROFILES_ACTIVE: "producao"
    restart: unless-stopped
```

### EC2 OCR

```yaml
version: "3.9"
services:
  ocr:
    image: seu-registry/jaf-ocr:latest
    ports:
      - "8081:8081"
    environment:
      OCR_API_KEY: "sua-chave-ocrspace"
    restart: unless-stopped
```

---

## Deploy em cada instancia

```bash
ssh -i ~/.ssh/id_rsa ec2-user@IP_INSTANCIA
cloud-init status --wait
docker --version && docker-compose --version
nano ~/docker-compose.yml
docker-compose up -d
docker-compose logs -f
```

---

## Verificar conectividade

```bash
# Frontend -> Backend
curl http://BACKEND_PRIVATE_IP:8080/actuator/health

# Backend -> RDS
sudo dnf install -y postgresql15
psql -h RDS_HOST -U appuser -d appdb -c "SELECT version();"

# Backend -> OCR
curl http://OCR_PRIVATE_IP:8081/health
```

---

## 11. Destruir infraestrutura

```bash
cd terraform/
terraform destroy
```

---