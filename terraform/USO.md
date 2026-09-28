
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

```bash
cd terraform/
terraform init
terraform validate
terraform plan
terraform apply
```

Confirme digitando `yes` quando solicitado.

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