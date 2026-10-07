# ============================================================
# modules/vpc/variables.tf
# ============================================================

variable "project_name" {
  description = "Nome do projeto"
  type        = string
}

variable "environment" {
  description = "Ambiente de execução"
  type        = string
}

variable "vpc_cidr" {
  description = "Bloco CIDR da VPC"
  type        = string
}

variable "public_subnet_cidr" {
  description = "CIDR da subnet pública"
  type        = string
}

variable "private_subnet_backend_cidr" {
  description = "CIDR da subnet privada do Backend"
  type        = string
}

variable "private_subnet_ocr_cidr" {
  description = "CIDR da subnet privada do OCR"
  type        = string
}

variable "private_subnet_rds_cidr" {
  description = "CIDR da subnet privada do RDS"
  type        = string
}

variable "availability_zone" {
  description = "Availability Zone dos recursos"
  type        = string
}

variable "secondary_availability_zone" {
  description = "Availability Zone secundaria para RDS"
  type        = string
}
