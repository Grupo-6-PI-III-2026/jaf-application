# Bucket privado para fotos (perfil e obras) e notas fiscais.
# O navegador só acessa os arquivos por URLs pré-assinadas geradas pelo backend.
resource "aws_s3_bucket" "arquivos" {
  bucket_prefix = "${var.project_name}-arquivos-"
  force_destroy = var.force_destroy

  tags = {
    Name        = "arquivos-${var.project_name}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

resource "aws_s3_bucket_public_access_block" "arquivos" {
  bucket                  = aws_s3_bucket.arquivos.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_ownership_controls" "arquivos" {
  bucket = aws_s3_bucket.arquivos.id

  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "arquivos" {
  bucket = aws_s3_bucket.arquivos.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

data "aws_iam_policy_document" "somente_https" {
  statement {
    sid     = "NegarAcessoSemTLS"
    effect  = "Deny"
    actions = ["s3:*"]
    resources = [
      aws_s3_bucket.arquivos.arn,
      "${aws_s3_bucket.arquivos.arn}/*",
    ]

    principals {
      type        = "*"
      identifiers = ["*"]
    }

    condition {
      test     = "Bool"
      variable = "aws:SecureTransport"
      values   = ["false"]
    }
  }
}

resource "aws_s3_bucket_policy" "arquivos" {
  bucket = aws_s3_bucket.arquivos.id
  policy = data.aws_iam_policy_document.somente_https.json

  depends_on = [aws_s3_bucket_public_access_block.arquivos]
}
