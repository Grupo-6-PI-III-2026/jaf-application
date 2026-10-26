resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name        = "vpc-${var.project_name}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

locals {
  public_subnets = {
    a = {
      cidr = var.public_subnet_cidr
      az   = var.availability_zone
    }
    b = {
      cidr = var.public_subnet_secondary_cidr
      az   = var.secondary_availability_zone
    }
  }
  backend_subnets = {
    a = {
      cidr = var.private_subnet_backend_cidr
      az   = var.availability_zone
    }
    b = {
      cidr = var.private_subnet_backend_secondary_cidr
      az   = var.secondary_availability_zone
    }
  }
  ocr_subnets = {
    a = {
      cidr = var.private_subnet_ocr_cidr
      az   = var.availability_zone
    }
    b = {
      cidr = var.private_subnet_ocr_secondary_cidr
      az   = var.secondary_availability_zone
    }
  }
  rds_subnets = {
    a = {
      cidr = var.private_subnet_rds_secondary_cidr
      az   = var.availability_zone
    }
    b = {
      cidr = var.private_subnet_rds_cidr
      az   = var.secondary_availability_zone
    }
  }

  # Com single_nat_gateway, apenas a chave "a" recebe NAT/EIP e todas as rotas privadas apontam para ele.
  nat_subnets = { for k, v in local.public_subnets : k => v if !var.single_nat_gateway || k == "a" }
}

moved {
  from = aws_subnet.public
  to   = aws_subnet.public["a"]
}

moved {
  from = aws_subnet.private_backend
  to   = aws_subnet.private_backend["a"]
}

moved {
  from = aws_subnet.private_ocr
  to   = aws_subnet.private_ocr["a"]
}

moved {
  from = aws_subnet.private_rds
  to   = aws_subnet.private_rds["b"]
}

moved {
  from = aws_eip.nat
  to   = aws_eip.nat["a"]
}

moved {
  from = aws_nat_gateway.main
  to   = aws_nat_gateway.main["a"]
}

moved {
  from = aws_route_table.public
  to   = aws_route_table.public["a"]
}

moved {
  from = aws_route_table_association.public
  to   = aws_route_table_association.public["a"]
}

moved {
  from = aws_route_table.private
  to   = aws_route_table.private["a"]
}

moved {
  from = aws_route_table_association.private_backend
  to   = aws_route_table_association.private_backend["a"]
}

moved {
  from = aws_route_table_association.private_ocr
  to   = aws_route_table_association.private_ocr["a"]
}

moved {
  from = aws_route_table_association.private_rds
  to   = aws_route_table_association.private_rds["b"]
}

resource "aws_subnet" "public" {
  for_each                = local.public_subnets
  vpc_id                  = aws_vpc.main.id
  cidr_block              = each.value.cidr
  availability_zone       = each.value.az
  map_public_ip_on_launch = true

  tags = {
    Name        = "subnet-public-${each.key}-${var.project_name}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

resource "aws_subnet" "private_backend" {
  for_each          = local.backend_subnets
  vpc_id            = aws_vpc.main.id
  cidr_block        = each.value.cidr
  availability_zone = each.value.az

  tags = {
    Name        = "subnet-private-backend-${each.key}-${var.project_name}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

resource "aws_subnet" "private_ocr" {
  for_each          = local.ocr_subnets
  vpc_id            = aws_vpc.main.id
  cidr_block        = each.value.cidr
  availability_zone = each.value.az

  tags = {
    Name        = "subnet-private-ocr-${each.key}-${var.project_name}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

resource "aws_subnet" "private_rds" {
  for_each          = local.rds_subnets
  vpc_id            = aws_vpc.main.id
  cidr_block        = each.value.cidr
  availability_zone = each.value.az

  tags = {
    Name        = "subnet-private-rds-${each.key}-${var.project_name}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name        = "igw-${var.project_name}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

resource "aws_eip" "nat" {
  for_each   = local.nat_subnets
  domain     = "vpc"
  depends_on = [aws_internet_gateway.main]
}

resource "aws_nat_gateway" "main" {
  for_each      = local.nat_subnets
  allocation_id = aws_eip.nat[each.key].id
  subnet_id     = aws_subnet.public[each.key].id
  depends_on    = [aws_internet_gateway.main]

  tags = {
    Name        = "nat-gw-${each.key}-${var.project_name}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

resource "aws_route_table" "public" {
  for_each = local.public_subnets
  vpc_id   = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = {
    Name = "rt-public-${each.key}-${var.project_name}"
  }
}

resource "aws_route_table_association" "public" {
  for_each       = local.public_subnets
  subnet_id      = aws_subnet.public[each.key].id
  route_table_id = aws_route_table.public[each.key].id
}

resource "aws_route_table" "private" {
  for_each = local.public_subnets
  vpc_id   = aws_vpc.main.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.main[var.single_nat_gateway ? "a" : each.key].id
  }

  tags = {
    Name = "rt-private-${each.key}-${var.project_name}"
  }
}

resource "aws_route_table_association" "private_backend" {
  for_each       = local.backend_subnets
  subnet_id      = aws_subnet.private_backend[each.key].id
  route_table_id = aws_route_table.private[each.key].id
}

resource "aws_route_table_association" "private_ocr" {
  for_each       = local.ocr_subnets
  subnet_id      = aws_subnet.private_ocr[each.key].id
  route_table_id = aws_route_table.private[each.key].id
}

resource "aws_route_table_association" "private_rds" {
  for_each       = local.rds_subnets
  subnet_id      = aws_subnet.private_rds[each.key].id
  route_table_id = aws_route_table.private[each.key].id
}

resource "aws_security_group" "eic_endpoint" {
  name        = "${var.project_name}-eic-endpoint-sg"
  description = "Security Group do endpoint EC2 Instance Connect"
  vpc_id      = aws_vpc.main.id

  egress {
    description = "Saida para instancias privadas"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_ec2_instance_connect_endpoint" "main" {
  subnet_id          = aws_subnet.private_backend["a"].id
  security_group_ids = [aws_security_group.eic_endpoint.id]

  tags = {
    Name        = "eic-endpoint-${var.project_name}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}
