#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SSH_USER="${SSH_USER:-root}"
SSH_HOST="${SSH_HOST:-39.105.26.34}"
SSH_PORT="${SSH_PORT:-22}"
BACKEND_SERVICE="${BACKEND_SERVICE:-backend-api}"
REMOTE_FRONTEND_DIR="${REMOTE_FRONTEND_DIR:-/var/www/html/admin}"
REMOTE_OFFICIAL_DIR="${REMOTE_OFFICIAL_DIR:-/var/www/html}"
REMOTE_BACKEND_JAR="${REMOTE_BACKEND_JAR:-/root/backend-prod.jar}"
REMOTE_BACKEND_ENV_FILE="${REMOTE_BACKEND_ENV_FILE:-/etc/backend-api/backend-api.env}"
REMOTE_BACKEND_ENV_DIR="$(dirname "$REMOTE_BACKEND_ENV_FILE")"
LOCAL_FRONTEND_DIST="${LOCAL_FRONTEND_DIST:-$ROOT_DIR/admin-web/dist}"
LOCAL_OFFICIAL_PAGE="${LOCAL_OFFICIAL_PAGE:-$ROOT_DIR/deploy/index.html}"
LOCAL_OFFICIAL_FAVICON="${LOCAL_OFFICIAL_FAVICON:-$ROOT_DIR/admin-web/public/favicon.svg}"
LOCAL_BACKEND_JAR="${LOCAL_BACKEND_JAR:-$ROOT_DIR/backend-api/target/backend-api-0.0.1-SNAPSHOT.jar}"
LOCAL_BACKEND_ENV_FILE="${LOCAL_BACKEND_ENV_FILE:-}"
LOCAL_JOURNALD_CONFIG="${LOCAL_JOURNALD_CONFIG:-$ROOT_DIR/deploy/ops/journald-vehicle-recycle-system.conf}"
LOCAL_LOGROTATE_CONFIG="${LOCAL_LOGROTATE_CONFIG:-$ROOT_DIR/deploy/ops/vehicle-recycle-system.logrotate}"
LOCAL_LOGROTATE_SERVICE="${LOCAL_LOGROTATE_SERVICE:-$ROOT_DIR/deploy/ops/vehicle-recycle-system-logrotate.service}"
LOCAL_LOGROTATE_TIMER="${LOCAL_LOGROTATE_TIMER:-$ROOT_DIR/deploy/ops/vehicle-recycle-system-logrotate.timer}"
REMOTE_JOURNALD_CONFIG="${REMOTE_JOURNALD_CONFIG:-/etc/systemd/journald.conf.d/vehicle-recycle-system.conf}"
REMOTE_LOGROTATE_CONFIG="${REMOTE_LOGROTATE_CONFIG:-/etc/logrotate.d/vehicle-recycle-system}"
REMOTE_LOGROTATE_SERVICE="${REMOTE_LOGROTATE_SERVICE:-/etc/systemd/system/vehicle-recycle-system-logrotate.service}"
REMOTE_LOGROTATE_TIMER="${REMOTE_LOGROTATE_TIMER:-/etc/systemd/system/vehicle-recycle-system-logrotate.timer}"
REMOTE_MIN_FREE_MB="${REMOTE_MIN_FREE_MB:-2048}"
REMOTE_MAX_USED_PERCENT="${REMOTE_MAX_USED_PERCENT:-85}"

log() {
  printf '[%s] %s\n' "$(date '+%F %T')" "$1"
}

run_ssh() {
  ssh -p "$SSH_PORT" "${SSH_USER}@${SSH_HOST}" "$1"
}

install_log_governance() {
  if [[ -f "$LOCAL_JOURNALD_CONFIG" && -f "$LOCAL_LOGROTATE_CONFIG" && -f "$LOCAL_LOGROTATE_SERVICE" && -f "$LOCAL_LOGROTATE_TIMER" ]]; then
    log "安装生产日志治理配置"
    run_ssh "sudo mkdir -p '$(dirname "$REMOTE_JOURNALD_CONFIG")'"
    scp -P "$SSH_PORT" "$LOCAL_JOURNALD_CONFIG" "${SSH_USER}@${SSH_HOST}:/tmp/vehicle-recycle-system-journald.conf"
    scp -P "$SSH_PORT" "$LOCAL_LOGROTATE_CONFIG" "${SSH_USER}@${SSH_HOST}:/tmp/vehicle-recycle-system.logrotate"
    scp -P "$SSH_PORT" "$LOCAL_LOGROTATE_SERVICE" "${SSH_USER}@${SSH_HOST}:/tmp/vehicle-recycle-system-logrotate.service"
    scp -P "$SSH_PORT" "$LOCAL_LOGROTATE_TIMER" "${SSH_USER}@${SSH_HOST}:/tmp/vehicle-recycle-system-logrotate.timer"
    run_ssh "set -e; sudo mv /tmp/vehicle-recycle-system-journald.conf '$REMOTE_JOURNALD_CONFIG'; sudo mv /tmp/vehicle-recycle-system.logrotate '$REMOTE_LOGROTATE_CONFIG'; sudo mv /tmp/vehicle-recycle-system-logrotate.service '$REMOTE_LOGROTATE_SERVICE'; sudo mv /tmp/vehicle-recycle-system-logrotate.timer '$REMOTE_LOGROTATE_TIMER'; sudo chmod 644 '$REMOTE_JOURNALD_CONFIG' '$REMOTE_LOGROTATE_CONFIG' '$REMOTE_LOGROTATE_SERVICE' '$REMOTE_LOGROTATE_TIMER'; sudo systemctl daemon-reload; sudo systemctl restart systemd-journald; sudo systemctl enable --now vehicle-recycle-system-logrotate.timer; sudo logrotate -f '$REMOTE_LOGROTATE_CONFIG' >/dev/null 2>&1 || true; sudo journalctl --vacuum-size=500M >/dev/null 2>&1 || true"
  fi
}

java_version_from_home() {
  local java_home="$1"
  "$java_home/bin/java" -version 2>&1 | awk -F'"' 'NR==1 {print $2}'
}

java_vendor_from_home() {
  local java_home="$1"
  "$java_home/bin/java" -XshowSettings:properties -version 2>&1 | awk -F'= ' '/^[[:space:]]*java.vendor = / {print $2; exit}'
}

ensure_backend_java() {
  local current_version current_vendor candidate candidate_version candidate_vendor
  local -a candidates=()

  if ! command -v java >/dev/null 2>&1; then
    echo "未检测到 java 命令，无法继续后端构建。" >&2
    echo "请先安装 Eclipse Temurin 17。" >&2
    exit 1
  fi

  current_version="$(java -version 2>&1 | awk -F'"' 'NR==1 {print $2}')"
  current_vendor="$(java -XshowSettings:properties -version 2>&1 | awk -F'= ' '/^[[:space:]]*java.vendor = / {print $2; exit}')"

  if [[ "$current_version" == 17.* && "$current_vendor" == "Eclipse Adoptium" ]]; then
    log "检测到后端构建 JDK: $current_vendor $current_version"
    return
  fi

  if [[ -n "${JAVA_HOME:-}" ]]; then
    candidates+=("$JAVA_HOME")
  fi

  if [[ -x /usr/libexec/java_home ]]; then
    candidate="$(/usr/libexec/java_home -v 17 2>/dev/null || true)"
    if [[ -n "$candidate" ]]; then
      candidates+=("$candidate")
    fi
  fi

  for candidate in \
    /Library/Java/JavaVirtualMachines/temurin-17*.jdk/Contents/Home \
    "$HOME"/Library/Java/JavaVirtualMachines/temurin-17*.jdk/Contents/Home
  do
    if [[ -d "$candidate" ]]; then
      candidates+=("$candidate")
    fi
  done

  for candidate in "${candidates[@]}"; do
    if [[ ! -x "$candidate/bin/java" ]]; then
      continue
    fi

    candidate_version="$(java_version_from_home "$candidate")"
    candidate_vendor="$(java_vendor_from_home "$candidate")"
    if [[ "$candidate_version" == 17.* && "$candidate_vendor" == "Eclipse Adoptium" ]]; then
      export JAVA_HOME="$candidate"
      export PATH="$JAVA_HOME/bin:$PATH"
      log "已自动切换后端构建 JDK: $candidate_vendor $candidate_version ($JAVA_HOME)"
      return
    fi
  done

  echo "未找到可用的 Eclipse Temurin 17，无法继续后端构建。" >&2
  echo "当前检测到的 Java: version=${current_version:-unknown}, vendor=${current_vendor:-unknown}" >&2
  echo "请先安装并切换到 Temurin 17，例如：" >&2
  echo "  brew install --cask temurin@17" >&2
  echo "  export JAVA_HOME=\$(/usr/libexec/java_home -v 17)" >&2
  echo "  export PATH=\"\$JAVA_HOME/bin:\$PATH\"" >&2
  echo "然后重新执行 ./deploy/release_all.sh" >&2
  exit 1
}

ensure_backend_java

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

if [[ ! -f "$LOCAL_OFFICIAL_PAGE" ]]; then
  echo "官网静态页不存在: $LOCAL_OFFICIAL_PAGE" >&2
  exit 1
fi

if [[ ! -f "$LOCAL_OFFICIAL_FAVICON" ]]; then
  echo "官网图标不存在: $LOCAL_OFFICIAL_FAVICON" >&2
  # 允许图标不存在，不强制退出，但给出警告
fi

log "准备服务器目录"
run_ssh "mkdir -p '$REMOTE_FRONTEND_DIR' '$REMOTE_OFFICIAL_DIR'"

install_log_governance

log "检查服务器磁盘空间"
run_ssh "used=\$(df -Pm / | awk 'NR==2 {gsub(/%/, \"\", \$5); print \$5}'); avail=\$(df -Pm / | awk 'NR==2 {print \$4}'); echo \"rootfs: used=\${used}% avail=\${avail}MB\"; if [ \"\$used\" -ge '$REMOTE_MAX_USED_PERCENT' ] || [ \"\$avail\" -lt '$REMOTE_MIN_FREE_MB' ]; then echo '服务器磁盘空间不足，终止发布' >&2; exit 1; fi"

log "上传前端产物"
scp -P "$SSH_PORT" -r "$LOCAL_FRONTEND_DIST"/. "${SSH_USER}@${SSH_HOST}:${REMOTE_FRONTEND_DIR}/"

log "上传官网静态页和图标"
scp -P "$SSH_PORT" "$LOCAL_OFFICIAL_PAGE" "${SSH_USER}@${SSH_HOST}:${REMOTE_OFFICIAL_DIR}/"
if [[ -f "$LOCAL_OFFICIAL_FAVICON" ]]; then
  scp -P "$SSH_PORT" "$LOCAL_OFFICIAL_FAVICON" "${SSH_USER}@${SSH_HOST}:${REMOTE_OFFICIAL_DIR}/favicon.svg"
fi

log "上传后端产物"
scp -P "$SSH_PORT" "$LOCAL_BACKEND_JAR" "${SSH_USER}@${SSH_HOST}:${REMOTE_BACKEND_JAR}"

if [[ -n "$LOCAL_BACKEND_ENV_FILE" ]]; then
  if [[ ! -f "$LOCAL_BACKEND_ENV_FILE" ]]; then
    echo "指定的环境变量文件不存在: $LOCAL_BACKEND_ENV_FILE" >&2
    exit 1
  fi
  log "上传后端环境变量文件"
  run_ssh "sudo mkdir -p '$REMOTE_BACKEND_ENV_DIR'"
  scp -P "$SSH_PORT" "$LOCAL_BACKEND_ENV_FILE" "${SSH_USER}@${SSH_HOST}:/tmp/backend-api.env"
  run_ssh "sudo mv /tmp/backend-api.env '$REMOTE_BACKEND_ENV_FILE' && sudo chmod 600 '$REMOTE_BACKEND_ENV_FILE'"
fi

log "检查后端环境变量是否齐全"
run_ssh "test -f '$REMOTE_BACKEND_ENV_FILE' || (echo '缺少环境变量文件: $REMOTE_BACKEND_ENV_FILE' >&2; exit 1)"
run_ssh "grep -qE '^WX_MINIAPP_SECRET=' '$REMOTE_BACKEND_ENV_FILE' || (echo '缺少 WX_MINIAPP_SECRET（小程序登录会失败）' >&2; exit 1)"
run_ssh "grep -qE '^JWT_SECRET=' '$REMOTE_BACKEND_ENV_FILE' || (echo '缺少 JWT_SECRET（后端将无法启动）' >&2; exit 1)"

log "重启服务器前后端服务"
run_ssh "sudo nginx -t && sudo systemctl reload nginx && sudo systemctl restart '$BACKEND_SERVICE' && sudo systemctl status '$BACKEND_SERVICE' --no-pager"

log "检查后端启动与登录接口可达性"
run_ssh "for i in \$(seq 1 30); do curl -fsS -o /dev/null -X OPTIONS 'http://127.0.0.1:8090/api/auth/login' -H 'Origin: https://xhyscrapcar.com' -H 'Access-Control-Request-Method: POST' && exit 0; sleep 2; done; echo 'backend login endpoint not ready on 127.0.0.1:8090' >&2; sudo journalctl -u '$BACKEND_SERVICE' -n 80 --no-pager >&2; exit 1"

log "发布完成"
