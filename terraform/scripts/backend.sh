set -eux
dnf update -y
dnf install -y docker


systemctl enable docker
systemctl start docker

usermod -aG docker ec2-user

echo "${ghcr_token}" | docker login ghcr.io \
  --username "${ghcr_user}" \
  --password-stdin
docker pull ghcr.io/${repo_owner}/jaf-backend:latest

docker run -d \
  --name jaf-backend \
  --restart always \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="${db_url}" \
  -e SPRING_DATASOURCE_USERNAME="${db_username}" \
  -e SPRING_DATASOURCE_PASSWORD="${db_password}" \
  ghcr.io/${repo_owner}/jaf-backend:latest