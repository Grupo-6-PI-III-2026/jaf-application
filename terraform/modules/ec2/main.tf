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

data "aws_iam_instance_profile" "lab_profile" {
  count = var.use_lab_role ? 1 : 0
  name  = "LabInstanceProfile"
}

# Conta AWS normal: role minima para as instancias (acesso via SSM Session Manager)
resource "aws_iam_role" "instance" {
  count = var.use_lab_role ? 0 : 1
  name  = "${var.project_name}-instance-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })

  tags = {
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

resource "aws_iam_role_policy_attachment" "instance_ssm" {
  count      = var.use_lab_role ? 0 : 1
  role       = aws_iam_role.instance[0].name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "instance" {
  count = var.use_lab_role ? 0 : 1
  name  = "${var.project_name}-instance-profile"
  role  = aws_iam_role.instance[0].name
}

locals {
  instance_profile_name = var.use_lab_role ? data.aws_iam_instance_profile.lab_profile[0].name : aws_iam_instance_profile.instance[0].name
  ocr_habilitado        = var.ocr_asg.desired_capacity > 0
  rabbitmq_host         = local.ocr_habilitado ? aws_instance.rabbitmq[0].private_ip : "localhost"

  user_data_frontend = templatefile("${path.module}/../../scripts/frontend.sh", {
    ghcr_user    = var.ghcr_user
    ghcr_token   = var.ghcr_token
    repo_owner   = var.repo_owner
    backend_host = var.backend_internal_dns_name
  })

  user_data_backend = templatefile("${path.module}/../../scripts/backend.sh", {
    ghcr_user         = var.ghcr_user
    ghcr_token        = var.ghcr_token
    repo_owner        = var.repo_owner
    db_url            = var.db_url
    db_username       = var.db_username
    db_password       = var.db_password
    jwt_secret        = var.jwt_secret
    jwt_expiration    = var.jwt_expiration
    rabbitmq_host     = local.rabbitmq_host
    rabbitmq_user     = var.rabbitmq_user
    rabbitmq_password = var.rabbitmq_password
  })

  user_data_ocr = templatefile("${path.module}/../../scripts/ocr.sh", {
    ghcr_user         = var.ghcr_user
    ghcr_token        = var.ghcr_token
    repo_owner        = var.repo_owner
    rabbitmq_host     = local.rabbitmq_host
    rabbitmq_user     = var.rabbitmq_user
    rabbitmq_password = var.rabbitmq_password
    ocr_space_api_key = var.ocr_space_api_key
  })

  user_data_rabbitmq = templatefile("${path.module}/../../scripts/rabbitmq.sh", {
    rabbitmq_user     = var.rabbitmq_user
    rabbitmq_password = var.rabbitmq_password
  })
}

# Broker único: vários RabbitMQ independentes atrás de um ASG perderiam mensagens entre nós.
resource "aws_instance" "rabbitmq" {
  count                  = local.ocr_habilitado ? 1 : 0
  ami                    = var.ami_id
  instance_type          = var.instance_type
  key_name               = aws_key_pair.main.key_name
  subnet_id              = var.private_subnet_ocr_ids[0]
  vpc_security_group_ids = [var.sg_rabbitmq_id]
  iam_instance_profile   = local.instance_profile_name
  user_data              = base64encode(local.user_data_rabbitmq)

  root_block_device {
    volume_type           = "gp3"
    volume_size           = var.root_volume_size
    delete_on_termination = true
  }

  tags = {
    Name        = "rabbitmq-${var.project_name}"
    Role        = "rabbitmq"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

resource "aws_launch_template" "frontend" {
  name_prefix   = "${var.project_name}-frontend-"
  image_id      = var.ami_id
  instance_type = var.instance_type
  key_name      = aws_key_pair.main.key_name

  iam_instance_profile {
    name = local.instance_profile_name
  }

  vpc_security_group_ids = [var.sg_frontend_id]
  user_data              = base64encode(local.user_data_frontend)

  block_device_mappings {
    device_name = "/dev/xvda"

    ebs {
      volume_type           = "gp3"
      volume_size           = var.root_volume_size
      delete_on_termination = true
    }
  }

  tag_specifications {
    resource_type = "instance"

    tags = {
      Name        = "frontend-asg-${var.project_name}"
      Role        = "frontend"
      Environment = var.environment
      Project     = var.project_name
      ManagedBy   = "Terraform"
    }
  }
}

resource "aws_autoscaling_group" "frontend" {
  name                      = "${var.project_name}-frontend-asg"
  min_size                  = var.frontend_asg.min_size
  desired_capacity          = var.frontend_asg.desired_capacity
  max_size                  = var.frontend_asg.max_size
  health_check_type         = "ELB"
  health_check_grace_period = 300
  vpc_zone_identifier       = var.public_subnet_ids
  target_group_arns         = [var.frontend_target_group_arn]

  launch_template {
    id      = aws_launch_template.frontend.id
    version = "$Latest"
  }
}

resource "aws_launch_template" "backend" {
  name_prefix   = "${var.project_name}-backend-"
  image_id      = var.ami_id
  instance_type = var.instance_type
  key_name      = aws_key_pair.main.key_name

  iam_instance_profile {
    name = local.instance_profile_name
  }

  user_data = base64encode(local.user_data_backend)

  dynamic "network_interfaces" {
    for_each = var.backend_public_access ? [1] : []

    content {
      associate_public_ip_address = true
      security_groups             = [var.sg_backend_id]
    }
  }

  dynamic "network_interfaces" {
    for_each = var.backend_public_access ? [] : [1]

    content {
      security_groups = [var.sg_backend_id]
    }
  }

  block_device_mappings {
    device_name = "/dev/xvda"

    ebs {
      volume_type           = "gp3"
      volume_size           = var.root_volume_size
      delete_on_termination = true
    }
  }

  tag_specifications {
    resource_type = "instance"

    tags = {
      Name        = "backend-asg-${var.project_name}"
      Role        = "backend"
      Environment = var.environment
      Project     = var.project_name
      ManagedBy   = "Terraform"
    }
  }
}

resource "aws_autoscaling_group" "backend" {
  name                      = "${var.project_name}-backend-asg"
  min_size                  = var.backend_asg.min_size
  desired_capacity          = var.backend_asg.desired_capacity
  max_size                  = var.backend_asg.max_size
  health_check_type         = "ELB"
  health_check_grace_period = 300
  vpc_zone_identifier       = var.backend_public_access ? var.public_subnet_ids : var.private_subnet_backend_ids
  target_group_arns         = [var.backend_target_group_arn]

  launch_template {
    id      = aws_launch_template.backend.id
    version = "$Latest"
  }
}

resource "aws_launch_template" "ocr" {
  name_prefix   = "${var.project_name}-ocr-"
  image_id      = var.ami_id
  instance_type = var.instance_type
  key_name      = aws_key_pair.main.key_name

  iam_instance_profile {
    name = local.instance_profile_name
  }

  vpc_security_group_ids = [var.sg_ocr_id]
  user_data              = base64encode(local.user_data_ocr)

  block_device_mappings {
    device_name = "/dev/xvda"

    ebs {
      volume_type           = "gp3"
      volume_size           = var.root_volume_size
      delete_on_termination = true
    }
  }

  tag_specifications {
    resource_type = "instance"

    tags = {
      Name        = "ocr-asg-${var.project_name}"
      Role        = "ocr"
      Environment = var.environment
      Project     = var.project_name
      ManagedBy   = "Terraform"
    }
  }
}

resource "aws_autoscaling_group" "ocr" {
  name                      = "${var.project_name}-ocr-asg"
  min_size                  = var.ocr_asg.min_size
  desired_capacity          = var.ocr_asg.desired_capacity
  max_size                  = var.ocr_asg.max_size
  health_check_type         = "EC2"
  health_check_grace_period = 300
  vpc_zone_identifier       = var.private_subnet_ocr_ids

  launch_template {
    id      = aws_launch_template.ocr.id
    version = "$Latest"
  }
}
