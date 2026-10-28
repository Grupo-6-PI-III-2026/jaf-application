#!/bin/bash
set -eux
dnf update -y
dnf install -y docker ec2-instance-connect openssh-server
systemctl enable docker
systemctl start docker
systemctl enable --now sshd
usermod -aG docker ec2-user
echo "${ghcr_token}" | docker login ghcr.io \
  --username "${ghcr_user}" \
  --password-stdin
docker pull ghcr.io/${repo_owner}/jaf-frontend:latest

# BACKEND_HOST alimenta o template do Nginx; o DNS do ALB interno é resolvido periodicamente.
docker run -d \
  --name jaf-frontend \
  --restart always \
  -p 80:80 \
  -e BACKEND_HOST="${backend_host}" \
  ghcr.io/${repo_owner}/jaf-frontend:latest