# ============================================================
# modules/ec2/main.tf — Instâncias EC2, Key Pair, Instance Profile, EIP
# ============================================================

# ── Key Pair ──────────────────────────────────────────────────
# A chave pública é lida do arquivo local e enviada à AWS.
# A chave privada (~/.ssh/id_rsa) fica apenas na máquina local.
resource "aws_key_pair" "main" {
  key_name   = "keypair-${var.project_name}"
  public_key = file(var.public_key_path)

  tags = {
    Name        = "keypair-${var.project_name}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

# ── Instance Profile (LabInstanceProfile nativo do Learner Lab) ──
# RESTRIÇÃO Learner Lab: não criar instance profile novo (dá AccessDenied).
# Reutilizamos o "LabInstanceProfile" pré-existente na conta.
data "aws_iam_instance_profile" "lab_profile" {
  name = "LabInstanceProfile"
}

# ── User Data: Docker + Docker Compose ────────────────────────
# Script executado uma única vez na inicialização de cada instância.
# Instala Docker e Docker Compose no Amazon Linux 2023 (dnf).
locals {
  user_data = <<-EOF
    #!/bin/bash
    set -eux

    # Atualizar pacotes do sistema
    dnf update -y

    # Instalar Docker
    dnf install -y docker

    # Habilitar e iniciar o serviço Docker
    systemctl enable docker
    systemctl start docker

    # Adicionar ec2-user ao grupo docker (sem necessidade de sudo)
    usermod -aG docker ec2-user

    # Instalar Docker Compose v2 (binário standalone)
    curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
      -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose

    # Verificar instalação
    docker --version
    docker-compose --version
  EOF
}

# ── EC2 Frontend ──────────────────────────────────────────────
# Subnet PÚBLICA — recebe Elastic IP para acesso externo
resource "aws_instance" "frontend" {
  ami                    = var.ami_id
  instance_type          = var.instance_type
  subnet_id              = var.public_subnet_id
  vpc_security_group_ids = [var.sg_frontend_id]
  key_name               = aws_key_pair.main.key_name
  iam_instance_profile   = data.aws_iam_instance_profile.lab_profile.name

  # Instalar Docker automaticamente na primeira inicialização
  user_data = local.user_data

  # Volume root com 20GB — adequado para imagens Docker
  root_block_device {
    volume_type           = "gp3"
    volume_size           = 30
    delete_on_termination = true

    tags = {
      Name      = "ebs-frontend-${var.project_name}"
      ManagedBy = "Terraform"
    }
  }

  tags = {
    Name        = "frontend-ec2-${var.project_name}"
    Role        = "frontend"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

# ── Elastic IP do Frontend ────────────────────────────────────
# IP público fixo para o frontend — não muda ao parar/iniciar a instância
resource "aws_eip" "frontend" {
  instance = aws_instance.frontend.id
  domain   = "vpc"

  tags = {
    Name        = "eip-frontend-${var.project_name}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

# ── EC2 Backend (Spring Boot) ─────────────────────────────────
# Subnet PRIVADA — sem IP público, acessível apenas via rede interna
resource "aws_instance" "backend" {
  ami                    = var.ami_id
  instance_type          = var.instance_type
  subnet_id              = var.private_subnet_backend_id
  vpc_security_group_ids = [var.sg_backend_id]
  key_name               = aws_key_pair.main.key_name
  iam_instance_profile   = data.aws_iam_instance_profile.lab_profile.name

  user_data = local.user_data

  root_block_device {
    volume_type           = "gp3"
    volume_size           = 30
    delete_on_termination = true

    tags = {
      Name      = "ebs-backend-${var.project_name}"
      ManagedBy = "Terraform"
    }
  }

  tags = {
    Name        = "backend-ec2-${var.project_name}"
    Role        = "backend"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

# ── EC2 OCR (Microsserviço) ───────────────────────────────────
# Subnet PRIVADA — sem IP público, acessível apenas pelo backend
resource "aws_instance" "ocr" {
  ami                    = var.ami_id
  instance_type          = var.instance_type
  subnet_id              = var.private_subnet_ocr_id
  vpc_security_group_ids = [var.sg_ocr_id]
  key_name               = aws_key_pair.main.key_name
  iam_instance_profile   = data.aws_iam_instance_profile.lab_profile.name

  user_data = local.user_data

  root_block_device {
    volume_type           = "gp3"
    volume_size           = 30
    delete_on_termination = true

    tags = {
      Name      = "ebs-ocr-${var.project_name}"
      ManagedBy = "Terraform"
    }
  }

  tags = {
    Name        = "ocr-ec2-${var.project_name}"
    Role        = "ocr"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}
