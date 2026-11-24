output "bucket_name" {
  description = "Nome do bucket de arquivos"
  value       = aws_s3_bucket.arquivos.id
}

output "bucket_arn" {
  description = "ARN do bucket de arquivos"
  value       = aws_s3_bucket.arquivos.arn
}
