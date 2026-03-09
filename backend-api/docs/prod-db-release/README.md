# 生产数据库变更标准流程

所有生产环境数据库变更（DDL/DML），必须遵循本流程以确保可控、可验证、可回滚。

## 目录结构规范

所有变更必须在 `prod-db-release/` 下按日期创建子目录：

```
prod-db-release/
├── README.md                   # 本文件（标准流程指南）
└── 2026-03-09-feature-name/    # 某次具体变更
    ├── README.md               # 该次变更的具体说明（目标、风险、操作步骤）
    ├── precheck.sql            # 变更前检查脚本（只读）
    ├── migrate.sql             # 变更脚本（DDL + DML）
    ├── postcheck.sql           # 变更后验证脚本（只读）
    └── rollback.sql            # 回滚脚本（恢复到变更前状态）
```

## 变更文件要求

1. **precheck.sql**
   - 必须是只读查询。
   - 用于检查脏数据（如：唯一约束冲突、空值风险）。
   - 必须在执行 migrate 前运行并通过。

2. **migrate.sql**
   - 包含所有表结构变更和数据迁移逻辑。
   - 建议使用 `IF EXISTS` / `IF NOT EXISTS` 保证幂等性（若可能）。

3. **postcheck.sql**
   - 必须是只读查询。
   - 用于验证新字段是否创建、数据是否正确回填。

4. **rollback.sql**
   - 用于将数据库恢复到 migrate 前的状态。
   - **注意**：涉及 `DROP COLUMN` 的回滚会导致新写入的数据丢失，需在 README 中评估业务影响。

## 标准执行步骤（Docker 环境）

以下命令假设已 SSH 到服务器，并进入了具体的变更目录（如 `.../2026-03-09-xxx`）。

### 1. 准备环境与备份

```bash
# 临时加载数据库密码（避免明文）
export $(grep DB_PASSWORD /etc/backend-api/backend-api.env | xargs)

# 全库备份（必须步骤）
mkdir -p /root/db-backup
docker exec mysql-prod sh -c \
  'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --quick --routines --triggers --databases scrap_system' \
  > /root/db-backup/scrap_system_$(date +%F_%H%M%S).sql
gzip -9 /root/db-backup/scrap_system_*.sql
```

### 2. 预检查（Pre-check）

```bash
docker exec -i mysql-prod mysql -uroot -p"$DB_PASSWORD" scrap_system < precheck.sql
```
> **通过标准**：脚本输出必须符合预期（如：count=0）。若有脏数据，必须先修复，严禁强行迁移。

### 3. 执行迁移（Migrate）

```bash
docker exec -i mysql-prod mysql -uroot -p"$DB_PASSWORD" scrap_system < migrate.sql
```

### 4. 后检查（Post-check）

```bash
docker exec -i mysql-prod mysql -uroot -p"$DB_PASSWORD" scrap_system < postcheck.sql
```
> **通过标准**：新列/新表已存在，数据行数与预期一致。

### 5. 清理环境

```bash
unset DB_PASSWORD
```

## 回滚流程（仅在迁移失败或应用发布失败时执行）

1. 先回滚应用代码（后端/前端）。
2. 执行数据库回滚脚本：
   ```bash
   export $(grep DB_PASSWORD /etc/backend-api/backend-api.env | xargs)
   docker exec -i mysql-prod mysql -uroot -p"$DB_PASSWORD" scrap_system < rollback.sql
   unset DB_PASSWORD
   ```
3. 若 rollback 脚本也失败，使用步骤 1 的全库备份进行恢复。
