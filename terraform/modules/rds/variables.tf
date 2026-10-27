# ============================================================
# modules/rds/variables.tf
# ============================================================

variable "project_name" {
  description = "Nome do projeto"
  type        = string
}

variable "environment" {
  description = "Ambiente de execução"
  type        = string
}

variable "db_name" {
  description = "Nome do banco de dados"
  type        = string
}

variable "db_username" {
  description = "Usuário administrador do RDS"
  type        = string
}

variable "db_password" {
  description = "Senha do usuário administrador do RDS"
  type        = string
  sensitive   = true
}

variable "rds_instance_class" {
  description = "Classe da instância RDS"
  type        = string
  default     = "db.t3.micro"
}

variable "rds_allocated_storage" {
  description = "Tamanho do disco do RDS em GB"
  type        = number
  default     = 20
}

variable "sg_rds_id" {
  description = "ID do Security Group do RDS"
  type        = string
}

# ── Subnets ───────────────────────────────────────────────────
variable "private_subnet_backend_id" {
  description = "ID da subnet privada do Backend"
  type        = string
}

variable "private_subnet_ocr_id" {
  description = "ID da subnet privada do OCR"
  type        = string
}

variable "private_subnet_rds_id" {
  description = "ID da subnet privada dedicada ao RDS"
  type        = string
}
