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
  name = "LabInstanceProfile"
}

locals {
  user_data_frontend = templatefile("${path.module}/../../scripts/frontend.sh", {
    ghcr_user    = var.ghcr_user
    ghcr_token   = var.ghcr_token
    repo_owner   = var.repo_owner
    backend_host = var.backend_internal_dns_name
  })

  user_data_backend = templatefile("${path.module}/../../scripts/backend.sh", {
    ghcr_user      = var.ghcr_user
    ghcr_token     = var.ghcr_token
    repo_owner     = var.repo_owner
    db_url         = var.db_url
    db_username    = var.db_username
    db_password    = var.db_password
    jwt_secret     = var.jwt_secret
    jwt_expiration = var.jwt_expiration
  })

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

resource "aws_launch_template" "frontend" {
  name_prefix   = "${var.project_name}-frontend-"
  image_id      = var.ami_id
  instance_type = var.instance_type
  key_name      = aws_key_pair.main.key_name

  iam_instance_profile {
    name = data.aws_iam_instance_profile.lab_profile.name
  }

  vpc_security_group_ids = [var.sg_frontend_id]
  user_data              = base64encode(local.user_data_frontend)

  block_device_mappings {
    device_name = "/dev/xvda"

    ebs {
      volume_type           = "gp3"
      volume_size           = 30
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
  min_size                  = 2
  desired_capacity          = 2
  max_size                  = 4
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
    name = data.aws_iam_instance_profile.lab_profile.name
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
      volume_size           = 30
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
  min_size                  = 2
  desired_capacity          = 2
  max_size                  = 4
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
    name = data.aws_iam_instance_profile.lab_profile.name
  }

  vpc_security_group_ids = [var.sg_ocr_id]
  user_data              = base64encode(local.user_data_ocr)

  block_device_mappings {
    device_name = "/dev/xvda"

    ebs {
      volume_type           = "gp3"
      volume_size           = 30
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
  min_size                  = 2
  desired_capacity          = 2
  max_size                  = 4
  health_check_type         = "EC2"
  health_check_grace_period = 300
  vpc_zone_identifier       = var.private_subnet_ocr_ids

  launch_template {
    id      = aws_launch_template.ocr.id
    version = "$Latest"
  }
}
