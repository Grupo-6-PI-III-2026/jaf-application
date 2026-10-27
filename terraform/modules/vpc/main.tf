# ============================================================
# modules/vpc/main.tf — VPC, Subnets, IGW, NAT GW, Route Tables
# ============================================================

# ── VPC Principal ─────────────────────────────────────────────
resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_support   = true   # Necessário para RDS endpoint resolver via DNS
  enable_dns_hostnames = true   # Necessário para EC2 ter DNS público

  tags = {
    Name        = "vpc-${var.project_name}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

# ── Subnet Pública (Frontend React) ───────────────────────────
resource "aws_subnet" "public" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = var.public_subnet_cidr
  availability_zone       = var.availability_zone
  # Instâncias nessa subnet recebem IP público automaticamente
  map_public_ip_on_launch = true

  tags = {
    Name        = "subnet-public-${var.project_name}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

# ── Subnet Privada 1 (Backend Spring Boot) ────────────────────
resource "aws_subnet" "private_backend" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = var.private_subnet_backend_cidr
  availability_zone = var.availability_zone

  tags = {
    Name        = "subnet-private-backend-${var.project_name}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

# ── Subnet Privada 2 (Microsserviço OCR) ─────────────────────
resource "aws_subnet" "private_ocr" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = var.private_subnet_ocr_cidr
  availability_zone = var.availability_zone

  tags = {
    Name        = "subnet-private-ocr-${var.project_name}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

# ── Subnet Privada 3 (RDS PostgreSQL) ────────────────────────
resource "aws_subnet" "private_rds" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = var.private_subnet_rds_cidr
  availability_zone = var.secondary_availability_zone

  tags = {
    Name        = "subnet-private-rds-${var.project_name}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

# ── Internet Gateway ──────────────────────────────────────────
# Permite que a subnet pública se comunique com a internet
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name        = "igw-${var.project_name}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

# ── Elastic IP para o NAT Gateway ────────────────────────────
# O NAT GW precisa de um EIP para ter endereço público fixo
resource "aws_eip" "nat" {
  domain = "vpc"

  # Garantir que o IGW exista antes de criar o EIP
  depends_on = [aws_internet_gateway.main]

  tags = {
    Name        = "eip-nat-${var.project_name}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

# ── NAT Gateway ───────────────────────────────────────────────
# Fica na subnet PÚBLICA e permite que as subnets privadas
# acessem a internet (para pull de imagens Docker, updates etc.)
resource "aws_nat_gateway" "main" {
  allocation_id = aws_eip.nat.id
  subnet_id     = aws_subnet.public.id  # NAT GW deve estar na subnet pública

  depends_on = [aws_internet_gateway.main]

  tags = {
    Name        = "nat-gw-${var.project_name}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

# ── Route Table Pública ───────────────────────────────────────
# Todo tráfego da subnet pública sem destino local → IGW
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = {
    Name        = "rt-public-${var.project_name}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

# Associar Route Table pública à subnet pública
resource "aws_route_table_association" "public" {
  subnet_id      = aws_subnet.public.id
  route_table_id = aws_route_table.public.id
}

# ── Route Table Privada ───────────────────────────────────────
# Todo tráfego das subnets privadas sem destino local → NAT GW
resource "aws_route_table" "private" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.main.id
  }

  tags = {
    Name        = "rt-private-${var.project_name}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

# Associar Route Table privada às 3 subnets privadas
resource "aws_route_table_association" "private_backend" {
  subnet_id      = aws_subnet.private_backend.id
  route_table_id = aws_route_table.private.id
}

resource "aws_route_table_association" "private_ocr" {
  subnet_id      = aws_subnet.private_ocr.id
  route_table_id = aws_route_table.private.id
}

resource "aws_route_table_association" "private_rds" {
  subnet_id      = aws_subnet.private_rds.id
  route_table_id = aws_route_table.private.id
}
