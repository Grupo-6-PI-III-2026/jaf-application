# ============================================================
# modules/security_groups/main.tf — Security Groups por camada
# ============================================================

# ── SG ALB ────────────────────────────────────────────────────
# Application Load Balancer - aceita tráfego da internet
resource "aws_security_group" "alb" {
  name        = "${var.project_name}-alb-sg"
  description = "Security Group do Application Load Balancer"
  vpc_id      = var.vpc_id

  # HTTP público
  ingress {
    description = "HTTP público"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # HTTPS público (preparado para futuro)
  ingress {
    description = "HTTPS público"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Saída irrestrita
  egress {
    description = "Saida irrestrita"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "sg-alb-${var.project_name}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

# ── SG Frontend ───────────────────────────────────────────────
# Acesso HTTP, HTTPS e SSH direto da internet e do ALB
resource "aws_security_group" "frontend" {
  name        = "${var.project_name}-frontend-sg"
  description = "Security Group do Frontend React - acesso publico HTTP/HTTPS/SSH"
  vpc_id      = var.vpc_id

  # HTTP — acesso público ao frontend (ALB)
  ingress {
    description = "HTTP publico"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # HTTPS — preparado para uso futuro com certificado
  ingress {
    description = "HTTPS publico"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # SSH — método padrão de acesso no Learner Lab (sem SSM)
  ingress {
    description = "SSH para administracao via Learner Lab"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Saída irrestrita — necessário para pull de imagens Docker, updates etc.
  egress {
    description = "Saida irrestrita"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "sg-frontend-${var.project_name}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

# ── SG Backend ────────────────────────────────────────────────
# Recebe tráfego apenas do Frontend (Spring Boot na porta 8080)
resource "aws_security_group" "backend" {
  name        = "${var.project_name}-backend-sg"
  description = "Security Group do Backend Spring Boot - acesso restrito ao Frontend"
  vpc_id      = var.vpc_id

  # Porta da API Spring Boot — permite acesso do Frontend e do ALB
  ingress {
    description     = "API Spring Boot vinda do Frontend"
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.frontend.id]
  }

  # Health check do ALB (porta 8080)
  ingress {
    description     = "Health check do ALB"
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  # SSH via jump host — apenas o frontend pode iniciar a sessão
  ingress {
    description     = "SSH via jump host Frontend"
    from_port       = 22
    to_port         = 22
    protocol        = "tcp"
    security_groups = [aws_security_group.frontend.id]
  }

  egress {
    description = "Saida irrestrita"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "sg-backend-${var.project_name}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

# ── SG OCR ────────────────────────────────────────────────────
# Microsserviço OCR — acessível apenas pelo Backend
resource "aws_security_group" "ocr" {
  name        = "${var.project_name}-ocr-sg"
  description = "Security Group do Microsservico OCR - acesso restrito ao Backend"
  vpc_id      = var.vpc_id

  # Porta do serviço OCR — ajustar se a aplicação usar porta diferente
  ingress {
    description     = "API OCR vinda do Backend"
    from_port       = 8081
    to_port         = 8081
    protocol        = "tcp"
    security_groups = [aws_security_group.backend.id]
  }

  # SSH via jump host — apenas o frontend pode iniciar a sessão
  ingress {
    description     = "SSH via jump host Frontend"
    from_port       = 22
    to_port         = 22
    protocol        = "tcp"
    security_groups = [aws_security_group.frontend.id]
  }

  egress {
    description = "Saida irrestrita"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "sg-ocr-${var.project_name}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

# ── SG RDS ────────────────────────────────────────────────────
# PostgreSQL — acessível apenas pelo Backend
resource "aws_security_group" "rds" {
  name        = "${var.project_name}-rds-sg"
  description = "Security Group do RDS PostgreSQL - acesso restrito ao Backend"
  vpc_id      = var.vpc_id

  # PostgreSQL porta padrão — somente o backend pode conectar
  ingress {
    description     = "PostgreSQL vindo do Backend"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.backend.id]
  }

  egress {
    description = "Saida irrestrita"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "sg-rds-${var.project_name}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}
