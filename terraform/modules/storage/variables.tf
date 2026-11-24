variable "project_name" {
  description = "Nome do projeto"
  type        = string
}

variable "environment" {
  description = "Ambiente de execução"
  type        = string
}

variable "force_destroy" {
  description = "true permite que o destroy apague o bucket mesmo com arquivos (fotos e notas são perdidos)"
  type        = bool
  default     = false
}
