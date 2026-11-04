
FROM node:20.19-alpine3.21 AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
ARG VITE_API_URL=/api
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build


FROM nginx:1.27-alpine

ENV BACKEND_HOST=jaf-backend \
    NGINX_ENTRYPOINT_LOCAL_RESOLVERS=1

COPY --from=builder /app/dist /usr/share/nginx/html

COPY security.conf /etc/nginx/snippets/security.conf
COPY nginx.conf /etc/nginx/templates/default.conf.template
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
