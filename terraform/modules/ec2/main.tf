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

# ── User Data via templatefile ────────────────────────────────
# Cada instância recebe um script específico com suas variáveis.
# Os scripts ficam em terraform/scripts/ e são processados pelo templatefile.
locals {
  # Frontend: instala Docker, faz login no ghcr.io e sobe o container na porta 80
  user_data_frontend = templatefile("${path.module}/../../scripts/frontend.sh", {
    ghcr_user  = var.ghcr_user
    ghcr_token = var.ghcr_token
    repo_owner = var.repo_owner
  })

  # Backend: instala Docker, faz login no ghcr.io, sobe o container na porta 8080
  # e injeta as variáveis de ambiente do banco de dados
  user_data_backend = templatefile("${path.module}/../../scripts/backend.sh", {
    ghcr_user   = var.ghcr_user
    ghcr_token  = var.ghcr_token
    repo_owner  = var.repo_owner
    db_url      = var.db_url
    db_username = var.db_username
    db_password = var.db_password
  })

  # OCR: script genérico — apenas instala Docker (sem pull de imagem específica)
  user_data_ocr = <<-EOF
    #!/bin/bash
    set -eux
    dnf update -y
    dnf install -y docker
    systemctl enable docker
    systemctl start docker
    usermod -aG docker ec2-user
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

  # Script específico do frontend: Docker + login ghcr.io + pull + run porta 80
  user_data = local.user_data_frontend

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

  # Script específico do backend: Docker + login ghcr.io + pull + run porta 8080 + envs do DB
  user_data = local.user_data_backend

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

  # Script genérico do OCR: apenas instala Docker (sem pull de imagem específica)
  user_data = local.user_data_ocr

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
