# ============================================================
# modules/ec2/outputs.tf
# ============================================================

output "frontend_public_ip" {
  description = "Elastic IP público do frontend"
  value       = aws_eip.frontend.public_ip
}

output "frontend_public_dns" {
  description = "DNS público do Elastic IP do frontend"
  value       = aws_eip.frontend.public_dns
}

output "frontend_instance_id" {
  description = "ID da instância EC2 do frontend"
  value       = aws_instance.frontend.id
}

output "backend_private_ip" {
  description = "IP privado da instância do backend"
  value       = aws_instance.backend.private_ip
}

output "backend_instance_id" {
  description = "ID da instância EC2 do backend"
  value       = aws_instance.backend.id
}

output "ocr_private_ip" {
  description = "IP privado da instância do OCR"
  value       = aws_instance.ocr.private_ip
}

output "ocr_instance_id" {
  description = "ID da instância EC2 do OCR"
  value       = aws_instance.ocr.id
}

output "key_pair_name" {
  description = "Nome do Key Pair criado na AWS"
  value       = aws_key_pair.main.key_name
}
