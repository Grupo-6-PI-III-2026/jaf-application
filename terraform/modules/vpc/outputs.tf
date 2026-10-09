# ============================================================
# modules/vpc/outputs.tf — Exporta IDs para outros módulos
# ============================================================

output "vpc_id" {
  description = "ID da VPC criada"
  value       = aws_vpc.main.id
}

output "public_subnet_id" {
  description = "ID da subnet pública (Frontend)"
  value       = aws_subnet.public.id
}

output "public_subnet_ids" {
  description = "Lista de IDs das subnets públicas (para ALB)"
  value       = [aws_subnet.public.id]
}

output "private_subnet_backend_id" {
  description = "ID da subnet privada do Backend"
  value       = aws_subnet.private_backend.id
}

output "private_subnet_ocr_id" {
  description = "ID da subnet privada do OCR"
  value       = aws_subnet.private_ocr.id
}

output "private_subnet_rds_id" {
  description = "ID da subnet privada do RDS"
  value       = aws_subnet.private_rds.id
}

output "nat_gateway_id" {
  description = "ID do NAT Gateway"
  value       = aws_nat_gateway.main.id
}
