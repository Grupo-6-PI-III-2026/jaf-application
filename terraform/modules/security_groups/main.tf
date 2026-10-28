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
    description = "HTTP publico"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # HTTPS público (preparado para futuro)
  ingress {
    description = "HTTPS publico"
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

resource "aws_security_group" "backend_alb" {
  name        = "${var.project_name}-backend-alb-sg"
  description = "Security Group do ALB interno do Backend"
  vpc_id      = var.vpc_id

  ingress {
    description     = "Backend interno vindo do Frontend"
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.frontend.id]
  }

  egress {
    description = "Saida para os backends"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
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

  # SSH — navegador (EC2 Instance Connect) e SSH direto; restrinja via var.frontend_ssh_cidrs
  dynamic "ingress" {
    for_each = length(var.frontend_ssh_cidrs) > 0 ? [1] : []

    content {
      description = "SSH para administracao via Learner Lab"
      from_port   = 22
      to_port     = 22
      protocol    = "tcp"
      cidr_blocks = var.frontend_ssh_cidrs
    }
  }

  # SSH via EC2 Instance Connect Endpoint (sem depender de IP publico ou CIDR)
  ingress {
    description     = "SSH via EC2 Instance Connect Endpoint"
    from_port       = 22
    to_port         = 22
    protocol        = "tcp"
    security_groups = [var.eic_security_group_id]
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

  ingress {
    description     = "Health check do ALB interno"
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.backend_alb.id]
  }

  ingress {
    description     = "SSH via EC2 Instance Connect Endpoint"
    from_port       = 22
    to_port         = 22
    protocol        = "tcp"
    security_groups = [var.eic_security_group_id]
  }

  dynamic "ingress" {
    for_each = var.backend_public_access ? [1] : []

    content {
      description = "SSH publico temporario de laboratorio"
      from_port   = 22
      to_port     = 22
      protocol    = "tcp"
      cidr_blocks = ["0.0.0.0/0"]
    }
  }

  dynamic "ingress" {
    for_each = var.backend_public_access ? [1] : []

    content {
      description = "API publica temporaria de laboratorio"
      from_port   = 8080
      to_port     = 8080
      protocol    = "tcp"
      cidr_blocks = ["0.0.0.0/0"]
    }
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
# Workers OCR só consomem a fila (sem porta de entrada de aplicação)
resource "aws_security_group" "ocr" {
  name        = "${var.project_name}-ocr-sg"
  description = "Security Group do Microsservico OCR - acesso restrito ao Backend"
  vpc_id      = var.vpc_id

  # SSH via jump host — apenas o frontend pode iniciar a sessão
  ingress {
    description     = "SSH via jump host Frontend"
    from_port       = 22
    to_port         = 22
    protocol        = "tcp"
    security_groups = [aws_security_group.frontend.id]
  }

  ingress {
    description     = "SSH via EC2 Instance Connect Endpoint"
    from_port       = 22
    to_port         = 22
    protocol        = "tcp"
    security_groups = [var.eic_security_group_id]
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

# ── SG RabbitMQ ───────────────────────────────────────────────
# Broker AMQP — apenas backend (publica/recebe resultados) e workers OCR
resource "aws_security_group" "rabbitmq" {
  name        = "${var.project_name}-rabbitmq-sg"
  description = "Security Group do RabbitMQ - acesso restrito ao Backend e ao OCR"
  vpc_id      = var.vpc_id

  ingress {
    description     = "AMQP vindo do Backend"
    from_port       = 5672
    to_port         = 5672
    protocol        = "tcp"
    security_groups = [aws_security_group.backend.id]
  }

  ingress {
    description     = "AMQP vindo dos workers OCR"
    from_port       = 5672
    to_port         = 5672
    protocol        = "tcp"
    security_groups = [aws_security_group.ocr.id]
  }

  ingress {
    description     = "SSH via EC2 Instance Connect Endpoint"
    from_port       = 22
    to_port         = 22
    protocol        = "tcp"
    security_groups = [var.eic_security_group_id]
  }

  egress {
    description = "Saida irrestrita"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "sg-rabbitmq-${var.project_name}"
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
