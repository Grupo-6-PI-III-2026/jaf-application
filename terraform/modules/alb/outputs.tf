# ============================================================
# modules/alb/outputs.tf
# ============================================================

output "alb_dns_name" {
  description = "DNS name do Application Load Balancer"
  value       = aws_lb.main.dns_name
}

output "alb_arn" {
  description = "ARN do Application Load Balancer"
  value       = aws_lb.main.arn
}

output "alb_zone_id" {
  description = "Zone ID do ALB (útil para Route53)"
  value       = aws_lb.main.zone_id
}

output "frontend_target_group_arn" {
  description = "ARN do target group do frontend"
  value       = aws_lb_target_group.frontend.arn
}

output "backend_target_group_arn" {
  description = "ARN do target group do backend"
  value       = aws_lb_target_group.backend.arn
}
