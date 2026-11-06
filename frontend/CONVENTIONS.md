# PawBites 前端迁移约定（子代理必读）

所有迁移工作遵循以下规范。修改代码前先读完本文件。

## 背景与角色

- 本项目（`frontend/`）是 Vue 3 + TS + Vite + Pinia + Vue Router + Element Plus 的 SPA，
  替换仓库根目录的旧静态页（`index.html` / `script.js` / `styles.css` / `admin-console*.js` / `admin*.js`）。
- 旧静态页与 `backend/` 目录是**只读参考**：只准读，禁止改动或删除。
- 后端本机可能因内存不足无法启动：验证手段是 `npm run build`（vue-tsc 类型检查 + 打包），不要尝试启动后端。

## 技术约定

1. **接口层**：所有请求走 `@/api/http.ts` 的 `get/post/put/patch/del` 封装（已自动解包 `ApiResponse.data`、
   携带 `Authorization` 与 `X-Guest-Id`、统一 401 跳转）。按后端 Controller 一域一文件建 `src/api/<domain>.ts`。
2. **类型**：与后端 VO/DTO 字段逐一对齐（读 `backend/src/main/java/com/pawbites/petshop/vo|dto` 确认）。
   公共类型在 `src/types/api.ts`；商城 VO 追加到 `src/types/shop.ts`，管理台 VO 追加到 `src/types/admin.ts`（不存在则创建）。
   后端 `Long` → `number`，`BigDecimal` → `number`，`LocalDateTime` → `string`，可空对象字段 → `| null`。
3. **路由**：全部懒加载，注册进 `src/router/index.ts`。商城页面挂 `/` 的 ShopLayout children；
   管理台页面替换 `/admin/*` 下对应 `AdminPlaceholderView` 占位（保留 `meta.title`）。
4. **状态**：跨页面共享状态用 Pinia（`src/stores/`），如 cart；页面内状态用 `ref/reactive`。
5. **UI**：管理台用 Element Plus（全局已注册，无需 import 组件；图标 `import { X } from '@element-plus/icons-vue'`）。
   商城复用旧视觉：全局样式 `src/styles/shop.css`（由旧 `styles.css` 复制而来，已在 ShopLayout 引入），
   新页面尽量沿用旧 class 名；确实缺的样式追加到 `shop.css` 或用 scoped 补充。
6. **登录态**：只用 `useAuthStore()`（`@/stores/auth`），不要直接读 localStorage。游客判断 `!auth.isLoggedIn`。
7. **错误提示**：接口报错已由拦截器统一 ElMessage 弹出，视图层 catch 后只需处理收尾（如停止 loading），不要重复弹。
8. **代码风格**：与现有文件一致——`<script setup lang="ts">` 置顶、中文注释说明模块职责、
   组合式 API、文件顶部一段块注释。不要引入新依赖。
9. **共享文件**（`router/index.ts`、`types/*.ts`、`api/http.ts`、`App.vue`、`main.ts`）：只追加/扩展，不删除他人代码。
10. **验收**：完成后必须 `cd E:/pet/frontend && npm run build` 通过（无 TS 错误）才算完成。

## 后端接口速查

- 统一响应 `{ code, message, data, timestamp }`，`code=0` 成功。
- 公开只读：`GET /api/categories|products|bundles/reviews/**`；`POST /api/subscriptions`。
- 游客可用：`/api/cart/**`、`/api/orders/**`（X-Guest-Id 关联）。
- 需登录：`/api/favorites/**`、`/api/coupons/**`、`/api/auth/me`。
- 管理台：`/api/admin/**`（products/categories/tags/bundles/orders/users/reviews/coupon-templates/logs/dashboard）。
- 具体参数与返回结构以 `backend/.../controller` 与 `vo|dto` 为准，不确定就读源码，不要猜。
