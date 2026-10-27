output "rds_endpoint" {
  description = "Endpoint de conexão do RDS PostgreSQL"
  value       = module.rds.rds_endpoint
}

output "rds_port" {
  description = "Porta do RDS PostgreSQL"
  value       = module.rds.rds_port
}

output "frontend_asg_name" {
  description = "Auto Scaling Group do frontend"
  value       = module.ec2.frontend_asg_name
}

output "backend_asg_name" {
  description = "Auto Scaling Group do backend"
  value       = module.ec2.backend_asg_name
}

output "backend_internal_dns_name" {
  description = "DNS privado do ALB interno do Backend"
  value       = module.alb.backend_internal_dns_name
}

output "ocr_asg_name" {
  description = "Auto Scaling Group do OCR"
  value       = module.ec2.ocr_asg_name
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
  description = "URL do ALB conforme o protocolo habilitado"
  value       = "${var.enable_https ? "https" : "http"}://${module.alb.alb_dns_name}"
}

output "eic_endpoint_id" {
  description = "ID do endpoint para conexão às instâncias privadas"
  value       = module.vpc.eic_endpoint_id
}
