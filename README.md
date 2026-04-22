# vehicle-recycle-system

## 开发环境要求

### 后端 JDK

- `backend-api` 现在强制要求使用 `Eclipse Temurin 17`
- Maven 构建会校验：
  - `java.version` 必须是 `17.x`
  - `java.vendor` 必须是 `Eclipse Adoptium`
- 当前如果继续使用 Oracle JDK 或 Oracle 发布的 OpenJDK，`./mvnw validate` 会失败

详细说明见 [backend-api/docs/java-runtime.md](backend-api/docs/java-runtime.md)。

### 第三方依赖说明

仓库级第三方依赖说明草稿见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。

## 生产发版（数据库 + 后端 + 前端）

以下命令基于当前目录结构与服务器路径：
- 服务器：`root@39.105.26.34`
- 前端发布目录：`/var/www/html/admin/`
- 后端 Jar 路径：`/root/backend-prod.jar`
- 后端服务名：`backend-api`
- MySQL 容器名：`mysql-prod`
- 生产库名：`scrap_system`

### 常用快捷命令

```bash
cd /Users/kkkfcc/Desktop/vehicle-recycle-system

# 一次性赋予执行权限
chmod +x deploy/release_all.sh deploy/release_frontend.sh scripts/run_prod_local.sh

# 1) 一键发布前后端（含 nginx reload + backend-api restart）
./deploy/release_all.sh

# 2) 仅发布前端 admin-web
./deploy/release_frontend.sh

# 3) 本地启动（连接生产库，自动建 SSH 隧道）
# 需先在 backend-api/src/main/resources/application-local-prod.yaml 配好 password
SSH_HOST=39.105.26.34 SSH_USER=root ./scripts/run_prod_local.sh
```

### 1) 本地构建（前后端）

```bash
cd /Users/kkkfcc/Desktop/vehicle-recycle-system

# 构建前端
cd admin-web
npm run build

# 构建后端
cd ../backend-api
./mvnw -DskipTests package
```

### 2) 上传前后端产物

```bash
cd /Users/kkkfcc/Desktop/vehicle-recycle-system/backend-api

# 上传前端 dist
scp -r ../admin-web/dist/* root@39.105.26.34:/var/www/html/admin/

# 上传后端 jar
scp ./target/backend-api-0.0.1-SNAPSHOT.jar root@39.105.26.34:/root/backend-prod.jar
```

### 3) 上传数据库发布脚本（若服务器未准备）

```bash
RELEASE_DIR=2026-04-21-api-performance-indexes

ssh root@39.105.26.34 "mkdir -p /root/vehicle-recycle-system/backend-api/docs/prod-db-release"

scp -r /Users/kkkfcc/Desktop/vehicle-recycle-system/backend-api/docs/prod-db-release/${RELEASE_DIR} \
  root@39.105.26.34:/root/vehicle-recycle-system/backend-api/docs/prod-db-release/
```

### 4) 服务器执行数据库预检查与迁移（Docker MySQL）

**安全提示**：使用 `export` 临时注入密码，避免明文留痕。

```bash
RELEASE_DIR=2026-04-21-api-performance-indexes

ssh root@39.105.26.34
cd /root/vehicle-recycle-system/backend-api/docs/prod-db-release/${RELEASE_DIR}

# 4.1 临时加载数据库密码到环境变量（执行完会自动清除）
export $(grep DB_PASSWORD /etc/backend-api/backend-api.env | xargs)

# 4.2 备份数据库
mkdir -p /root/db-backup
docker exec mysql-prod sh -c \
  'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --quick --routines --triggers --databases scrap_system' \
  > /root/db-backup/scrap_system_$(date +%F_%H%M%S).sql
gzip -9 /root/db-backup/scrap_system_*.sql

# 4.3 预检查（确认旧数据状态）
docker exec -i mysql-prod mysql -uroot -p"$DB_PASSWORD" scrap_system < precheck.sql

# 4.4 执行迁移
docker exec -i mysql-prod mysql -uroot -p"$DB_PASSWORD" scrap_system < migrate.sql

# 4.5 迁移后复查（验证新结构与回填）
docker exec -i mysql-prod mysql -uroot -p"$DB_PASSWORD" scrap_system < postcheck.sql

# 4.6 清除环境变量
unset DB_PASSWORD
```

### 5) 服务器重载应用服务

```bash
ssh root@39.105.26.34 '
  sudo nginx -t &&
  sudo systemctl reload nginx &&
  sudo systemctl restart backend-api &&
  sudo systemctl status backend-api --no-pager
'
```

### 6) 发布后快速验收

```bash
# 后端接口不应出现 500
curl -i http://39.105.26.34/api/auth/me

# 查看后端最近日志
ssh root@39.105.26.34 "sudo journalctl -u backend-api -n 120 --no-pager"
```

### 7) 如需回滚（先回滚应用，再回滚数据库）

```bash
# 7.1 先回滚后端/前端到上一版产物（按你的备份路径恢复）

# 7.2 再执行数据库回滚
RELEASE_DIR=2026-04-21-api-performance-indexes

ssh root@39.105.26.34 '
  cd /root/vehicle-recycle-system/backend-api/docs/prod-db-release/'"${RELEASE_DIR}"' &&
  export $(grep DB_PASSWORD /etc/backend-api/backend-api.env | xargs) &&
  docker exec -i mysql-prod mysql -uroot -p"$DB_PASSWORD" scrap_system < rollback.sql &&
  unset DB_PASSWORD
'
```

## 生产数据库变更标准流程

后续所有生产数据库结构变更，建议统一按“预检查 -> 迁移 -> 发布 -> 验证 -> 可回滚”执行。
标准流程文档与脚本模板见 [backend-api/docs/prod-db-release/README.md](backend-api/docs/prod-db-release/README.md)。

当前这次接口性能相关的线上改表脚本位于：

- `backend-api/docs/prod-db-release/2026-04-21-api-performance-indexes/README.md`
- `backend-api/docs/prod-db-release/2026-04-21-api-performance-indexes/precheck.sql`
- `backend-api/docs/prod-db-release/2026-04-21-api-performance-indexes/migrate.sql`
- `backend-api/docs/prod-db-release/2026-04-21-api-performance-indexes/postcheck.sql`
- `backend-api/docs/prod-db-release/2026-04-21-api-performance-indexes/rollback.sql`

## 生产环境变量与 OSS AccessKey 切换
