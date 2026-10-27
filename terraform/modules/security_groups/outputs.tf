# ============================================================
# modules/security_groups/outputs.tf
# ============================================================

output "sg_frontend_id" {
  description = "ID do Security Group do Frontend"
  value       = aws_security_group.frontend.id
}

output "sg_backend_id" {
  description = "ID do Security Group do Backend"
  value       = aws_security_group.backend.id
}

output "sg_ocr_id" {
  description = "ID do Security Group do OCR"
  value       = aws_security_group.ocr.id
}

output "sg_rds_id" {
  description = "ID do Security Group do RDS"
  value       = aws_security_group.rds.id
}
