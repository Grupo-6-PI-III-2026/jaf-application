# ============================================================
# modules/alb/variables.tf
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
  description = "ID da VPC onde o ALB será criado"
  type        = string
}

variable "public_subnet_ids" {
  description = "Lista de IDs das subnets públicas para o ALB"
  type        = list(string)
}

variable "private_backend_subnet_ids" {
  description = "Subnets privadas do ALB interno do Backend"
  type        = list(string)
}

variable "alb_security_group_id" {
  description = "ID do Security Group do ALB (do módulo security_groups)"
  type        = string
}

variable "acm_certificate_arn" {
  description = "ARN do certificado ACM validado"
  type        = string
  default     = null
  nullable    = true
}

variable "enable_https" {
  description = "Habilita o listener HTTPS do ALB"
  type        = bool
  default     = false
}

variable "backend_internal_security_group_id" {
  description = "Security Group do ALB interno do Backend"
  type        = string
}
