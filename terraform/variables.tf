# ============================================================
# variables.tf — Variáveis globais do projeto
# ============================================================

# ── Identificação do projeto ─────────────────────────────────
variable "project_name" {
  description = "Nome do projeto, usado como prefixo nos recursos"
  type        = string
}

variable "environment" {
  description = "Ambiente de execução (ex: producao, staging, dev)"
  type        = string
}

# ── Região AWS ───────────────────────────────────────────────
variable "aws_region" {
  description = "Região AWS onde os recursos serão criados"
  type        = string
  default     = "us-east-1"
}

# ── Rede ─────────────────────────────────────────────────────
variable "vpc_cidr" {
  description = "Bloco CIDR da VPC principal"
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidr" {
  description = "CIDR da subnet pública (Frontend)"
  type        = string
  default     = "10.0.1.0/24"
}

variable "private_subnet_backend_cidr" {
  description = "CIDR da subnet privada do Backend (Spring Boot)"
  type        = string
  default     = "10.0.2.0/24"
}

variable "private_subnet_ocr_cidr" {
  description = "CIDR da subnet privada do microsserviço OCR"
  type        = string
  default     = "10.0.3.0/24"
}

variable "private_subnet_rds_cidr" {
  description = "CIDR da subnet privada do banco de dados (RDS)"
  type        = string
  default     = "10.0.4.0/24"
}

variable "public_subnet_secondary_cidr" {
  description = "CIDR da segunda subnet pública"
  type        = string
  default     = "10.0.5.0/24"
}

variable "private_subnet_backend_secondary_cidr" {
  description = "CIDR da segunda subnet privada do Backend"
  type        = string
  default     = "10.0.6.0/24"
}

variable "private_subnet_ocr_secondary_cidr" {
  description = "CIDR da segunda subnet privada do OCR"
  type        = string
  default     = "10.0.7.0/24"
}

variable "private_subnet_rds_secondary_cidr" {
  description = "CIDR da segunda subnet privada do RDS"
  type        = string
  default     = "10.0.8.0/24"
}

variable "availability_zone" {
  description = "Availability Zone principal dos recursos"
  type        = string
  default     = "us-east-1a"
}

variable "secondary_availability_zone" {
  description = "Availability Zone secundaria (exigido pelo RDS)"
  type        = string
  default     = "us-east-1b"
}

variable "acm_certificate_arn" {
  description = "ARN de um certificado ACM validado para o domínio público"
  type        = string
  default     = null
  nullable    = true
}

variable "enable_https" {
  description = "Habilita HTTPS no ALB quando houver certificado ACM válido"
  type        = bool
  default     = false
}

variable "backend_public_access" {
  description = "Modo temporario de diagnostico: coloca o backend em subnets publicas com IP publico e SSH/8080 abertos ao mundo. Mantenha false no uso normal"
  type        = bool
  default     = false
}

variable "frontend_ssh_cidrs" {
  description = "CIDRs com acesso SSH (22) ao frontend. Padrao aberto (Learner Lab). Use [] para depender so do EIC Endpoint/SSM, ou o range EC2_INSTANCE_CONNECT da regiao para o EIC pelo console"
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "single_nat_gateway" {
  description = "true usa um unico NAT Gateway (AZ principal) para todas as subnets privadas; false usa um NAT por AZ"
  type        = bool
  default     = false
}

# ── Conta AWS / perfil ───────────────────────────────────────
variable "use_lab_role" {
  description = "true usa a LabRole/LabInstanceProfile do Learner Lab; false cria uma role e instance profile proprios (conta AWS normal)"
  type        = bool
  default     = true
}

variable "allowed_account_ids" {
  description = "Contas AWS permitidas para este state. Lista vazia desativa a trava. Evita apply na conta errada"
  type        = list(string)
  default     = []
}

# ── EC2 ──────────────────────────────────────────────────────
variable "instance_type" {
  description = "Tipo de instância EC2 (limitado pelo Learner Lab)"
  type        = string
  default     = "t3.micro"
}

variable "root_volume_size" {
  description = "Tamanho do disco raiz (GB) das instâncias EC2"
  type        = number
  default     = 30

  validation {
    condition     = var.root_volume_size >= 8
    error_message = "root_volume_size deve ser >= 8 (tamanho minimo da AMI)."
  }
}

variable "frontend_asg" {
  description = "Capacidade do Auto Scaling Group do frontend"
  type = object({
    min_size         = number
    desired_capacity = number
    max_size         = number
  })
  default = {
    min_size         = 2
    desired_capacity = 2
    max_size         = 4
  }

  validation {
    condition     = var.frontend_asg.min_size <= var.frontend_asg.desired_capacity && var.frontend_asg.desired_capacity <= var.frontend_asg.max_size
    error_message = "frontend_asg exige min_size <= desired_capacity <= max_size."
  }
}

variable "backend_asg" {
  description = "Capacidade do Auto Scaling Group do backend"
  type = object({
    min_size         = number
    desired_capacity = number
    max_size         = number
  })
  default = {
    min_size         = 2
    desired_capacity = 2
    max_size         = 4
  }

  validation {
    condition     = var.backend_asg.min_size <= var.backend_asg.desired_capacity && var.backend_asg.desired_capacity <= var.backend_asg.max_size
    error_message = "backend_asg exige min_size <= desired_capacity <= max_size."
  }
}

variable "ocr_asg" {
  description = "Capacidade do Auto Scaling Group do OCR (0/0/0 desliga o OCR)"
  type = object({
    min_size         = number
    desired_capacity = number
    max_size         = number
  })
  default = {
    min_size         = 2
    desired_capacity = 2
    max_size         = 4
  }

  validation {
    condition     = var.ocr_asg.min_size <= var.ocr_asg.desired_capacity && var.ocr_asg.desired_capacity <= var.ocr_asg.max_size
    error_message = "ocr_asg exige min_size <= desired_capacity <= max_size."
  }
}

variable "public_key_path" {
  description = "Caminho para a chave pública SSH (ex: ~/.ssh/id_rsa.pub)"
  type        = string
}

# ── RDS ──────────────────────────────────────────────────────
variable "db_name" {
  description = "Nome do banco de dados PostgreSQL"
  type        = string
}

variable "db_username" {
  description = "Usuário administrador do RDS"
  type        = string
}

variable "db_password" {
  description = "Senha do usuário administrador do RDS — NUNCA commitar este valor"
  type        = string
  sensitive   = true # Terraform ocultará este valor nos logs e outputs
}

variable "rds_instance_class" {
  description = "Classe da instância RDS"
  type        = string
  default     = "db.t3.micro"
}

variable "rds_allocated_storage" {
  description = "Tamanho do disco RDS em GB"
  type        = number
  default     = 20
}

variable "rds_multi_az" {
  description = "true cria standby sincrono em outra AZ (failover automatico); false usa Single-AZ (mais barato)"
  type        = bool
  default     = true
}

variable "rds_apply_immediately" {
  description = "true aplica mudancas do RDS (ex.: Multi-AZ) na hora; false aguarda a janela de manutencao"
  type        = bool
  default     = false
}

# ── GHCR (GitHub Container Registry) ─────────────────────────
# Usado pelo user_data das EC2 para fazer pull das imagens Docker

variable "ghcr_token" {
  description = "Personal Access Token do GitHub com permissão read:packages — NUNCA commitar"
  type        = string
  sensitive   = true # Oculto nos logs e outputs do Terraform
}

variable "ghcr_user" {
  description = "Username ou organization owner no ghcr.io (ex: Grupo-6-PI-III-2026)"
  type        = string
}

variable "repo_owner" {
  description = "Owner do repositório GitHub (mesmo valor que ghcr_user na maioria dos casos)"
  type        = string
}

# ── Variáveis de ambiente do Backend ─────────────────────────
# Passadas via templatefile para o user_data da instância backend

variable "db_url" {
  description = "JDBC URL do banco PostgreSQL (ex: jdbc:postgresql://host:5432/appdb)"
  type        = string
  sensitive   = true # Contém host e nome do banco — não expor
}

variable "jwt_secret" {
  description = "Segredo usado para assinar e validar tokens JWT"
  type        = string
  sensitive   = true
}

variable "jwt_expiration" {
  description = "Tempo de expiração do JWT (em milissegundos)"
  type        = string
  default     = "86400000" # 24 horas por padrão, se não for passado
}
