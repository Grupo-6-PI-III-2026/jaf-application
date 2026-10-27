# ============================================================
# modules/ec2/variables.tf
# ============================================================

variable "project_name" {
  description = "Nome do projeto"
  type        = string
}

variable "environment" {
  description = "Ambiente de execução"
  type        = string
}

variable "instance_type" {
  description = "Tipo de instância EC2"
  type        = string
}

variable "ami_id" {
  description = "ID da AMI a usar nas instâncias (Amazon Linux 2023)"
  type        = string
}

variable "public_key_path" {
  description = "Caminho para o arquivo da chave pública SSH"
  type        = string
}

# ── IAM ──────────────────────────────────────────────────────
variable "use_lab_role" {
  description = "true usa LabInstanceProfile (Learner Lab); false cria role e instance profile proprios"
  type        = bool
  default     = true
}

variable "root_volume_size" {
  description = "Tamanho do disco raiz (GB)"
  type        = number
  default     = 30
}

variable "frontend_asg" {
  description = "Capacidade do ASG do frontend"
  type = object({
    min_size         = number
    desired_capacity = number
    max_size         = number
  })
}

variable "backend_asg" {
  description = "Capacidade do ASG do backend"
  type = object({
    min_size         = number
    desired_capacity = number
    max_size         = number
  })
}

variable "ocr_asg" {
  description = "Capacidade do ASG do OCR"
  type = object({
    min_size         = number
    desired_capacity = number
    max_size         = number
  })
}

# ── Subnets ───────────────────────────────────────────────────
variable "public_subnet_ids" {
  description = "IDs das subnets públicas em duas AZs"
  type        = list(string)
}

variable "private_subnet_backend_ids" {
  description = "IDs das subnets privadas do Backend em duas AZs"
  type        = list(string)
}

variable "private_subnet_ocr_ids" {
  description = "IDs das subnets privadas do OCR em duas AZs"
  type        = list(string)
}

variable "frontend_target_group_arn" {
  type = string
}

variable "backend_target_group_arn" {
  type = string
}

variable "backend_internal_dns_name" {
  description = "DNS do ALB interno do Backend usado pelo Nginx"
  type        = string
}

variable "backend_public_access" {
  description = "Coloca temporariamente o backend em subnets publicas e associa IP publico"
  type        = bool
}

# ── Security Groups ───────────────────────────────────────────
variable "sg_frontend_id" {
  description = "ID do Security Group do Frontend"
  type        = string
}

variable "sg_backend_id" {
  description = "ID do Security Group do Backend"
  type        = string
}

variable "sg_ocr_id" {
  description = "ID do Security Group do OCR"
  type        = string
}

# ── GHCR (GitHub Container Registry) ─────────────────────────
variable "ghcr_token" {
  description = "Personal Access Token do GitHub com permissão read:packages"
  type        = string
  sensitive   = true
}

variable "ghcr_user" {
  description = "Username/owner no ghcr.io"
  type        = string
}

variable "repo_owner" {
  description = "Owner do repositório GitHub"
  type        = string
}

# ── Variáveis de ambiente do Backend ─────────────────────────
variable "db_url" {
  description = "JDBC URL do banco PostgreSQL"
  type        = string
  sensitive   = true
}

variable "db_username" {
  description = "Usuário do banco de dados"
  type        = string
}

variable "db_password" {
  description = "Senha do banco de dados"
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "Segredo JWT para autenticação"
  type        = string
  sensitive   = true
}

variable "jwt_expiration" {
  description = "Tempo de expiração do JWT (em milissegundos)"
  type        = string
}
