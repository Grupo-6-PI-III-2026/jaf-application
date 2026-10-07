#!/bin/bash
set -eux
dnf update -y
dnf install -y docker
systemctl enable docker
systemctl start docker
usermod -aG docker ec2-user
echo "${ghcr_token}" | docker login ghcr.io \
  --username "${ghcr_user}" \
  --password-stdin
docker pull ghcr.io/${repo_owner}/jaf-frontend:latest

# ── Extrai o nginx.conf original de dentro da imagem ────────
# Cria um container temporário sem rodá-lo, só para copiar o arquivo
docker create --name temp ghcr.io/${repo_owner}/jaf-frontend:latest
docker cp temp:/etc/nginx/conf.d/default.conf /home/ec2-user/default.conf
docker rm temp

# ── Substitui o hostname "jaf-backend" pelo IP privado do backend ──
# O valor de ${backend_ip} é injetado pelo templatefile() do Terraform
sed -i "s/jaf-backend/${backend_ip}/g" /home/ec2-user/default.conf

# ── Sobe o container montando o conf corrigido como volume ───
# O :ro (read-only) garante que o Nginx não tente sobrescrever o arquivo
docker run -d \
  --name jaf-frontend \
  --restart always \
  -p 80:80 \
  -v /home/ec2-user/default.conf:/etc/nginx/conf.d/default.conf:ro \
  ghcr.io/${repo_owner}/jaf-frontend:latest