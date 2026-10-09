# ============================================================
# modules/alb/main.tf — Application Load Balancer, Target Groups, Listeners
# ============================================================

# ── Application Load Balancer ────────────────────────────────────
# ALB na subnet pública para distribuir tráfego para o frontend
resource "aws_lb" "main" {
  name               = "${var.project_name}-alb"
  internal           = false  # ALB público
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
    path                = "/actuator/health"  # Endpoint padrão do Spring Boot Actuator
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

# ── Listener HTTP (Porta 80) ──────────────────────────────────────
# Listener principal que roteia baseado no path
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = 80
  protocol          = "HTTP"

  # Rota padrão: tudo vai para o frontend
  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.frontend.arn
  }
}

# ── Listener Rule para /api/* ─────────────────────────────────────
# Requisições para /api/* são roteadas para o backend
resource "aws_lb_listener_rule" "api" {
  listener_arn = aws_lb_listener.http.arn
  priority     = 100

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend.arn
  }

  condition {
    path_pattern {
      values = ["/api/*"]
    }
  }
}

# ── Attachment do Frontend ao Target Group ───────────────────────
# Anexa a instância EC2 do frontend ao target group
resource "aws_lb_target_group_attachment" "frontend" {
  target_group_arn = aws_lb_target_group.frontend.arn
  target_id        = var.frontend_instance_id
  port             = 80
}

# ── Attachment do Backend ao Target Group ────────────────────────
# Anexa a instância EC2 do backend ao target group
resource "aws_lb_target_group_attachment" "backend" {
  target_group_arn = aws_lb_target_group.backend.arn
  target_id        = var.backend_instance_id
  port             = 8080
}
