# 云脑诊疗平台管理端

基于 Vue 3、TypeScript、Vite、Element Plus、Vue Router、Pinia 与 Axios 开发的后台管理前端。

## 功能

- 管理员 JWT 登录、退出与路由权限控制
- 数据总览与资源统计
- 科室新增、编辑、删除
- 医生查询、新增、重置密码、删除
- 药品查询、新增、编辑、库存调整、删除
- 管理员个人信息与密码修改
- Axios 请求/响应拦截器、统一错误提示
- Vite 开发代理（`/api`、`/upload` → `http://localhost:8080`）

## 启动

```bash
npm install
npm run dev
```

浏览器访问 `http://localhost:5174`。启动前请确保后端服务运行在 `http://localhost:8080`。

## 构建

```bash
npm run build
```

生产文件输出到 `dist` 目录。
