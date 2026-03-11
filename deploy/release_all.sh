#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SSH_USER="${SSH_USER:-root}"
SSH_HOST="${SSH_HOST:-39.105.26.34}"
SSH_PORT="${SSH_PORT:-22}"
BACKEND_SERVICE="${BACKEND_SERVICE:-backend-api}"
REMOTE_FRONTEND_DIR="${REMOTE_FRONTEND_DIR:-/var/www/html/admin}"
REMOTE_BACKEND_JAR="${REMOTE_BACKEND_JAR:-/root/backend-prod.jar}"
LOCAL_FRONTEND_DIST="${LOCAL_FRONTEND_DIST:-$ROOT_DIR/admin-web/dist}"
LOCAL_BACKEND_JAR="${LOCAL_BACKEND_JAR:-$ROOT_DIR/backend-api/target/backend-api-0.0.1-SNAPSHOT.jar}"

log() {
  printf '[%s] %s\n' "$(date '+%F %T')" "$1"
}

run_ssh() {
  ssh -p "$SSH_PORT" "${SSH_USER}@${SSH_HOST}" "$1"
}

log "开始构建前端"
(
  cd "$ROOT_DIR/admin-web"
  npm run build
)

log "开始构建后端"
(
  cd "$ROOT_DIR/backend-api"
  ./mvnw -DskipTests package
)

if [[ ! -d "$LOCAL_FRONTEND_DIST" ]]; then
  echo "前端构建产物不存在: $LOCAL_FRONTEND_DIST" >&2
  exit 1
fi

if [[ ! -f "$LOCAL_BACKEND_JAR" ]]; then
  echo "后端构建产物不存在: $LOCAL_BACKEND_JAR" >&2
  exit 1
fi

log "准备服务器目录"
run_ssh "mkdir -p '$REMOTE_FRONTEND_DIR'"

log "上传前端产物"
scp -P "$SSH_PORT" -r "$LOCAL_FRONTEND_DIST"/. "${SSH_USER}@${SSH_HOST}:${REMOTE_FRONTEND_DIR}/"

log "上传后端产物"
scp -P "$SSH_PORT" "$LOCAL_BACKEND_JAR" "${SSH_USER}@${SSH_HOST}:${REMOTE_BACKEND_JAR}"

log "重启服务器前后端服务"
run_ssh "sudo nginx -t && sudo systemctl reload nginx && sudo systemctl restart '$BACKEND_SERVICE' && sudo systemctl status '$BACKEND_SERVICE' --no-pager"

log "发布完成"
