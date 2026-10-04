
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
docker run -d \
  --name jaf-frontend \
  --restart always \
  -p 80:80 \
  ghcr.io/${repo_owner}/jaf-frontend:latest