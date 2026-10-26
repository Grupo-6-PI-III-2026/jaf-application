# ===========================================================
# main.tf (root) — Instancia todos os módulos
# ===========================================================

# ── Data sources globais ──────────────────────────────────────

# No Learner Lab, a LabRole/LabInstanceProfile ja existem e sao resolvidas no modulo EC2.
# Em conta AWS normal (use_lab_role = false), o modulo EC2 cria role e instance profile proprios.

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

  project_name                          = var.project_name
  environment                           = var.environment
  vpc_cidr                              = var.vpc_cidr
  public_subnet_cidr                    = var.public_subnet_cidr
  private_subnet_backend_cidr           = var.private_subnet_backend_cidr
  private_subnet_ocr_cidr               = var.private_subnet_ocr_cidr
  private_subnet_rds_cidr               = var.private_subnet_rds_cidr
  public_subnet_secondary_cidr          = var.public_subnet_secondary_cidr
  private_subnet_backend_secondary_cidr = var.private_subnet_backend_secondary_cidr
  private_subnet_ocr_secondary_cidr     = var.private_subnet_ocr_secondary_cidr
  private_subnet_rds_secondary_cidr     = var.private_subnet_rds_secondary_cidr
  availability_zone                     = var.availability_zone
  secondary_availability_zone           = var.secondary_availability_zone
  single_nat_gateway                    = var.single_nat_gateway
}

# ── Módulo: Security Groups ───────────────────────────────────
module "security_groups" {
  source = "./modules/security_groups"

  project_name          = var.project_name
  environment           = var.environment
  vpc_id                = module.vpc.vpc_id
  eic_security_group_id = module.vpc.sg_eic_endpoint_id
  backend_public_access = var.backend_public_access
  frontend_ssh_cidrs    = var.frontend_ssh_cidrs
}

# ── Módulo: EC2 ───────────────────────────────────────────────
module "ec2" {
  source = "./modules/ec2"

  project_name     = var.project_name
  environment      = var.environment
  instance_type    = var.instance_type
  ami_id           = data.aws_ami.amazon_linux_2023.id
  public_key_path  = var.public_key_path
  use_lab_role     = var.use_lab_role
  root_volume_size = var.root_volume_size
  frontend_asg     = var.frontend_asg
  backend_asg      = var.backend_asg
  ocr_asg          = var.ocr_asg

  # Subnets
  public_subnet_ids          = module.vpc.public_subnet_ids
  private_subnet_backend_ids = module.vpc.private_subnet_backend_ids
  private_subnet_ocr_ids     = module.vpc.private_subnet_ocr_ids
  frontend_target_group_arn  = module.alb.frontend_target_group_arn
  backend_target_group_arn   = module.alb.backend_target_group_arn
  backend_internal_dns_name  = module.alb.backend_internal_dns_name
  backend_public_access      = var.backend_public_access

  # Security Groups
  sg_frontend_id = module.security_groups.sg_frontend_id
  sg_backend_id  = module.security_groups.sg_backend_id
  sg_ocr_id      = module.security_groups.sg_ocr_id

  # GHCR — credenciais para pull das imagens Docker nas EC2
  ghcr_token = var.ghcr_token
  ghcr_user  = var.ghcr_user
  repo_owner = var.repo_owner

  # Variáveis de ambiente do Backend (banco de dados e JWT)
  db_url         = var.db_url
  db_username    = var.db_username
  db_password    = var.db_password
  jwt_secret     = var.jwt_secret
  jwt_expiration = var.jwt_expiration
}

# ── Módulo: RDS ───────────────────────────────────────────────
module "rds" {
  source = "./modules/rds"

  vpc_id                = module.vpc.vpc_id
  project_name          = var.project_name
  environment           = var.environment
  db_name               = var.db_name
  db_username           = var.db_username
  db_password           = var.db_password
  rds_instance_class    = var.rds_instance_class
  rds_allocated_storage = var.rds_allocated_storage
  multi_az              = var.rds_multi_az
  apply_immediately     = var.rds_apply_immediately
  sg_rds_id             = module.security_groups.sg_rds_id

  # Subnets privadas dedicadas em duas AZs
  private_subnet_rds_ids = module.vpc.private_subnet_rds_ids
}

# ── Módulo: Application Load Balancer ─────────────────────────
module "alb" {
  source = "./modules/alb"

  project_name                       = var.project_name
  environment                        = var.environment
  vpc_id                             = module.vpc.vpc_id
  public_subnet_ids                  = module.vpc.public_subnet_ids
  private_backend_subnet_ids         = module.vpc.private_subnet_backend_ids
  alb_security_group_id              = module.security_groups.sg_alb_id
  backend_internal_security_group_id = module.security_groups.sg_backend_alb_id
  acm_certificate_arn                = var.acm_certificate_arn
  enable_https                       = var.enable_https
}
