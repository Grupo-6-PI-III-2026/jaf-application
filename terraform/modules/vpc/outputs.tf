# ============================================================
# modules/vpc/outputs.tf — Exporta IDs para outros módulos
# ============================================================

output "vpc_id" {
  description = "ID da VPC criada"
  value       = aws_vpc.main.id
}

output "public_subnet_id" {
  description = "ID da subnet pública (Frontend)"
  value       = aws_subnet.public["a"].id
}

output "public_subnet_ids" {
  description = "Lista de IDs das subnets públicas (para ALB)"
  value       = values(aws_subnet.public)[*].id
}

output "private_subnet_backend_ids" {
  description = "IDs das subnets privadas do Backend em duas AZs"
  value       = values(aws_subnet.private_backend)[*].id
}

output "private_subnet_ocr_ids" {
  description = "IDs das subnets privadas do OCR em duas AZs"
  value       = values(aws_subnet.private_ocr)[*].id
}

output "private_subnet_rds_ids" {
  description = "IDs das subnets privadas do RDS em duas AZs"
  value       = values(aws_subnet.private_rds)[*].id
}

output "nat_gateway_id" {
  description = "ID do NAT Gateway"
  value       = aws_nat_gateway.main["a"].id
}

output "eic_endpoint_id" {
  description = "ID do endpoint EC2 Instance Connect"
  value       = aws_ec2_instance_connect_endpoint.main.id
}

output "sg_eic_endpoint_id" {
  description = "ID do Security Group do EC2 Instance Connect Endpoint"
  value       = aws_security_group.eic_endpoint.id
}
