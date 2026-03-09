## 生产发版（数据库 + 后端 + 前端）

以下命令基于当前目录结构与服务器路径：
- 服务器：`root@39.105.26.34`
- 前端发布目录：`/var/www/html/admin/`
- 后端 Jar 路径：`/root/backend-prod.jar`
- 后端服务名：`backend-api`
- MySQL 容器名：`mysql-prod`
- 生产库名：`scrap_system`

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
ssh root@39.105.26.34 "mkdir -p /root/vehicle-recycle-system/backend-api/docs/prod-db-release"

scp -r /Users/kkkfcc/Desktop/vehicle-recycle-system/backend-api/docs/prod-db-release/2026-03-09-material-template-scope \
  root@39.105.26.34:/root/vehicle-recycle-system/backend-api/docs/prod-db-release/
```

### 4) 服务器执行数据库预检查与迁移（Docker MySQL）

**安全提示**：使用 `export` 临时注入密码，避免明文留痕。

```bash
ssh root@39.105.26.34
cd /root/vehicle-recycle-system/backend-api/docs/prod-db-release/2026-03-09-material-template-scope

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
ssh root@39.105.26.34 '
  cd /root/vehicle-recycle-system/backend-api/docs/prod-db-release/2026-03-09-material-template-scope &&
  export $(grep DB_PASSWORD /etc/backend-api/backend-api.env | xargs) &&
  docker exec -i mysql-prod mysql -uroot -p"$DB_PASSWORD" scrap_system < rollback.sql &&
  unset DB_PASSWORD
'
```

## 生产数据库变更标准流程

后续所有生产数据库结构变更，建议统一按“预检查 -> 迁移 -> 发布 -> 验证 -> 可回滚”执行。
标准流程文档与脚本模板见：[prod-db-release/README.md](file:///Users/kkkfcc/Desktop/vehicle-recycle-system/backend-api/docs/prod-db-release/README.md)

## 生产环境变量与 OSS AccessKey 切换
