# ===========================================================
# main.tf (root) — Instancia todos os módulos
# ===========================================================

# ── Data sources globais ──────────────────────────────────────

# IAM Role pré-existente do Learner Lab — NÃO criar nova role
data "aws_iam_role" "lab_role" {
  name = "LabRole"
}

# AMI mais recente do Amazon Linux 2023 (64-bit x86)
data "aws_ami" "amazon_linux_2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-*-x86_64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }

  filter {
    name   = "architecture"
    values = ["x86_64"]
  }
}

# ── Módulo: VPC ───────────────────────────────────────────────
module "vpc" {
  source = "./modules/vpc"

  project_name                = var.project_name
  environment                 = var.environment
  vpc_cidr                    = var.vpc_cidr
  public_subnet_cidr          = var.public_subnet_cidr
  private_subnet_backend_cidr = var.private_subnet_backend_cidr
  private_subnet_ocr_cidr     = var.private_subnet_ocr_cidr
  private_subnet_rds_cidr     = var.private_subnet_rds_cidr
  availability_zone           = var.availability_zone
  secondary_availability_zone = var.secondary_availability_zone
}

# ── Módulo: Security Groups ───────────────────────────────────
module "security_groups" {
  source = "./modules/security_groups"

  project_name = var.project_name
  environment  = var.environment
  vpc_id       = module.vpc.vpc_id
}

# ── Módulo: EC2 ───────────────────────────────────────────────
module "ec2" {
  source = "./modules/ec2"

  project_name           = var.project_name
  environment            = var.environment
  instance_type          = var.instance_type
  ami_id                 = data.aws_ami.amazon_linux_2023.id
  public_key_path        = var.public_key_path
  lab_role_arn           = data.aws_iam_role.lab_role.arn
  lab_role_name          = data.aws_iam_role.lab_role.name

  # Subnets
  public_subnet_id          = module.vpc.public_subnet_id
  private_subnet_backend_id = module.vpc.private_subnet_backend_id
  private_subnet_ocr_id     = module.vpc.private_subnet_ocr_id

  # Security Groups
  sg_frontend_id = module.security_groups.sg_frontend_id
  sg_backend_id  = module.security_groups.sg_backend_id
  sg_ocr_id      = module.security_groups.sg_ocr_id

  # GHCR — credenciais para pull das imagens Docker nas EC2
  ghcr_token = var.ghcr_token
  ghcr_user  = var.ghcr_user
  repo_owner = var.repo_owner

  # Variáveis de ambiente do Backend (banco de dados e JWT)
  db_url      = var.db_url
  db_username    = var.db_username
  db_password    = var.db_password
  jwt_secret     = var.jwt_secret
  jwt_expiration = var.jwt_expiration
}

# ── Módulo: RDS ───────────────────────────────────────────────
module "rds" {
  source = "./modules/rds"

  project_name          = var.project_name
  environment           = var.environment
  db_name               = var.db_name
  db_username           = var.db_username
  db_password           = var.db_password
  rds_instance_class    = var.rds_instance_class
  rds_allocated_storage = var.rds_allocated_storage
  sg_rds_id             = module.security_groups.sg_rds_id

  # Subnet group usa as 3 subnets privadas
  private_subnet_backend_id = module.vpc.private_subnet_backend_id
  private_subnet_ocr_id     = module.vpc.private_subnet_ocr_id
  private_subnet_rds_id     = module.vpc.private_subnet_rds_id
}
