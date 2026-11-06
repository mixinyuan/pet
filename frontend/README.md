# PawBites 前端（Vue 3）

商城 + 管理后台一体的 SPA，与 `../backend`（Spring Boot 3）通过 REST API 通信，前后端分离。

## 技术栈

| 层 | 选型 |
| --- | --- |
| 框架 | Vue 3.5（Composition API + `<script setup>`） |
| 构建 | Vite 8 + TypeScript |
| 路由 / 状态 | Vue Router 4（懒加载 + 守卫）/ Pinia |
| UI | Element Plus（管理台）/ 自定义样式（商城） |
| HTTP | Axios（统一拦截器） |

## 启动

```bash
npm install     # 首次
npm run dev     # http://localhost:5173，/api 由 Vite proxy 转发到 localhost:8080
```

需要先启动后端：`../backend` 目录 `mvn spring-boot:run`（端口 8080）。

默认页面：

- 商城：http://localhost:5173/
- 登录/注册：http://localhost:5173/login
- 管理后台登录门（仅 ADMIN）：http://localhost:5173/admin/login

## 目录结构

```
src/
├── api/            # 接口模块：http.ts 为 Axios 实例（JWT/X-Guest-Id 拦截器、401 统一处理）
├── layouts/        # ShopLayout（商城顶栏）/ AdminLayout（管理台侧边栏）
├── router/         # 路由与守卫（requiresAuth / requiresAdmin）
├── stores/         # Pinia：auth（token/user 唯一来源，持久化 localStorage）
├── styles/         # 全局基础样式与主题变量
├── types/          # 与后端 ApiResponse / VO 对齐的 TS 类型
├── utils/          # guest.ts：游客 X-Guest-Id（沿用旧静态页存储键，购物车无缝衔接）
└── views/
    ├── shop/       # 商城页面（迁移自根目录 index.html / script.js）
    └── admin/      # 管理台模块（迁移自 admin-console*.js）
```

## 与后端的契约

- 统一响应体 `{ code, message, data, timestamp }`，`code=0` 成功，拦截器自动解包
- 登录态：`Authorization: Bearer <token>`；游客购物车/订单：`X-Guest-Id` 头
- 管理台接口前缀 `/api/admin/**`，仅 ADMIN 角色；游客可浏览商品/分类/套餐/评价

## 迁移进度

- [x] 骨架：路由守卫、登录态、两套 layout、接口层
- [x] 数据看板（接 `/api/admin/dashboard` 真实数据）
- [x] 商城：分类 / 商品 / 套餐 / 评价 / 购物车 / 下单 / 订单 / 收藏 / 优惠券 / 个人中心
- [x] 管理台：商品（含分类/标签）/ 套餐 / 订单 / 用户 / 评价 / 优惠券 / 日志
- [x] 旧静态页（根目录 *.html/js/css）已删除，frontend/ 为唯一前端（2026-11 经真实浏览器回归：管理台登录、八模块跳转、商城全区块数据均正常）

## 构建与部署

```bash
npm run build     # 产物 dist/，vue-tsc 类型检查 + Vite 打包
npm run preview   # 本地预览生产构建
```

生产部署：Nginx 托管 `dist/` 静态文件，`/api` 反向代理到 Spring Boot（参考 `vite.config.ts` 的 proxy 配置）。
