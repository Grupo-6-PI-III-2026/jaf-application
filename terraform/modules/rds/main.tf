# ============================================================
# modules/rds/main.tf — RDS PostgreSQL e DB Subnet Group
# ============================================================

# ── DB Subnet Group ───────────────────────────────────────────
# O RDS exige um subnet group com pelo menos 2 subnets em AZs diferentes.
# Aqui usamos as 3 subnets privadas para cobrir o requisito.
# NOTA: mesmo sendo Single-AZ, a AWS exige o subnet group com múltiplas subnets.
resource "aws_db_subnet_group" "main" {
  name        = "rds-subnet-group-${var.project_name}"
  description = "Subnet group para RDS PostgreSQL - subnets privadas"

  subnet_ids = [
    var.private_subnet_backend_id,
    var.private_subnet_ocr_id,
    var.private_subnet_rds_id,
  ]

  tags = {
    Name        = "rds-subnet-group-${var.project_name}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

# ── RDS PostgreSQL ────────────────────────────────────────────
resource "aws_db_instance" "main" {
  identifier = "rds-${var.project_name}"

  # Engine
  engine         = "postgres"
  engine_version = "15"      # PostgreSQL 15 LTS

  # Instância — usar db.t3.micro para compatibilidade com Learner Lab
  instance_class = var.rds_instance_class

  # Storage
  allocated_storage = var.rds_allocated_storage
  storage_type      = "gp2"

  # Credenciais — nunca hardcoded, vêm de variáveis sensíveis
  db_name  = var.db_name
  username = var.db_username
  password = var.db_password  # sensitive = true definido no variables.tf raiz

  # Rede e segurança
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [var.sg_rds_id]
  publicly_accessible    = false   # Banco não deve ser acessível via internet

  # Alta disponibilidade — Single-AZ no Learner Lab (Multi-AZ requer créditos)
  multi_az = false

  # Backup automático — manter por 7 dias
  backup_retention_period = 7
  backup_window           = "03:00-04:00"   # UTC — janela de backup
  maintenance_window      = "mon:04:00-mon:05:00"  # Manutenção fora do horário de pico

  # Snapshot final — desativado para facilitar destruição no lab
  skip_final_snapshot = true

  # Proteção contra exclusão — desativada para facilitar limpeza no lab
  deletion_protection = false

  # Parâmetros adicionais
  auto_minor_version_upgrade = true
  copy_tags_to_snapshot      = true

  tags = {
    Name        = "rds-${var.project_name}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}
