# 云脑诊疗平台药房端

独立的药房 Web 工作端，采用 Vue 3、TypeScript、Vite、Element Plus、Axios、Vue Router 和 Pinia。

## 功能

- 药房人员 JWT 登录与路由权限控制
- 药房数据总览和优先发药队列
- 处方详情、四项核对、确认发药
- 已发药记录追溯
- 药品库存查询、低库存提示和入库登记
- 个人中心与密码修改

## 运行

```bash
npm install
npm run dev
```

访问 `http://localhost:5175`，后端需运行在 `http://localhost:8080`。

测试账号：`pharmacy01` / `123456`
