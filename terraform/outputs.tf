# ============================================================
# outputs.tf (root) — Outputs essenciais exibidos após o apply
# ============================================================

output "frontend_public_ip" {
  description = "Elastic IP público do frontend React"
  value       = module.ec2.frontend_public_ip
}

output "frontend_public_dns" {
  description = "DNS público do frontend React"
  value       = module.ec2.frontend_public_dns
}

output "backend_private_ip" {
  description = "IP privado da instância do backend Spring Boot"
  value       = module.ec2.backend_private_ip
}

output "ocr_private_ip" {
  description = "IP privado da instância do microsserviço OCR"
  value       = module.ec2.ocr_private_ip
}

output "rds_endpoint" {
  description = "Endpoint de conexão do RDS PostgreSQL"
  value       = module.rds.rds_endpoint
}

output "rds_port" {
  description = "Porta do RDS PostgreSQL"
  value       = module.rds.rds_port
}

output "ssh_command_frontend" {
  description = "Comando SSH pronto para conectar ao frontend"
  value       = "ssh -i ~/.ssh/id_rsa ec2-user@${module.ec2.frontend_public_ip}"
}

output "ssh_command_backend_via_jump" {
  description = "Comando SSH para acessar o backend usando o frontend como jump host"
  value       = "ssh -J ec2-user@${module.ec2.frontend_public_ip} ec2-user@${module.ec2.backend_private_ip}"
}

output "ssh_command_ocr_via_jump" {
  description = "Comando SSH para acessar o OCR usando o frontend como jump host"
  value       = "ssh -J ec2-user@${module.ec2.frontend_public_ip} ec2-user@${module.ec2.ocr_private_ip}"
}

output "ami_id_used" {
  description = "ID da AMI do Amazon Linux 2023 utilizada"
  value       = data.aws_ami.amazon_linux_2023.id
}

output "alb_dns_name" {
  description = "DNS name do Application Load Balancer"
  value       = module.alb.alb_dns_name
}

output "alb_url" {
  description = "URL completa do ALB (http://alb-dns-name)"
  value       = "http://${module.alb.alb_dns_name}"
}
