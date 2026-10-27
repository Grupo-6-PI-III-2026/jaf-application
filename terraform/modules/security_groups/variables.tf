# ============================================================
# modules/security_groups/variables.tf
# ============================================================

variable "project_name" {
  description = "Nome do projeto"
  type        = string
}

variable "environment" {
  description = "Ambiente de execução"
  type        = string
}

variable "vpc_id" {
  description = "ID da VPC onde os Security Groups serão criados"
  type        = string
}
