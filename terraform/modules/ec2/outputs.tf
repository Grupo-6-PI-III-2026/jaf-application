output "frontend_asg_name" {
  description = "Nome do Auto Scaling Group do frontend"
  value       = aws_autoscaling_group.frontend.name
}

output "backend_asg_name" {
  description = "Nome do Auto Scaling Group do backend"
  value       = aws_autoscaling_group.backend.name
}

output "ocr_asg_name" {
  description = "Nome do Auto Scaling Group do OCR"
  value       = aws_autoscaling_group.ocr.name
}

output "key_pair_name" {
  description = "Nome do Key Pair criado na AWS"
  value       = aws_key_pair.main.key_name
}
