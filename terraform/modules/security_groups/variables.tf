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

variable "eic_security_group_id" {
  description = "Security Group do endpoint EC2 Instance Connect"
  type        = string
}

variable "backend_public_access" {
  description = "Libera SSH publico temporariamente no backend"
  type        = bool
}

variable "frontend_ssh_cidrs" {
  description = "CIDRs com acesso SSH (porta 22) ao frontend; lista vazia desativa a regra por CIDR"
  type        = list(string)
  default     = ["0.0.0.0/0"]
}
