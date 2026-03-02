# 后端管理后台（Vue）

本项目用于管理：
- 车型信息（新增/编辑/删除/分页搜索）
- 车型关联（导入外部车型版本库并半自动关联通用名称）
- 材料价格（查看与手工修正）
- 估值方式（材料配比模板维护）
- 用户账号（创建/禁用/重置密码/角色分配）

## 本地开发

先启动后端（默认 `http://localhost:8090`），再启动管理端：

```bash
npm install
npm run dev
```

浏览器访问：`http://localhost:5174/`

开发态已通过 Vite proxy 将 `/api` 代理到后端 `http://localhost:8090`（见 [vite.config.ts](file:///Users/kkkfcc/Desktop/vehicle-recycle-system/admin-web/vite.config.ts)）。

## 登录

后端默认会初始化一个管理账号（非 test 环境）：
- 用户名：`fcc`（可通过后端环境变量 `ADMIN_USERNAME` 覆盖）
- 密码：`12345`（开发/本地默认；生产环境请通过环境变量 `ADMIN_PASSWORD` 显式设置）

管理端登录后会把 JWT 存在浏览器本地存储（key：`admin_token`），并在所有请求里自动携带 `Authorization: Bearer <token>`。

## 依赖

- Vue 3 + TypeScript
- Vue Router
- Pinia
- Element Plus

## 对接接口

- 车型管理：`/api/admin/vehicles`
- 材料价格：`/api/material-prices`
- 估值方式（模板）：`/api/material-templates`
- 抓取记录：`/api/admin/job-runs`
- 用户管理：`/api/admin/users`
- 车型关联：`/api/admin/vehicle-mappings`、`/api/admin/external-trims`
