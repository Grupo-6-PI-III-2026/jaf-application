# ============================================================
# modules/alb/main.tf — Application Load Balancer, Target Groups, Listeners
# ============================================================

# ── Application Load Balancer ────────────────────────────────────
# ALB na subnet pública para distribuir tráfego para o frontend
resource "aws_lb" "main" {
  name               = "${var.project_name}-alb"
  internal           = false # ALB público
  load_balancer_type = "application"
  security_groups    = [var.alb_security_group_id]
  subnets            = var.public_subnet_ids

  enable_deletion_protection = false
  enable_http2               = true

  tags = {
    Name        = "alb-${var.project_name}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

# ── Target Group do Frontend ─────────────────────────────────────
# Para as instâncias EC2 do frontend React (porta 80)
resource "aws_lb_target_group" "frontend" {
  name        = "${var.project_name}-frontend-tg"
  port        = 80
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "instance"

  health_check {
    enabled             = true
    healthy_threshold   = 2
    interval            = 30
    matcher             = "200"
    path                = "/"
    port                = "traffic-port"
    protocol            = "HTTP"
    timeout             = 5
    unhealthy_threshold = 3
  }

  tags = {
    Name        = "tg-frontend-${var.project_name}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

# ── Target Group do Backend ──────────────────────────────────────
# Para as instâncias EC2 do backend Spring Boot (porta 8080)
resource "aws_lb_target_group" "backend" {
  name        = "${var.project_name}-backend-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "instance"

  health_check {
    enabled             = true
    healthy_threshold   = 2
    interval            = 30
    matcher             = "200"
    path                = "/actuator/health" # Endpoint padrão do Spring Boot Actuator
    port                = "traffic-port"
    protocol            = "HTTP"
    timeout             = 5
    unhealthy_threshold = 3
  }

  tags = {
    Name        = "tg-backend-${var.project_name}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

resource "aws_lb" "backend_internal" {
  name               = "${var.project_name}-backend-int"
  internal           = true
  load_balancer_type = "application"
  security_groups    = [var.backend_internal_security_group_id]
  subnets            = var.private_backend_subnet_ids

  enable_http2 = true

  tags = {
    Name        = "alb-backend-internal-${var.project_name}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

resource "aws_lb_listener" "backend_internal" {
  load_balancer_arn = aws_lb.backend_internal.arn
  port              = 8080
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend.arn
  }
}

# ── Listener HTTPS (Porta 443) ────────────────────────────────────
resource "aws_lb_listener" "https" {
  count             = var.enable_https ? 1 : 0
  load_balancer_arn = aws_lb.main.arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = var.acm_certificate_arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.frontend.arn
  }
}

# ── Listener HTTP (Porta 80) ──────────────────────────────────────
# Redireciona o tráfego para HTTPS.
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = 80
  protocol          = "HTTP"

  dynamic "default_action" {
    for_each = var.enable_https ? [1] : []

    content {
      type = "redirect"

      redirect {
        port        = "443"
        protocol    = "HTTPS"
        status_code = "HTTP_301"
      }
    }
  }

  dynamic "default_action" {
    for_each = var.enable_https ? [] : [1]

    content {
      type             = "forward"
      target_group_arn = aws_lb_target_group.frontend.arn
    }
  }
}

# O ALB público NÃO possui regra para /api/*: todo o tráfego vai para o frontend (Nginx),
# que remove o prefixo /api e repassa ao ALB interno do backend. Uma regra /api/* aqui
# enviaria o path sem rewrite ao Spring e resultaria em 403.
