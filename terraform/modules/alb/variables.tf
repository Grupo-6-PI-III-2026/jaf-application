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

variable "alb_security_group_id" {
  description = "ID do Security Group do ALB (do módulo security_groups)"
  type        = string
}

variable "frontend_instance_id" {
  description = "ID da instância EC2 do frontend para anexar ao target group"
  type        = string
}

variable "backend_instance_id" {
  description = "ID da instância EC2 do backend para anexar ao target group"
  type        = string
}
