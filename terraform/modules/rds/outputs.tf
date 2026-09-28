# ============================================================
# modules/rds/outputs.tf
# ============================================================

output "rds_endpoint" {
  description = "Endpoint completo de conexão ao RDS (host:port)"
  value       = aws_db_instance.main.endpoint
}

output "rds_host" {
  description = "Hostname do RDS (sem porta)"
  value       = aws_db_instance.main.address
}

output "rds_port" {
  description = "Porta do RDS PostgreSQL"
  value       = aws_db_instance.main.port
}

output "rds_db_name" {
  description = "Nome do banco de dados criado"
  value       = aws_db_instance.main.db_name
}

output "rds_instance_id" {
  description = "Identifier da instância RDS"
  value       = aws_db_instance.main.id
}
