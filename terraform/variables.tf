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

# ── EC2 ──────────────────────────────────────────────────────
variable "instance_type" {
  description = "Tipo de instância EC2 (limitado pelo Learner Lab)"
  type        = string
  default     = "t3.micro"
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
  sensitive   = true  # Terraform ocultará este valor nos logs e outputs
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

# ── GHCR (GitHub Container Registry) ─────────────────────────
# Usado pelo user_data das EC2 para fazer pull das imagens Docker

variable "ghcr_token" {
  description = "Personal Access Token do GitHub com permissão read:packages — NUNCA commitar"
  type        = string
  sensitive   = true  # Oculto nos logs e outputs do Terraform
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
  sensitive   = true  # Contém host e nome do banco — não expor
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
