"use strict";

/* ============================================================
 * PawBites 后台管理控制台（admin-console.html 配套脚本）· FE-A
 * ------------------------------------------------------------
 * 职责：登录门（仅 ADMIN）/ 侧边栏导航 / 数据看板 / 商品管理
 * （商品列表 + 分类 + 标签三个子标签页）/ 场景套餐管理。
 * 基础设施（apiRequest / escapeHtml / toast / modal / confirm /
 * 分页 / Switch 乐观更新）沿 admin.js 的既定写法。
 *
 * ★ FE-B 扩展方法（三步接入订单/用户/评价/优惠券/日志模块）★
 *
 * 1) 新增脚本文件（或直接在本文件之后追加 <script>），调用：
 *
 *      PawAdmin.registerModule({
 *        id: "orders",            // 与占位模块同 id，覆盖后即刻生效
 *        title: "订单管理",
 *        order: 40,               // 导航排序（越小越靠前）
 *        icon: "cart",            // 图标 key，见 ICONS 表（可自行加 key）
 *        mount(container, ctx) {  // 切换到该模块时调用；container=内容区
 *          // ctx 提供：api / escapeHtml / money / toast / showModal /
 *          // closeModal / confirmBox / onAuthFailure / onAction / icons 等
 *        }
 *      });
 *
 *    FE-A 已为 orders/users/reviews/coupons/logs 注册占位模块
 *    （点击导航显示「该模块建设中（FE-B 交付）」），后注册的同 id
 *    定义直接覆盖占位，导航自动重渲染。
 *
 * 2) 事件：内容区与弹窗统一走根容器 data-action 委托。FE-B 用
 *      PawAdmin.onAction("order-cancel", (btn, event) => { ... });
 *    注册新处理器；渲染时给按钮加 data-action="order-cancel" 即可。
 *    复杂交互也可在 mount 后对 container 自行 addEventListener。
 *
 * 3) 模块间互调：模块定义对象存于 PawAdmin.modules（Map），可挂
 *    自定义方法。例：看板「去补库存」即调用
 *      PawAdmin.modules.get("products").openEdit(productId)
 *    拉起商品编辑弹窗。
 *
 * 接口契约（与后端 controller/admin 一致）：
 *   /auth/login、/auth/me；/admin/dashboard；
 *   /admin/products（分页 records/total/page/size）；
 *   /admin/categories、/admin/tags（全量列表，带 productCount）；
 *   /admin/bundles（分页 records/total/page/size，含 items 子表）。
 * ============================================================ */

/* ---------------- 常量 ---------------- */

const API_BASE_URL = localStorage.getItem("pawbites-api-base") || "http://localhost:8080/api";
const AUTH_STORAGE_KEY = "pawbites-auth";
const PAGE_SIZE = 10; // 与日志管理一致：10 条/页
/** 商品/套餐列表下拉每页最大条数（后端 size clamp 到 [1,100]） */
const OPTION_FETCH_SIZE = 100;

/** 缩略图加载失败时的占位色块调色板 */
const THUMB_COLORS = [
  "#7f9cf5", "#68d391", "#f6ad55", "#f687b3", "#63b3ed", "#f6e05e",
  "#b794f4", "#4fd1c5", "#fc8181", "#9ae6b4", "#fbd38d", "#90cdf4",
  "#ecc94b", "#fed7d7",
];

/* ---------------- 基础工具 ---------------- */

function readStoredValue(key, fallbackValue = null) {
  try {
    const value = localStorage.getItem(key);
    return value ? JSON.parse(value) : fallbackValue;
  } catch {
    return fallbackValue;
  }
}

function writeStoredValue(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch {
    // 隐私模式下 localStorage 可能不可用，忽略
  }
}

function removeStoredValue(key) {
  try {
    localStorage.removeItem(key);
  } catch {
    // 忽略
  }
}

/** HTML 转义（所有动态文本输出前必过） */
function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

/** 金额格式化：¥xx.xx */
function money(value) {
  const num = Number(value);
  return "¥" + (Number.isFinite(num) ? num : 0).toFixed(2);
}

/** LocalDateTime 展示格式化：去掉 ISO 的 T / 毫秒 / 时区后缀 */
function fmtTime(value) {
  if (value == null || value === "") return "—";
  return String(value).replace("T", " ").replace(/\.\d+$/, "").replace(/([+-]\d{2}:\d{2}|Z)$/, "");
}

/** 取错误文案（网络异常在 apiRequest 里已转友好提示） */
function errText(error, fallback = "操作失败，请稍后重试。") {
  return (error && error.message) || fallback;
}

/** 是否登录失效（业务码或 HTTP 状态 401） */
function isAuthError(error) {
  return error && (error.code === 401 || error.status === 401);
}

/* ---------------- 内联图标（无外部依赖，替代原型的 lucide） ---------------- */

const ICONS = {
  dashboard: '<rect x="3" y="3" width="7" height="9" rx="1"/><rect x="14" y="3" width="7" height="5" rx="1"/><rect x="14" y="12" width="7" height="9" rx="1"/><rect x="3" y="16" width="7" height="5" rx="1"/>',
  package: '<path d="M21 8l-9-5-9 5 9 5 9-5z"/><path d="M3 8v8l9 5 9-5V8"/><path d="M12 13v8"/>',
  gift: '<rect x="3" y="8" width="18" height="4" rx="1"/><path d="M5 12v8a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-8"/><path d="M12 8v13"/><path d="M12 8c-1.5 0-4-.7-4-2.7C8 3.9 9.3 3 10.2 3c1.6 0 1.8 3.2 1.8 5z"/><path d="M12 8c1.5 0 4-.7 4-2.7C16 3.9 14.7 3 13.8 3c-1.6 0-1.8 3.2-1.8 5z"/>',
  cart: '<circle cx="9" cy="20" r="1.6"/><circle cx="17" cy="20" r="1.6"/><path d="M3 3h2l2.6 12.4a1 1 0 0 0 1 .8h8.8a1 1 0 0 0 1-.8L20 7H6"/>',
  users: '<circle cx="9" cy="8" r="3.5"/><path d="M2.5 20c.8-3.2 3.4-5 6.5-5s5.7 1.8 6.5 5"/><circle cx="17.5" cy="9" r="2.6"/><path d="M16.4 14.5c2.5.3 4.4 2 5 4.5"/>',
  star: '<path d="M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1-5.4-2.9-5.4 2.9 1-6.1L3.2 9.5l6.1-.9z"/>',
  ticket: '<path d="M3 9V7a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v2a3 3 0 0 0 0 6v2a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-2a3 3 0 0 0 0-6z"/><path d="M13 5v2"/><path d="M13 11v2"/><path d="M13 17v2"/>',
  "file-text": '<path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z"/><path d="M14 3v5h5"/><path d="M9 13h6"/><path d="M9 17h6"/>',
  search: '<circle cx="11" cy="11" r="7"/><path d="M20 20l-4-4"/>',
  plus: '<path d="M12 5v14"/><path d="M5 12h14"/>',
  x: '<path d="M18 6L6 18"/><path d="M6 6l12 12"/>',
  logout: '<path d="M9 21H6a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h3"/><path d="M16 17l5-5-5-5"/><path d="M21 12H9"/>',
  "external-link": '<path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><path d="M15 3h6v6"/><path d="M10 14L21 3"/>',
  warn: '<path d="M10.3 3.9L1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z"/><path d="M12 9v4"/><path d="M12 17h.01"/>',
  check: '<path d="M20 6L9 17l-5-5"/>',
  info: '<circle cx="12" cy="12" r="9"/><path d="M12 16v-5"/><path d="M12 8h.01"/>',
  wallet: '<rect x="2" y="6" width="20" height="12" rx="2"/><circle cx="12" cy="12" r="2.5"/>',
};

/** 生成内联 SVG 图标（找不到 key 时回退 info） */
function icon(name) {
  const body = ICONS[name] || ICONS.info;
  return '<svg class="pa-ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" ' +
    'stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' + body + "</svg>";
}

/* ---------------- 页面元素引用 ---------------- */

const rootEl = document.getElementById("pa-console");
const gateEl = document.getElementById("paGate");
const gateMsgEl = document.getElementById("paGateMsg");
const loginForm = document.getElementById("paLoginForm");
const loginUserInput = document.getElementById("paLoginUser");
const loginPwdInput = document.getElementById("paLoginPwd");
const loginButton = document.getElementById("paLoginBtn");
const shellEl = document.getElementById("paShell");
const navEl = document.getElementById("paNav");
const crumbEl = document.getElementById("paCrumb");
const accountNameEl = document.getElementById("paAccountName");
const avatarEl = document.getElementById("paAvatar");
const contentEl = document.getElementById("paContent");
const overlay = document.getElementById("paOverlay");
const modalEl = document.getElementById("paModal");
const toastsEl = document.getElementById("paToasts");
const logoutButton = document.querySelector('#pa-console [data-action="logout"]');

/* ---------------- 全局状态 ---------------- */

/** 当前登录态（{mode,token,user,displayName}），未登录为 null */
let authProfile = null;
const state = { ready: false, view: null };

/** 各列表模块的筛选/分页状态（翻页保留条件） */
const ui = {
  products: { subtab: "list", q: "", category: "all", status: "all", page: 1, size: PAGE_SIZE, rows: [], total: 0, totalPages: 1 },
  bundles: { q: "", status: "all", page: 1, size: PAGE_SIZE, rows: [], total: 0, totalPages: 1 },
};

/** 分类/标签引用数据缓存（商品表单 chips 与列表 pills 需要 code/name 映射） */
const refData = { categories: [], tags: [] };
let refPromise = null;

/** 弹窗编辑上下文：{kind, id?, cats:Set, tags:Set, items:[], products:[]} */
let editCtx = null;
/** confirmBox 确认回调 */
let onConfirmOk = null;
/** 弹窗打开前的焦点元素，关闭后归还 */
let modalReturnFocus = null;

/* ---------------- 请求封装（沿用 admin.js 写法） ---------------- */

async function apiRequest(path, options = {}) {
  const headers = {
    "Content-Type": "application/json",
    ...(options.headers || {}),
  };
  if (authProfile?.token) {
    headers.Authorization = `Bearer ${authProfile.token}`;
  }

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
    });
  } catch (error) {
    throw Object.assign(new Error("无法连接后端服务，请确认 Spring Boot 已启动。"), { network: true });
  }

  const payload = await response.json().catch(() => null);
  if (!response.ok || payload?.code !== 0) {
    throw Object.assign(new Error(payload?.message || `请求失败：${response.status}`), {
      code: payload?.code,
      status: response.status,
    });
  }
  return payload.data;
}

/** 统一错误处理：401 回登录门，其余 toast 后端文案 */
function handleApiError(error, fallback = "操作失败，请稍后重试。") {
  if (isAuthError(error)) {
    handleAuthFailure("登录状态已失效，请重新登录。");
    return;
  }
  showToast(errText(error, fallback), "err");
}

/* ---------------- toast（堆叠式，样式同原型） ---------------- */

function showToast(message, type = "ok") {
  const item = document.createElement("div");
  item.className = "pa-toast " + (type || "ok");
  const iconName = type === "err" ? "warn" : type === "warn" ? "info" : "check";
  item.innerHTML = icon(iconName) + "<span>" + escapeHtml(message) + "</span>";
  toastsEl.appendChild(item);
  setTimeout(() => {
    item.style.opacity = "0";
    item.style.transition = "opacity .3s";
    setTimeout(() => item.remove(), 320);
  }, 2600);
}

/* ---------------- 弹窗 / 确认框 ---------------- */

function showModal(size, title, bodyHtml, footHtml) {
  modalEl.className = "pa-modal" + (size ? " " + size : "");
  modalEl.innerHTML =
    '<div class="pa-modal-hd">' + escapeHtml(title) +
    '<button class="pa-iconbtn pa-x" type="button" data-action="modal-close" aria-label="关闭">' + icon("x") + "</button></div>" +
    '<div class="pa-modal-bd">' + bodyHtml + "</div>" +
    (footHtml ? '<div class="pa-modal-ft">' + footHtml + "</div>" : "");
  modalReturnFocus = document.activeElement;
  overlay.hidden = false;
  const first = modalEl.querySelector("input,select,textarea,button:not(.pa-x)");
  if (first instanceof HTMLElement) first.focus();
}

function closeModal() {
  overlay.hidden = true;
  modalEl.innerHTML = "";
  editCtx = null;
  onConfirmOk = null;
  if (modalReturnFocus instanceof HTMLElement) {
    modalReturnFocus.focus();
    modalReturnFocus = null;
  }
}

/** 危险操作二次确认（红色按钮）。messageHtml 为可信 HTML，动态内容由调用方 escapeHtml */
function confirmBox(title, messageHtml, okText, onOk) {
  onConfirmOk = onOk;
  showModal(
    "sm",
    "",
    '<div class="pa-confirm-ico">' + icon("warn") + "</div>" +
    '<div class="pa-confirm-t">' + escapeHtml(title) + "</div>" +
    '<div class="pa-confirm-m">' + messageHtml + "</div>",
    '<button class="pa-btn" type="button" data-action="modal-close">取消</button>' +
    '<button class="pa-btn pa-btn-danger" type="button" data-action="confirm-ok">' + escapeHtml(okText || "确认") + "</button>"
  );
  const okBtn = modalEl.querySelector('[data-action="confirm-ok"]');
  if (okBtn instanceof HTMLElement) okBtn.focus();
}

/* ---------------- 登录门 ---------------- */

function showGate(message) {
  shellEl.hidden = true;
  gateEl.hidden = false;
  gateMsgEl.textContent = message || "";
}

function handleAuthFailure(message) {
  authProfile = null;
  removeStoredValue(AUTH_STORAGE_KEY);
  closeModal();
  showGate(message);
}

/** 登录表单提交：必须 ADMIN 角色，非管理员不写入共享登录态 */
async function doLogin() {
  const username = loginUserInput.value.trim();
  const password = loginPwdInput.value;
  if (!username) {
    gateMsgEl.textContent = "请输入管理员账号。";
    loginUserInput.focus();
    return;
  }
  if (!password) {
    gateMsgEl.textContent = "请输入密码。";
    loginPwdInput.focus();
    return;
  }

  loginButton.disabled = true;
  loginButton.textContent = "登录中…";
  gateMsgEl.textContent = "";

  try {
    const data = await apiRequest("/auth/login", { method: "POST", body: { username, password } });
    const role = data?.role || data?.user?.role;
    if (role !== "ADMIN") {
      // 登录成功但不是管理员：不覆盖 pawbites-auth（避免顶掉 C 端登录态）
      gateMsgEl.textContent = "该账号无管理员权限。";
      return;
    }
    const user = data.user || {};
    authProfile = {
      mode: "user",
      token: data.token,
      user,
      displayName: user.nickname || user.username || "管理员",
    };
    writeStoredValue(AUTH_STORAGE_KEY, authProfile);
    enterConsole();
    showToast("欢迎回来，" + authProfile.displayName);
  } catch (error) {
    if (error.network) {
      gateMsgEl.textContent = errText(error);
    } else if (isAuthError(error)) {
      removeStoredValue(AUTH_STORAGE_KEY); // 401：清共享登录态回登录门
      gateMsgEl.textContent = error.message || "账号或密码错误。";
    } else {
      gateMsgEl.textContent = errText(error);
    }
  } finally {
    loginButton.disabled = false;
    loginButton.textContent = "登 录";
  }
}

/** 恢复登录态：读 pawbites-auth → /auth/me 校验管理员角色 */
async function initAuth() {
  const saved = readStoredValue(AUTH_STORAGE_KEY);

  if (!saved || saved.mode !== "user" || !saved.token) {
    showGate(saved?.mode === "guest"
      ? "后台需要管理员账号登录，游客模式无法访问。"
      : "请输入管理员账号登录。");
    return;
  }

  authProfile = saved;
  try {
    const user = await apiRequest("/auth/me");
    if (user?.role !== "ADMIN") {
      // 非管理员保留 C 端登录态，仅拦截后台
      showGate("该账号无管理员权限，请使用管理员账号登录。");
      return;
    }
    authProfile = { ...saved, user, displayName: user.nickname || user.username || "管理员" };
    enterConsole();
  } catch (error) {
    if (isAuthError(error)) {
      handleAuthFailure("登录状态已失效，请重新登录。");
      return;
    }
    showGate("无法确认登录状态：" + errText(error));
  }
}

function enterConsole() {
  gateEl.hidden = true;
  shellEl.hidden = false;
  accountNameEl.textContent = authProfile?.displayName || "管理员";
  avatarEl.textContent = (authProfile?.displayName || "管").slice(0, 1);
  state.ready = true;
  renderNav();
  switchTo("dashboard");
}

function doLogout() {
  authProfile = null;
  removeStoredValue(AUTH_STORAGE_KEY);
  closeModal();
  contentEl.innerHTML = "";
  state.ready = false;
  showGate("已退出登录，请重新登录。");
}

/* ---------------- 模块注册器（FE-B 扩展点） ---------------- */

/** data-action → 处理器表；FE-B 通过 PawAdmin.onAction 追加 */
const actionHandlers = Object.create(null);

function onAction(name, handler) {
  if (typeof name === "string" && typeof handler === "function") {
    actionHandlers[name] = handler;
  }
}

const PawAdmin = {
  /** 已注册模块（Map<id, def>），def 可挂自定义方法供模块间互调 */
  modules: new Map(),
  /** 注册（或按 id 覆盖）一个模块；导航自动重渲染 */
  registerModule(def) {
    if (!def || !def.id || typeof def.mount !== "function") return;
    this.modules.set(def.id, def);
    if (state.ready) renderNav();
  },
  /** 注册 data-action 处理器（事件委托） */
  onAction,
  /** 提供给模块 mount(container, ctx) 的基础设施 */
  ctx: {},
};
window.PawAdmin = PawAdmin;

function sortedModules() {
  return [...PawAdmin.modules.values()].sort((a, b) => (a.order ?? 99) - (b.order ?? 99));
}

function renderNav() {
  navEl.innerHTML = sortedModules()
    .map((m) =>
      '<button type="button" class="pa-nav-item' + (state.view === m.id ? " is-on" : "") +
      '" data-action="nav" data-view="' + escapeHtml(m.id) + '">' +
      icon(m.icon) + "<span>" + escapeHtml(m.title) + "</span>" +
      (m.placeholder ? '<span class="pa-nav-wip">建设中</span>' : "") +
      "</button>")
    .join("");
}

/** 切换模块：清空内容区并调用模块 mount */
function switchTo(viewId) {
  const def = PawAdmin.modules.get(viewId);
  if (!def) return;
  state.view = viewId;
  crumbEl.innerHTML = "首页 / <b>" + escapeHtml(def.title) + "</b>";
  document.title = def.title + " | PawBites 管理后台";
  renderNav();
  closeModal();
  contentEl.innerHTML = "";
  def.mount(contentEl, PawAdmin.ctx);
}

/* ---------------- 通用列表组件 ---------------- */

/** 下拉筛选框 */
function selBox(filter, options, current) {
  return '<select class="pa-sel" data-filter="' + escapeHtml(filter) + '">' +
    options.map((o) =>
      '<option value="' + escapeHtml(o.v) + '"' + (o.v === current ? " selected" : "") + ">" +
      escapeHtml(o.t) + "</option>").join("") +
    "</select>";
}

/** 关键字搜索框（回车触发 / data-action="search" 触发） */
function kwBox(scope, placeholder, value) {
  return '<span class="pa-kw">' +
    '<input class="pa-kw-inp" data-scope="' + escapeHtml(scope) + '" type="text" maxlength="64" ' +
    'value="' + escapeHtml(value || "") + '" placeholder="' + escapeHtml(placeholder) + '" ' +
    'aria-label="' + escapeHtml(placeholder) + '" autocomplete="off" />' +
    '<button class="pa-btn" type="button" data-action="search" data-scope="' + escapeHtml(scope) + '">' +
    icon("search") + "搜索</button></span>";
}

/** Switch 按钮（乐观更新，失败回滚见 makeSwitchToggle） */
function switchBtnHtml(on, action, id) {
  return '<button type="button" class="pa-switch" role="switch" aria-checked="' + (on ? "true" : "false") +
    '" data-action="' + escapeHtml(action) + '" data-id="' + escapeHtml(id) + '" aria-label="状态开关"></button>';
}

/** 生成页码序列：总页数 ≤ 7 全量展示，否则首尾页 + 当前页附近窗口 */
function buildPageNumbers(page, totalPages) {
  if (totalPages <= 7) {
    return Array.from({ length: totalPages }, (_, i) => i + 1);
  }
  const numbers = new Set([1, totalPages, page - 1, page, page + 1]);
  const sorted = [...numbers].filter((n) => n >= 1 && n <= totalPages).sort((a, b) => a - b);
  const result = [];
  sorted.forEach((n, i) => {
    if (i > 0 && n - sorted[i - 1] > 1) result.push("...");
    result.push(n);
  });
  return result;
}

/** 分页条 HTML（与原型同款：页码 / 上下页 / 跳页 / 总数） */
function pagerHtml(scope, page, totalPages, total) {
  const pages = Math.max(1, totalPages);
  const parts = [];
  parts.push('<button class="pa-pgbtn" type="button" data-action="page" data-scope="' + escapeHtml(scope) +
    '" data-p="prev"' + (page <= 1 ? " disabled" : "") + ' aria-label="上一页">‹</button>');
  buildPageNumbers(page, pages).forEach((n) => {
    if (n === "...") {
      parts.push('<span class="pa-pg-dots">…</span>');
      return;
    }
    parts.push('<button class="pa-pgbtn' + (n === page ? " is-on" : "") + '" type="button" data-action="page" ' +
      'data-scope="' + escapeHtml(scope) + '" data-p="' + n + '"' + (n === page ? ' aria-current="page"' : "") + ">" + n + "</button>");
  });
  parts.push('<button class="pa-pgbtn" type="button" data-action="page" data-scope="' + escapeHtml(scope) +
    '" data-p="next"' + (page >= pages ? " disabled" : "") + ' aria-label="下一页">›</button>');
  parts.push('<span class="pa-jump">前往<input id="jump-' + escapeHtml(scope) + '" value="' + page +
    '" inputmode="numeric" aria-label="跳转页码" />页' +
    '<button class="pa-pgbtn" type="button" data-action="jump" data-scope="' + escapeHtml(scope) + '">GO</button></span>');
  parts.push('<span class="pa-total">共 ' + total + " 条记录</span>");
  return '<div class="pa-pager">' + parts.join("") + "</div>";
}

/** 列表状态行：loading / empty / error（error 带重试按钮） */
function stateRowHtml(colspan, kind, message, scope) {
  if (kind === "error") {
    return '<tr class="pa-empty"><td colspan="' + colspan + '">' + inlineStateHtml(kind, message, scope) + "</td></tr>";
  }
  return '<tr class="pa-empty"><td colspan="' + colspan + '">' + escapeHtml(message) + "</td></tr>";
}

/** 非表格场景的状态块（看板等整卡加载态），error 带重试按钮 */
function inlineStateHtml(kind, message, scope) {
  if (kind === "error") {
    return '<div class="pa-state is-error"><span>' + escapeHtml(message) + "</span>" +
      '<button class="pa-btn" type="button" data-action="retry" data-scope="' + escapeHtml(scope) + '">重试</button></div>';
  }
  return '<div class="pa-state"><span>' + escapeHtml(message) + "</span></div>";
}

/** 订单状态 pill */
function statusPill(status) {
  return status === "CREATED"
    ? '<span class="pa-pill p-blue">已创建</span>'
    : '<span class="pa-pill p-gray">已取消</span>';
}

/** 缩略图：img 加载失败 onerror 换占位色块（首字） */
function thumbHtml(seedId, name, imageUrl) {
  const seed = Number(seedId ?? 0);
  const color = THUMB_COLORS[Math.abs(Number.isFinite(seed) ? seed : 0) % THUMB_COLORS.length];
  const label = escapeHtml(String(name || "").slice(0, 1) || "·");
  const html =
    '<span class="pa-thumb is-img" style="background:' + color + '">' +
    (imageUrl
      ? '<img src="' + escapeHtml(imageUrl) + '" alt="" loading="lazy" ' +
        'onerror="this.onerror=null;var w=this.closest(\'.pa-thumb\');if(w){w.classList.add(\'is-broken\');}this.remove();" />'
      : "") +
    '<span class="pa-thumb-fb">' + label + "</span></span>";
  return html;
}

/** 表单字段 HTML（id 前缀 f-；hint 为可信 HTML，动态内容调用方自行转义） */
function fi(label, id, value, hint, req, type, attrs) {
  return '<div class="pa-fi"><label>' + (req ? '<span class="req">*</span>' : "") + escapeHtml(label) + "</label>" +
    '<input id="f-' + escapeHtml(id) + '" type="' + (type || "text") + '" ' + (attrs || "") +
    ' value="' + escapeHtml(value ?? "") + '" />' +
    (hint ? '<span class="pa-hint">' + hint + "</span>" : "") + "</div>";
}

/** 表单取值（去空白） */
function formVal(id) {
  const el = document.getElementById("f-" + id);
  return el ? String(el.value).trim() : "";
}

/* ---------------- 引用数据（分类/标签） ---------------- */

/** 拉取并缓存全量分类/标签（force=true 强制刷新） */
function ensureRefData(force) {
  if (force || !refPromise) {
    refPromise = Promise.all([
      apiRequest("/admin/categories"),
      apiRequest("/admin/tags"),
    ])
      .then(([categories, tags]) => {
        refData.categories = Array.isArray(categories) ? categories : [];
        refData.tags = Array.isArray(tags) ? tags : [];
      })
      .catch((error) => {
        refPromise = null; // 失败后允许重试
        throw error;
      });
  }
  return refPromise;
}

/** 分类 code → 名称（未命中回退 code 本身） */
function catName(code) {
  const hit = refData.categories.find((c) => c.code === code);
  return hit ? hit.name : code;
}

/** 标签 id → 名称（未命中回退空串） */
function tagName(id) {
  const hit = refData.tags.find((t) => String(t.id) === String(id));
  return hit ? hit.name : "";
}

/* ============================================================
 * 模块：数据看板
 * ============================================================ */

async function loadDashboard() {
  contentEl.innerHTML = '<div class="pa-card"><div class="pa-card-hd"><h2>数据看板</h2><p>今日经营概况与待处理事项</p></div>' +
    inlineStateHtml("loading", "看板加载中…") + "</div>";
  try {
    const data = await apiRequest("/admin/dashboard");
    renderDashboard(data || {});
  } catch (error) {
    if (isAuthError(error)) {
      handleAuthFailure("登录状态已失效，请重新登录。");
      return;
    }
    contentEl.innerHTML = '<div class="pa-card"><div class="pa-card-hd"><h2>数据看板</h2></div>' +
      inlineStateHtml("error", "看板加载失败：" + errText(error), "dashboard") + "</div>";
  }
}

function statCard(iconName, label, value, deltaHtml, kind) {
  const colors = { order: "#409EFF", sales: "#67C23A", users: "#909399", warn: "#E6A23C" };
  return '<div class="pa-stat"><span class="pa-stat-ico" style="background:' + (colors[kind] || "#409EFF") + '">' +
    icon(iconName) + "</span><span><span class=\"pa-stat-label\">" + escapeHtml(label) + "</span><br>" +
    '<span class="pa-stat-value">' + value + "</span><br>" +
    '<span class="pa-stat-delta">' + deltaHtml + "</span></span></div>";
}

function renderDashboard(data) {
  const summary = data.summary || {};
  const recent = Array.isArray(data.recentOrders) ? data.recentOrders : [];
  const top = Array.isArray(data.topProducts) ? data.topProducts : [];
  const low = Array.isArray(data.lowStockProducts) ? data.lowStockProducts : [];

  // 今日订单较昨日差值（正绿负红，0 持平）
  const delta = Number(summary.todayOrderDelta || 0);
  const deltaHtml = delta === 0
    ? "与昨日持平"
    : '<b class="' + (delta > 0 ? "pa-up" : "pa-down") + '">' + (delta > 0 ? "+" : "") + delta + "</b> 较昨日";

  contentEl.innerHTML =
    '<div class="pa-statgrid">' +
    statCard("cart", "今日订单", String(summary.todayOrderCount ?? 0), deltaHtml, "order") +
    statCard("wallet", "今日销售额", money(summary.todaySalesAmount), "仅统计有效（已创建）订单", "sales") +
    statCard("users", "注册用户", String(summary.userTotal ?? 0), "含管理员 " + (summary.adminCount ?? 0) + " 名", "users") +
    statCard("warn", "库存预警", String(summary.lowStockCount ?? 0), "库存低于 20 件", "warn") +
    "</div>" +
    '<div class="pa-card"><div class="pa-card-hd"><h2>最近订单</h2><p>点击行可跳转订单管理并定位该订单</p></div>' +
    '<div class="pa-tblwrap"><table class="pa-tbl"><thead><tr><th>订单号</th><th>用户</th><th class="pa-num">件数</th>' +
    '<th class="pa-num">实付</th><th>状态</th><th>下单时间</th></tr></thead><tbody>' +
    (recent.length
      ? recent.map((o) =>
        '<tr class="pa-clickrow" data-action="goto-order" data-no="' + escapeHtml(o.orderNo) + '">' +
        '<td class="pa-mono">' + escapeHtml(o.orderNo) + "</td>" +
        "<td>" + escapeHtml(o.ownerName) + "</td>" +
        '<td class="pa-num">' + Number(o.itemCount || 0) + "</td>" +
        '<td class="pa-num">' + money(o.totalAmount) + "</td>" +
        "<td>" + statusPill(o.status) + "</td>" +
        "<td>" + escapeHtml(fmtTime(o.createdAt)) + "</td></tr>").join("")
      : stateRowHtml(6, "empty", "暂无订单")) +
    "</tbody></table></div></div>" +
    '<div class="pa-duo">' +
    '<div class="pa-card"><div class="pa-card-hd"><h2>热销 TOP 5</h2><p>按有效订单销量聚合</p></div>' +
    (top.length
      ? top.map((t, i) =>
        '<div class="pa-rank"><span class="pa-rank-no top' + (i + 1) + '">' + (i + 1) + "</span>" +
        thumbHtml(t.productId, t.productName, t.imageUrl) +
        '<span class="pa-rank-name" title="' + escapeHtml(t.productName) + '">' + escapeHtml(t.productName) + "</span>" +
        '<span class="pa-dim">销 ' + Number(t.soldQuantity || 0) + " 件</span>" +
        "<b>" + money(t.salesAmount) + "</b></div>").join("")
      : '<div class="pa-dim">暂无销量数据</div>') +
    "</div>" +
    '<div class="pa-card"><div class="pa-card-hd"><h2>库存预警</h2><p>库存 &lt; 20 件，点击可去补库存</p></div>' +
    (low.length
      ? low.map((p) =>
        '<div class="pa-rank">' + thumbHtml(p.productId, p.name, "") +
        '<span class="pa-rank-name" title="' + escapeHtml(p.name) + '">' + escapeHtml(p.name) + "</span>" +
        '<span class="pa-lowstock">仅剩 ' + Number(p.stock || 0) + ' 件</span>' +
        '<button class="pa-linkbtn" type="button" data-action="restock" data-id="' + escapeHtml(p.productId) + '">去补库存</button></div>').join("")
      : '<div class="pa-dim">暂无预警</div>') +
    "</div>" +
    "</div>";
}

/* ============================================================
 * 模块：商品管理（商品列表 / 分类管理 / 标签管理）
 * ============================================================ */

function productsSubtabsHtml(tab) {
  const tabs = [["list", "商品列表"], ["cats", "分类管理"], ["tags", "标签管理"]];
  return '<div class="pa-subtabs">' + tabs.map(([key, text]) =>
    '<button type="button" class="pa-subtab' + (tab === key ? " is-on" : "") +
    '" data-action="subtab" data-tab="' + key + '">' + text + "</button>").join("") + "</div>";
}

/** 按当前子标签页加载对应数据 */
function loadProductsSubtab() {
  const tab = ui.products.subtab;
  if (tab === "cats") loadCats();
  else if (tab === "tags") loadTags();
  else loadProducts();
}

/* ---------- 商品列表 ---------- */

function renderProductsCard(kind, message) {
  const u = ui.products;
  const catOptions = [{ v: "all", t: "全部分类" }].concat(
    refData.categories.filter((c) => c.code !== "all").map((c) => ({ v: c.code, t: c.name }))
  );

  contentEl.innerHTML = productsSubtabsHtml("list") +
    '<div class="pa-card"><div class="pa-toolbar">' +
    '<button class="pa-btn pa-btn-pri" type="button" data-action="new" data-kind="product">' + icon("plus") + "新增商品</button>" +
    selBox("p-cat", catOptions, u.category) +
    selBox("p-status", [{ v: "all", t: "全部状态" }, { v: "ACTIVE", t: "上架中" }, { v: "INACTIVE", t: "已下架" }], u.status) +
    '<span class="pa-sp"></span>' + kwBox("products", "名称 / SKU / 口味", u.q) +
    "</div>" +
    '<div class="pa-tblwrap"><table class="pa-tbl"><thead><tr><th>商品</th><th>分类</th><th>标签</th><th>适用宠物</th>' +
    '<th class="pa-num">价格</th><th class="pa-num">库存</th><th>评分</th><th>上架状态</th><th class="pa-nowrap">操作</th>' +
    "</tr></thead><tbody>" +
    (kind === "ok"
      ? u.rows.map((p) => productRowHtml(p)).join("")
      : stateRowHtml(9, kind, message, "products")) +
    "</tbody></table></div>" +
    (kind === "ok" ? pagerHtml("products", u.page, u.totalPages, u.total) : "") +
    "</div>";
}

function productRowHtml(p) {
  const cats = p.categories || [];
  const tags = p.tags || [];
  const catPills = cats.slice(0, 2).map((c) => '<span class="pa-pill p-blue">' + escapeHtml(catName(c)) + "</span>").join("") +
    (cats.length > 2 ? '<span class="pa-pill p-gray">+' + (cats.length - 2) + "</span>" : "");
  const tagPills = tags.slice(0, 2).map((t) => '<span class="pa-pill p-teal">' + escapeHtml(tagName(t)) + "</span>").join("") +
    (tags.length > 2 ? '<span class="pa-pill p-gray">+' + (tags.length - 2) + "</span>" : "");
  const stock = Number(p.stock ?? 0);
  const rating = p.rating == null || p.rating === "" ? "—" : '<span class="pa-stars">★</span> ' + Number(p.rating).toFixed(1);

  return "<tr>" +
    '<td><span class="pa-prod">' + thumbHtml(p.id, p.name, p.imageUrl) +
    "<span><span class=\"pa-cell-main\" title=\"" + escapeHtml(p.name) + '">' + escapeHtml(p.name) + "</span>" +
    '<div class="pa-cell-sub pa-mono">' + escapeHtml(p.sku) + "</div></span></span></td>" +
    "<td>" + (catPills || '<span class="pa-dim">—</span>') + "</td>" +
    "<td>" + (tagPills || '<span class="pa-dim">—</span>') + "</td>" +
    "<td>" + escapeHtml(p.petType) + "</td>" +
    '<td class="pa-num">' + money(p.price) + "</td>" +
    '<td class="pa-num' + (stock < 20 ? " pa-lowstock" : "") + '">' + stock + "</td>" +
    "<td>" + rating + "</td>" +
    "<td>" + switchBtnHtml(p.status === "ACTIVE", "sw-product", p.id) + "</td>" +
    '<td class="pa-nowrap">' +
    '<button class="pa-linkbtn" type="button" data-action="edit" data-kind="product" data-id="' + escapeHtml(p.id) + '">编辑</button>' +
    '<button class="pa-linkbtn is-danger" type="button" data-action="del" data-kind="product" data-id="' + escapeHtml(p.id) + '">删除</button>' +
    "</td></tr>";
}

async function loadProducts() {
  const u = ui.products;
  renderProductsCard("loading");
  // 分类/标签名映射（失败不阻塞列表，pills 回退显示 code / 空白）
  try {
    await ensureRefData();
  } catch (error) {
    if (isAuthError(error)) {
      handleAuthFailure("登录状态已失效，请重新登录。");
      return;
    }
  }

  const query = new URLSearchParams({ page: String(u.page), size: String(u.size) });
  const keyword = u.q.trim();
  if (keyword) query.set("keyword", keyword);
  if (u.category !== "all") query.set("category", u.category);
  if (u.status !== "all") query.set("status", u.status);

  try {
    const data = await apiRequest("/admin/products?" + query.toString());
    u.rows = Array.isArray(data?.records) ? data.records : [];
    u.total = Number(data?.total || 0);
    u.totalPages = Math.max(1, Math.ceil(u.total / u.size));
    const filtered = keyword || u.category !== "all" || u.status !== "all";
    renderProductsCard(u.rows.length ? "ok" : "empty", filtered ? "未找到匹配的商品" : "暂无商品，点击右上角「新增商品」创建");
  } catch (error) {
    if (isAuthError(error)) {
      handleAuthFailure("登录状态已失效，请重新登录。");
      return;
    }
    u.rows = [];
    u.total = 0;
    u.totalPages = 1;
    renderProductsCard("error", "加载失败：" + errText(error));
  }
}

/* ---------- 商品新增/编辑弹窗 ---------- */

function productModalHtml(p) {
  const pets = ["猫咪适用", "狗狗适用", "猫狗通用", "小型犬适用"];
  const catChips = refData.categories.filter((c) => c.code !== "all")
    .map((c) =>
      '<button type="button" class="pa-chip' + (editCtx.cats.has(c.code) ? " is-on" : "") +
      '" data-action="chip" data-g="cats" data-v="' + escapeHtml(c.code) + '">' + escapeHtml(c.name) + "</button>").join("");
  const tagChips = refData.tags
    .map((t) =>
      '<button type="button" class="pa-chip' + (editCtx.tags.has(Number(t.id)) ? " is-on" : "") +
      '" data-action="chip" data-g="tags" data-v="' + escapeHtml(t.id) + '">' + escapeHtml(t.name) + "</button>").join("");

  return '<div class="pa-form">' +
    fi("SKU 编码", "sku", p ? p.sku : "", "唯一，建议 PB-XXX-XXX，自动转大写", true, "text", 'maxlength="64" autocomplete="off"') +
    fi("商品名称", "name", p ? p.name : "", "", true, "text", "maxlength=\"100\"") +
    '<div class="pa-fi"><label><span class="req">*</span>适用宠物</label><select id="f-pet">' +
    pets.map((x) => '<option value="' + x + '"' + (p && p.petType === x ? " selected" : "") + ">" + x + "</option>").join("") +
    "</select></div>" +
    fi("口味", "flavor", p ? p.flavor : "", "如：鸡胸肉", true, "text", "maxlength=\"50\"") +
    fi("价格（元）", "price", p ? p.price : "", "&gt; 0，最多 8 位整数、2 位小数", true, "number", 'step="0.01" min="0.01" inputmode="decimal"') +
    fi("库存", "stock", p ? p.stock : "", "≥ 0 的整数", true, "number", 'step="1" min="0" inputmode="numeric"') +
    fi("评分", "rating", p ? p.rating : "5.0", "0.0 – 5.0，一位小数，缺省 5.0", false, "number", 'step="0.1" min="0" max="5" inputmode="decimal"') +
    fi("图片 URL", "img", p ? p.imageUrl : "", "必填，需以 http:// 或 https:// 开头", true, "text", 'maxlength="500" autocomplete="off"') +
    '<div class="pa-fi"><label>上架状态</label><select id="f-status">' +
    '<option value="ACTIVE"' + (!p || p.status === "ACTIVE" ? " selected" : "") + ">上架</option>" +
    '<option value="INACTIVE"' + (p && p.status === "INACTIVE" ? " selected" : "") + ">下架</option>" +
    "</select></div>" +
    '<div class="pa-fi full"><label><span class="req">*</span>所属分类（多选，至少 1 项，不含「全部」）</label>' +
    '<div class="pa-chips" id="f-cats">' + (catChips || '<span class="pa-dim">暂无分类，请先到「分类管理」创建</span>') + "</div></div>" +
    '<div class="pa-fi full"><label>标签（多选，可选）</label><div class="pa-chips" id="f-tags">' +
    (tagChips || '<span class="pa-dim">暂无标签，可到「标签管理」创建</span>') + "</div></div>" +
    '<div class="pa-fi full"><label>商品描述</label><textarea id="f-desc" maxlength="500" rows="4">' +
    escapeHtml(p ? p.description : "") + "</textarea></div>" +
    "</div>";
}

function openProductModal(p) {
  editCtx = {
    kind: "product",
    id: p ? p.id : null,
    cats: new Set(p ? p.categories || [] : []),
    tags: new Set((p ? p.tags || [] : []).map(Number)),
  };
  showModal(
    "",
    p ? "编辑商品" : "新增商品",
    productModalHtml(p),
    '<button class="pa-btn" type="button" data-action="modal-close">取消</button>' +
    '<button class="pa-btn pa-btn-pri" type="button" data-action="form-submit" data-kind="product">' +
    (p ? "保存修改" : "创建商品") + "</button>"
  );
}

/** 编辑商品（拉详情 + 引用数据后打开弹窗）；供本模块编辑按钮与看板「去补库存」共用 */
async function openProductEdit(id) {
  try {
    await ensureRefData();
    const detail = await apiRequest("/admin/products/" + id);
    openProductModal(detail);
  } catch (error) {
    handleApiError(error, "获取商品详情失败。");
  }
}

/** 金额格式校验：> 0，最多 8 位整数、2 位小数 */
function isValidPrice(raw) {
  return /^\d{1,8}(\.\d{1,2})?$/.test(raw) && Number(raw) > 0;
}

async function submitProduct(btn) {
  const sku = formVal("sku").toUpperCase();
  const name = formVal("name");
  const petType = formVal("pet");
  const flavor = formVal("flavor");
  const price = formVal("price");
  const stock = formVal("stock");
  const rating = formVal("rating");
  const imageUrl = formVal("img");
  const description = formVal("desc");
  const status = formVal("status");

  if (!sku) return showToast("请填写 SKU", "err");
  if (sku.length > 64) return showToast("SKU 长度不能超过 64 字符", "err");
  if (!name) return showToast("请填写商品名称", "err");
  if (name.length > 100) return showToast("商品名称长度不能超过 100 字符", "err");
  if (!flavor) return showToast("请填写口味", "err");
  if (flavor.length > 50) return showToast("口味长度不能超过 50 字符", "err");
  if (!isValidPrice(price)) return showToast("价格必须大于 0，最多 8 位整数、2 位小数", "err");
  if (!/^\d+$/.test(stock) || Number(stock) < 0) return showToast("库存须为 ≥ 0 的整数", "err");
  if (rating !== "" && !/^\d(\.\d)?$/.test(rating)) return showToast("评分须为 0.0 – 5.0 的一位小数", "err");
  if (Number(rating) > 5) return showToast("评分不能高于 5.0", "err");
  if (!/^https?:\/\/\S+$/.test(imageUrl)) return showToast("图片 URL 需以 http:// 或 https:// 开头", "err");
  if (imageUrl.length > 500) return showToast("图片 URL 长度不能超过 500 字符", "err");
  if (description.length > 500) return showToast("描述长度不能超过 500 字符", "err");
  if (editCtx.cats.size < 1) return showToast("请至少选择 1 个分类", "err");

  const body = {
    sku,
    name,
    petType,
    flavor,
    price: Number(price),
    stock: Number(stock),
    rating: rating === "" ? 5.0 : Number(rating),
    imageUrl,
    categories: [...editCtx.cats],
    tags: [...editCtx.tags],
    description,
    status,
  };

  btn.disabled = true;
  btn.textContent = "保存中…";
  try {
    if (editCtx.id) {
      await apiRequest("/admin/products/" + editCtx.id, { method: "PUT", body });
      showToast("商品「" + name + "」已保存");
    } else {
      await apiRequest("/admin/products", { method: "POST", body });
      showToast("商品「" + name + "」已创建");
      ui.products.page = 1; // 列表按 id 倒序，新商品在第 1 页
    }
    closeModal();
    await ensureRefData(true).catch(() => {}); // 引用数（productCount）可能变化
    loadProducts();
  } catch (error) {
    handleApiError(error, "保存失败，请稍后重试。");
    btn.disabled = false;
    btn.textContent = editCtx.id ? "保存修改" : "创建商品";
  }
}

/* ---------- 分类管理 ---------- */

function renderCatsCard(kind, message) {
  contentEl.innerHTML = productsSubtabsHtml("cats") +
    '<div class="pa-card"><div class="pa-toolbar">' +
    '<button class="pa-btn pa-btn-pri" type="button" data-action="new" data-kind="cat">' + icon("plus") + "新增分类</button>" +
    '<span class="pa-dim pa-toolbar-tip">编码创建后不可修改；「全部」为系统内置分类</span>' +
    "</div>" +
    '<div class="pa-tblwrap"><table class="pa-tbl"><thead><tr><th class="pa-ctr">排序</th><th>编码</th><th>名称</th><th>描述</th>' +
    '<th class="pa-ctr">引用商品</th><th>状态</th><th class="pa-nowrap">操作</th></tr></thead><tbody>' +
    (kind === "ok"
      ? refData.categories.map(catRowHtml).join("")
      : stateRowHtml(7, kind, message, "cats")) +
    "</tbody></table></div></div>";
}

function catRowHtml(c) {
  const isSys = c.code === "all";
  return "<tr>" +
    '<td class="pa-ctr">' + escapeHtml(c.sortOrder ?? 0) + "</td>" +
    '<td class="pa-mono">' + escapeHtml(c.code) + (isSys ? ' <span class="pa-pill p-gray">内置</span>' : "") + "</td>" +
    "<td>" + escapeHtml(c.name) + "</td>" +
    '<td class="pa-dim">' + escapeHtml(c.description || "—") + "</td>" +
    '<td class="pa-ctr">' + (Number(c.productCount) > 0 ? Number(c.productCount) : "—") + "</td>" +
    "<td>" + switchBtnHtml(c.status === "ACTIVE", "sw-cat", c.id) + "</td>" +
    '<td class="pa-nowrap">' +
    '<button class="pa-linkbtn" type="button" data-action="edit" data-kind="cat" data-id="' + escapeHtml(c.id) + '">编辑</button>' +
    (isSys
      ? '<button class="pa-linkbtn" type="button" disabled title="系统内置分类不可删除">删除</button>'
      : '<button class="pa-linkbtn is-danger" type="button" data-action="del" data-kind="cat" data-id="' + escapeHtml(c.id) + '">删除</button>') +
    "</td></tr>";
}

async function loadCats() {
  renderCatsCard("loading");
  try {
    await ensureRefData(true);
    renderCatsCard(refData.categories.length ? "ok" : "empty", "暂无分类，点击「新增分类」创建");
  } catch (error) {
    if (isAuthError(error)) {
      handleAuthFailure("登录状态已失效，请重新登录。");
      return;
    }
    renderCatsCard("error", "加载失败：" + errText(error));
  }
}

function openCatModal(c) {
  editCtx = { kind: "cat", id: c ? c.id : null, code: c ? c.code : null };
  showModal(
    "sm",
    c ? "编辑分类" : "新增分类",
    '<div class="pa-form pa-form-single">' +
    fi("分类编码", "ccode", c ? c.code : "", c
      ? "编码创建后不可修改"
      : "以小写字母开头，仅可含小写字母、数字与连字符，最长 40 字符，唯一", true, "text",
    (c ? "disabled " : "") + 'maxlength="40" autocomplete="off"') +
    fi("分类名称", "cname", c ? c.name : "", "", true, "text", "maxlength=\"64\"") +
    fi("描述", "cdesc", c ? c.description : "", "", false, "text", "maxlength=\"255\"") +
    fi("排序号", "csort", c ? c.sortOrder : 0, "数值越小越靠前，缺省 0", false, "number", 'step="1" inputmode="numeric"') +
    "</div>",
    '<button class="pa-btn" type="button" data-action="modal-close">取消</button>' +
    '<button class="pa-btn pa-btn-pri" type="button" data-action="form-submit" data-kind="cat">保存</button>'
  );
}

async function submitCat(btn) {
  const name = formVal("cname");
  const description = formVal("cdesc");
  const sortRaw = formVal("csort");

  if (!name) return showToast("请填写分类名称", "err");
  if (name.length > 64) return showToast("分类名称长度不能超过 64 字符", "err");
  if (description.length > 255) return showToast("分类描述长度不能超过 255 字符", "err");
  if (sortRaw !== "" && !/^-?\d+$/.test(sortRaw)) return showToast("排序号须为整数", "err");

  const isEdit = !!editCtx.id;
  let body;
  if (isEdit) {
    // 编辑：编码不可改（不传 code，后端视为不变）
    body = { name, description, sortOrder: sortRaw === "" ? 0 : Number(sortRaw) };
  } else {
    const code = formVal("ccode").toLowerCase();
    if (!code) return showToast("请填写分类编码", "err");
    if (code === "all") return showToast("all 为系统内置分类编码，不允许使用", "err");
    if (!/^[a-z][a-z0-9-]{0,39}$/.test(code)) {
      return showToast("编码需以小写字母开头，仅可含小写字母、数字与连字符，最长 40 字符", "err");
    }
    body = { code, name, description, sortOrder: sortRaw === "" ? 0 : Number(sortRaw) };
  }

  btn.disabled = true;
  btn.textContent = "保存中…";
  try {
    if (isEdit) {
      await apiRequest("/admin/categories/" + editCtx.id, { method: "PUT", body });
      showToast("分类已保存");
    } else {
      await apiRequest("/admin/categories", { method: "POST", body });
      showToast("分类已创建");
    }
    closeModal();
    await ensureRefData(true).catch(() => {});
    loadCats();
  } catch (error) {
    handleApiError(error, "保存失败，请稍后重试。");
    btn.disabled = false;
    btn.textContent = "保存";
  }
}

/* ---------- 标签管理 ---------- */

function renderTagsCard(kind, message) {
  contentEl.innerHTML = productsSubtabsHtml("tags") +
    '<div class="pa-card"><div class="pa-toolbar">' +
    '<button class="pa-btn pa-btn-pri" type="button" data-action="new" data-kind="tag">' + icon("plus") + "新增标签</button>" +
    '<span class="pa-dim pa-toolbar-tip">标签名全局唯一；被商品引用时不可删除（后端校验）</span>' +
    "</div>" +
    '<div class="pa-tblwrap"><table class="pa-tbl"><thead><tr><th>标签名</th><th class="pa-ctr">引用商品数</th><th>状态</th>' +
    '<th class="pa-nowrap">操作</th></tr></thead><tbody>' +
    (kind === "ok"
      ? refData.tags.map(tagRowHtml).join("")
      : stateRowHtml(4, kind, message, "tags")) +
    "</tbody></table></div></div>";
}

function tagRowHtml(t) {
  return "<tr>" +
    '<td><span class="pa-pill p-teal">' + escapeHtml(t.name) + "</span></td>" +
    '<td class="pa-ctr">' + (Number(t.productCount) > 0 ? Number(t.productCount) : "—") + "</td>" +
    "<td>" + switchBtnHtml(t.status === "ACTIVE", "sw-tag", t.id) + "</td>" +
    '<td class="pa-nowrap">' +
    '<button class="pa-linkbtn" type="button" data-action="edit" data-kind="tag" data-id="' + escapeHtml(t.id) + '">编辑</button>' +
    '<button class="pa-linkbtn is-danger" type="button" data-action="del" data-kind="tag" data-id="' + escapeHtml(t.id) + '">删除</button>' +
    "</td></tr>";
}

async function loadTags() {
  renderTagsCard("loading");
  try {
    await ensureRefData(true);
    renderTagsCard(refData.tags.length ? "ok" : "empty", "暂无标签，点击「新增标签」创建");
  } catch (error) {
    if (isAuthError(error)) {
      handleAuthFailure("登录状态已失效，请重新登录。");
      return;
    }
    renderTagsCard("error", "加载失败：" + errText(error));
  }
}

function openTagModal(t) {
  editCtx = { kind: "tag", id: t ? t.id : null };
  showModal(
    "sm",
    t ? "编辑标签" : "新增标签",
    '<div class="pa-form pa-form-single">' +
    fi("标签名称", "tname", t ? t.name : "", "全局唯一，最长 64 字符", true, "text", "maxlength=\"64\"") +
    "</div>",
    '<button class="pa-btn" type="button" data-action="modal-close">取消</button>' +
    '<button class="pa-btn pa-btn-pri" type="button" data-action="form-submit" data-kind="tag">保存</button>'
  );
}

async function submitTag(btn) {
  const name = formVal("tname");
  if (!name) return showToast("请填写标签名称", "err");
  if (name.length > 64) return showToast("标签名称长度不能超过 64 字符", "err");

  btn.disabled = true;
  btn.textContent = "保存中…";
  try {
    if (editCtx.id) {
      await apiRequest("/admin/tags/" + editCtx.id, { method: "PUT", body: { name } });
      showToast("标签已保存");
    } else {
      await apiRequest("/admin/tags", { method: "POST", body: { name } });
      showToast("标签已创建");
    }
    closeModal();
    await ensureRefData(true).catch(() => {});
    loadTags();
  } catch (error) {
    handleApiError(error, "保存失败，请稍后重试。");
    btn.disabled = false;
    btn.textContent = "保存";
  }
}

/* ============================================================
 * 模块：场景套餐
 * ============================================================ */

function renderBundlesCard(kind, message) {
  const u = ui.bundles;
  contentEl.innerHTML =
    '<div class="pa-card"><div class="pa-card-hd"><h2>场景套餐</h2><p>多商品打包组合，C 端首页套餐区展示；编辑不影响历史数据</p></div>' +
    '<div class="pa-toolbar">' +
    '<button class="pa-btn pa-btn-pri" type="button" data-action="new" data-kind="bundle">' + icon("plus") + "新增套餐</button>" +
    selBox("b-status", [{ v: "all", t: "全部状态" }, { v: "ACTIVE", t: "上架中" }, { v: "INACTIVE", t: "已下架" }], u.status) +
    '<span class="pa-sp"></span>' + kwBox("bundles", "套餐名称", u.q) +
    "</div>" +
    '<div class="pa-tblwrap"><table class="pa-tbl"><thead><tr><th class="pa-ctr">排序</th><th>套餐</th><th>折扣文案</th>' +
    '<th class="pa-num">现价</th><th class="pa-num">原价</th><th>明细</th><th>上架状态</th><th class="pa-nowrap">操作</th>' +
    "</tr></thead><tbody>" +
    (kind === "ok"
      ? u.rows.map((b) => bundleRowHtml(b)).join("")
      : stateRowHtml(8, kind, message, "bundles")) +
    "</tbody></table></div>" +
    (kind === "ok" ? pagerHtml("bundles", u.page, u.totalPages, u.total) : "") +
    "</div>";
}

function bundleRowHtml(b) {
  const items = Array.isArray(b.items) ? b.items : [];
  // 套餐缩略图取第一件商品的图（套餐 VO 本身无 imageUrl 字段）
  const firstItem = items[0];
  return "<tr>" +
    '<td class="pa-ctr">' + escapeHtml(b.sortOrder ?? 0) + "</td>" +
    "<td><span class=\"pa-prod\">" + thumbHtml(b.id, b.name, firstItem ? firstItem.imageUrl : "") +
    "<span><span class=\"pa-cell-main\" title=\"" + escapeHtml(b.name) + '">' + escapeHtml(b.name) + "</span>" +
    '<div class="pa-cell-sub" style="max-width:220px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" title="' +
    escapeHtml(b.description || "") + '">' + escapeHtml(b.description || "—") + "</div></span></span></td>" +
    "<td>" + (b.discountLabel ? '<span class="pa-pill p-orange">' + escapeHtml(b.discountLabel) + "</span>" : '<span class="pa-dim">—</span>') + "</td>" +
    '<td class="pa-num">' + money(b.price) + "</td>" +
    '<td class="pa-num pa-dim" style="text-decoration:line-through">' + money(b.originalPrice) + "</td>" +
    "<td>" + Number(b.itemCount || 0) + " 种 · 共 " + Number(b.totalQuantity || 0) + " 件</td>" +
    "<td>" + switchBtnHtml(b.status === "ACTIVE", "sw-bundle", b.id) + "</td>" +
    '<td class="pa-nowrap">' +
    '<button class="pa-linkbtn" type="button" data-action="edit" data-kind="bundle" data-id="' + escapeHtml(b.id) + '">编辑</button>' +
    '<button class="pa-linkbtn is-danger" type="button" data-action="del" data-kind="bundle" data-id="' + escapeHtml(b.id) + '">删除</button>' +
    "</td></tr>";
}

async function loadBundles() {
  const u = ui.bundles;
  renderBundlesCard("loading");

  const query = new URLSearchParams({ page: String(u.page), size: String(u.size) });
  const keyword = u.q.trim();
  if (keyword) query.set("keyword", keyword);
  if (u.status !== "all") query.set("status", u.status);

  try {
    const data = await apiRequest("/admin/bundles?" + query.toString());
    u.rows = Array.isArray(data?.records) ? data.records : [];
    u.total = Number(data?.total || 0);
    u.totalPages = Math.max(1, Math.ceil(u.total / u.size));
    renderBundlesCard(u.rows.length ? "ok" : "empty", keyword || u.status !== "all" ? "未找到匹配的套餐" : "暂无套餐，点击「新增套餐」创建");
  } catch (error) {
    if (isAuthError(error)) {
      handleAuthFailure("登录状态已失效，请重新登录。");
      return;
    }
    u.rows = [];
    u.total = 0;
    u.totalPages = 1;
    renderBundlesCard("error", "加载失败：" + errText(error));
  }
}

/* ---------- 套餐新增/编辑弹窗（含明细子表编辑器） ---------- */

/** 明细行的商品价格（取当前下拉数据源） */
function bundleItemPrice(pid) {
  const p = editCtx.products.find((x) => Number(x.id) === Number(pid));
  return p ? Number(p.price) : 0;
}

function bundleItemName(pid) {
  const p = editCtx.products.find((x) => Number(x.id) === Number(pid));
  return p ? p.name : "";
}

/** 明细编辑器 HTML（每次增删改后整体重绘） */
function biBoxHtml() {
  const items = editCtx.items;
  let total = 0;
  items.forEach((it) => {
    total += bundleItemPrice(it.productId) * Number(it.quantity || 0);
  });

  const rows = items.map((it, i) => {
    const usedByOthers = new Set(items.filter((_, j) => j !== i).map((x) => String(x.productId)));
    const options = editCtx.products
      .map((p) => {
        // 其他行已选的商品不再出现在本行下拉（同一商品去重）
        if (usedByOthers.has(String(p.id))) return "";
        const label = p.name + "（¥" + Number(p.price).toFixed(2) + "）" + (p.status === "INACTIVE" ? "（已下架）" : "");
        return '<option value="' + escapeHtml(p.id) + '"' + (Number(p.id) === Number(it.productId) ? " selected" : "") + ">" +
          escapeHtml(label) + "</option>";
      })
      .join("");
    const price = bundleItemPrice(it.productId);
    return "<tr>" +
      '<td><select data-bi="' + i + '" aria-label="选择商品">' + options + "</select></td>" +
      '<td><span class="pa-step">' +
      '<button type="button" data-action="bi-qty" data-i="' + i + '" data-d="-1"' + (it.quantity <= 1 ? " disabled" : "") + ">−</button>" +
      "<span>" + Number(it.quantity) + "</span>" +
      '<button type="button" data-action="bi-qty" data-i="' + i + '" data-d="1"' + (it.quantity >= 99 ? " disabled" : "") + ">+</button>" +
      "</span></td>" +
      '<td class="pa-num">' + money(price * Number(it.quantity)) + "</td>" +
      '<td class="pa-ctr"><button class="pa-linkbtn is-danger" type="button" data-action="bi-del" data-i="' + i + '">移除</button></td>' +
      "</tr>";
  }).join("");

  return '<div class="pa-bi-tools">' +
    '<button class="pa-btn" type="button" data-action="bi-add">' + icon("plus") + "添加商品</button>" +
    '<button class="pa-btn" type="button" data-action="bi-fill">一键填入明细合计为原价</button>' +
    '<span class="pa-bi-total">明细合计 <b id="paBiTotal">' + money(total) + "</b></span></div>" +
    '<table class="pa-bi-tbl"><thead><tr><th style="width:46%;">商品（全部未删除商品，已下架带后缀）</th><th>数量（1–99）</th>' +
    '<th style="text-align:right;">小计</th><th style="text-align:center;width:56px;">操作</th></tr></thead>' +
    '<tbody id="paBiBody">' +
    (rows || '<tr><td colspan="4" class="pa-dim" style="text-align:center;padding:12px;">暂无明细，请点击「添加商品」</td></tr>') +
    "</tbody></table>";
}

function refreshBiBox() {
  const box = modalEl.querySelector("#paBiBox");
  if (box) box.innerHTML = biBoxHtml();
}

function openBundleModal(b, products) {
  editCtx = {
    kind: "bundle",
    id: b ? b.id : null,
    products: Array.isArray(products) ? products : [],
    items: b ? (b.items || []).map((it) => ({ productId: it.productId, quantity: Number(it.quantity || 1) })) : [],
  };
  showModal(
    "lg",
    b ? "编辑套餐" : "新增套餐",
    '<div class="pa-form">' +
    fi("套餐名称", "bname", b ? b.name : "", "", true, "text", "maxlength=\"100\"") +
    fi("折扣文案", "blabel", b ? b.discountLabel : "", "如：省 18%，最长 20 字，留空不显示角标", false, "text", "maxlength=\"20\"") +
    fi("现价（元）", "bprice", b ? b.price : "", "&gt; 0，最多 8 位整数、2 位小数", true, "number", 'step="0.01" min="0.01" inputmode="decimal"') +
    fi("原价（元）", "borig", b ? b.originalPrice : "", "应 ≥ 现价（划线价）", true, "number", 'step="0.01" min="0.01" inputmode="decimal"') +
    fi("排序号", "bsort", b ? b.sortOrder : 0, "越小越靠前，缺省 0", false, "number", 'step="1" inputmode="numeric"') +
    '<div class="pa-fi"><label>上架状态</label><select id="f-bstatus">' +
    '<option value="ACTIVE"' + (!b || b.status === "ACTIVE" ? " selected" : "") + ">上架</option>" +
    '<option value="INACTIVE"' + (b && b.status === "INACTIVE" ? " selected" : "") + ">下架</option>" +
    "</select></div>" +
    '<div class="pa-fi full"><label>套餐描述</label><textarea id="f-bdesc" maxlength="200" rows="3">' +
    escapeHtml(b ? b.description : "") + "</textarea></div>" +
    '<div class="pa-fi full"><label><span class="req">*</span>商品明细（至少 1 行，同一商品不可重复）</label>' +
    '<div id="paBiBox">' + biBoxHtml() + "</div></div>" +
    "</div>",
    '<button class="pa-btn" type="button" data-action="modal-close">取消</button>' +
    '<button class="pa-btn pa-btn-pri" type="button" data-action="form-submit" data-kind="bundle">' +
    (b ? "保存修改" : "创建套餐") + "</button>"
  );
}

/** 打开套餐弹窗（新增/编辑）：先拉全部未删除商品作为明细下拉数据源 */
async function openBundleEdit(id) {
  let products = [];
  try {
    // 后端 size clamp [1,100]，商品超过 100 时仅取前 100 条
    const data = await apiRequest("/admin/products?page=1&size=" + OPTION_FETCH_SIZE);
    products = Array.isArray(data?.records) ? data.records : [];
  } catch (error) {
    handleApiError(error, "获取商品列表失败，无法编辑套餐明细。");
    return;
  }
  if (id == null) {
    openBundleModal(null, products);
    return;
  }
  try {
    const detail = await apiRequest("/admin/bundles/" + id);
    openBundleModal(detail, products);
  } catch (error) {
    handleApiError(error, "获取套餐详情失败。");
  }
}

/** 添加一行明细：默认选第一个未被占用的商品 */
function biAdd() {
  const used = new Set(editCtx.items.map((it) => String(it.productId)));
  const candidate = editCtx.products.find((p) => !used.has(String(p.id)));
  if (!candidate) {
    showToast("没有更多可添加的商品", "warn");
    return;
  }
  editCtx.items.push({ productId: Number(candidate.id), quantity: 1 });
  refreshBiBox();
}

function biDel(index) {
  editCtx.items.splice(index, 1);
  refreshBiBox();
}

function biQty(index, delta) {
  const item = editCtx.items[index];
  if (!item) return;
  item.quantity = Math.min(99, Math.max(1, Number(item.quantity) + Number(delta)));
  refreshBiBox();
}

function biFill() {
  let total = 0;
  editCtx.items.forEach((it) => {
    total += bundleItemPrice(it.productId) * Number(it.quantity || 0);
  });
  const input = modalEl.querySelector("#f-borig");
  if (input) {
    input.value = total.toFixed(2);
    showToast("已将明细合计 " + money(total) + " 填入原价");
  }
}

async function submitBundle(btn) {
  const name = formVal("bname");
  const discountLabel = formVal("blabel");
  const price = formVal("bprice");
  const originalPrice = formVal("borig");
  const description = formVal("bdesc");
  const sortRaw = formVal("bsort");
  const status = formVal("bstatus");

  if (!name) return showToast("请填写套餐名称", "err");
  if (name.length > 100) return showToast("套餐名称长度不能超过 100 字符", "err");
  if (!isValidPrice(price)) return showToast("现价必须大于 0，最多 8 位整数、2 位小数", "err");
  if (!isValidPrice(originalPrice)) return showToast("原价必须大于 0，最多 8 位整数、2 位小数", "err");
  if (Number(originalPrice) < Number(price)) return showToast("原价应不低于现价", "err"); // 前端先拦截
  if (discountLabel.length > 20) return showToast("折扣文案长度不能超过 20 字符", "err");
  if (description.length > 200) return showToast("描述长度不能超过 200 字符", "err");
  if (sortRaw !== "" && !/^-?\d+$/.test(sortRaw)) return showToast("排序号须为整数", "err");
  if (editCtx.items.length < 1) return showToast("请至少添加 1 行商品明细", "err");

  const body = {
    name,
    price: Number(price),
    originalPrice: Number(originalPrice),
    discountLabel,
    description,
    sortOrder: sortRaw === "" ? 0 : Number(sortRaw),
    status,
    items: editCtx.items.map((it) => ({ productId: Number(it.productId), quantity: Number(it.quantity) })),
  };

  btn.disabled = true;
  btn.textContent = "保存中…";
  try {
    if (editCtx.id) {
      await apiRequest("/admin/bundles/" + editCtx.id, { method: "PUT", body });
      showToast("套餐「" + name + "」已保存");
    } else {
      await apiRequest("/admin/bundles", { method: "POST", body });
      showToast("套餐「" + name + "」已创建");
    }
    closeModal();
    loadBundles(); // 列表按 sort_order 升序，保持当前页刷新
  } catch (error) {
    handleApiError(error, "保存失败，请稍后重试。");
    btn.disabled = false;
    btn.textContent = editCtx.id ? "保存修改" : "创建套餐";
  }
}

/* ============================================================
 * Switch 乐观更新（失败回滚）
 * ============================================================ */

/**
 * 构造状态开关处理器：先翻转视觉，失败回滚并 toast 后端文案
 * @param path     资源路径（如 "/admin/products"）
 * @param applyRow 成功后同步本地行状态（避免下次重渲染回跳）
 * @param message  (next) => toast 文案
 */
function makeSwitchToggle(path, applyRow, message) {
  return function (btn) {
    if (btn.disabled) return;
    const id = btn.dataset.id;
    const wasOn = btn.getAttribute("aria-checked") === "true";
    const next = wasOn ? "INACTIVE" : "ACTIVE";

    btn.setAttribute("aria-checked", String(!wasOn)); // 乐观更新
    btn.disabled = true;
    apiRequest(path + "/" + id + "/status", { method: "PUT", body: { status: next } })
      .then(() => {
        if (applyRow) applyRow(id, next);
        showToast(message(next));
      })
      .catch((error) => {
        btn.setAttribute("aria-checked", String(wasOn)); // 回滚
        handleApiError(error, "状态更新失败，请稍后重试。");
      })
      .finally(() => {
        btn.disabled = false;
      });
  };
}

const toggleProductStatus = makeSwitchToggle(
  "/admin/products",
  (id, next) => {
    const row = ui.products.rows.find((r) => String(r.id) === String(id));
    if (row) row.status = next;
  },
  (next) => (next === "ACTIVE" ? "商品已上架，C 端将恢复展示" : "商品已下架，C 端将不再展示")
);

const toggleBundleStatus = makeSwitchToggle(
  "/admin/bundles",
  (id, next) => {
    const row = ui.bundles.rows.find((r) => String(r.id) === String(id));
    if (row) row.status = next;
  },
  (next) => (next === "ACTIVE" ? "套餐已上架" : "套餐已下架")
);

const toggleCatStatus = makeSwitchToggle(
  "/admin/categories",
  (id, next) => {
    const row = refData.categories.find((c) => String(c.id) === String(id));
    if (row) row.status = next;
  },
  (next) => (next === "ACTIVE" ? "分类已启用" : "分类已停用，C 端导航将不再展示")
);

const toggleTagStatus = makeSwitchToggle(
  "/admin/tags",
  (id, next) => {
    const row = refData.tags.find((t) => String(t.id) === String(id));
    if (row) row.status = next;
  },
  (next) => (next === "ACTIVE" ? "标签已启用" : "标签已停用")
);

/* ============================================================
 * 删除（危险操作红色二次确认；被引用时直接 toast 后端 400 文案）
 * ============================================================ */

function confirmDeleteProduct(id) {
  const row = ui.products.rows.find((r) => String(r.id) === String(id));
  const name = row ? row.name : "#" + id;
  confirmBox("删除商品「" + name + "」？",
    "逻辑删除，历史订单快照不受影响；商品被套餐引用时后端将拒绝删除。",
    "确认删除",
    async () => {
      try {
        await apiRequest("/admin/products/" + id, { method: "DELETE" });
        showToast("商品已删除");
        if (ui.products.rows.length === 1 && ui.products.page > 1) ui.products.page -= 1;
        await ensureRefData(true).catch(() => {}); // 引用数变化
        loadProducts();
      } catch (error) {
        handleApiError(error, "删除失败，请稍后重试。");
      }
    });
}

function confirmDeleteBundle(id) {
  const row = ui.bundles.rows.find((r) => String(r.id) === String(id));
  const name = row ? row.name : "#" + id;
  confirmBox("删除套餐「" + name + "」？",
    "删除后 C 端套餐区立即不再展示。",
    "确认删除",
    async () => {
      try {
        await apiRequest("/admin/bundles/" + id, { method: "DELETE" });
        showToast("套餐已删除");
        if (ui.bundles.rows.length === 1 && ui.bundles.page > 1) ui.bundles.page -= 1;
        loadBundles();
      } catch (error) {
        handleApiError(error, "删除失败，请稍后重试。");
      }
    });
}

function confirmDeleteCat(id) {
  const row = refData.categories.find((c) => String(c.id) === String(id));
  const name = row ? row.name : "#" + id;
  confirmBox("删除分类「" + name + "」？",
    "删除后不可恢复；被商品引用时后端将拒绝并提示引用数。",
    "确认删除",
    async () => {
      try {
        await apiRequest("/admin/categories/" + id, { method: "DELETE" });
        showToast("分类已删除");
        await ensureRefData(true).catch(() => {});
        loadCats();
      } catch (error) {
        handleApiError(error, "删除失败，请稍后重试。");
      }
    });
}

function confirmDeleteTag(id) {
  const row = refData.tags.find((t) => String(t.id) === String(id));
  const name = row ? row.name : "#" + id;
  confirmBox("删除标签「" + name + "」？",
    "删除后不可恢复；被商品引用时后端将拒绝并提示引用数。",
    "确认删除",
    async () => {
      try {
        await apiRequest("/admin/tags/" + id, { method: "DELETE" });
        showToast("标签已删除");
        await ensureRefData(true).catch(() => {});
        loadTags();
      } catch (error) {
        handleApiError(error, "删除失败，请稍后重试。");
      }
    });
}

/* ============================================================
 * data-action 处理器注册
 * ============================================================ */

/** 各 scope 的重新加载器（搜索 / 翻页 / 重试共用，翻页保留条件） */
const retryLoaders = {
  dashboard: loadDashboard,
  products: loadProducts,
  cats: loadCats,
  tags: loadTags,
  bundles: loadBundles,
};

function goPage(scope, target) {
  const scopeState = ui[scope];
  if (!scopeState) return;
  const clamped = Math.min(Math.max(target, 1), scopeState.totalPages);
  if (clamped === scopeState.page) return;
  scopeState.page = clamped;
  (retryLoaders[scope] || (() => {}))();
}

function doJump(scope) {
  const input = document.getElementById("jump-" + scope);
  if (!input) return;
  const raw = String(input.value).trim();
  if (!/^\d+$/.test(raw)) {
    input.value = ui[scope] ? ui[scope].page : 1;
    return;
  }
  const clamped = Math.min(Math.max(Number(raw), 1), ui[scope] ? ui[scope].totalPages : 1);
  input.value = clamped;
  goPage(scope, clamped);
}

function registerActions() {
  onAction("nav", (t) => switchTo(t.dataset.view));
  onAction("logout", doLogout);

  onAction("subtab", (t) => {
    ui.products.subtab = t.dataset.tab;
    ui.products.page = 1;
    loadProductsSubtab();
  });

  onAction("search", (t) => {
    const scope = t.dataset.scope;
    if (ui[scope]) {
      ui[scope].page = 1; // 搜索重置到第 1 页
      (retryLoaders[scope] || (() => {}))();
    }
  });

  onAction("page", (t) => {
    const scope = t.dataset.scope;
    const raw = t.dataset.p;
    if (!ui[scope]) return;
    if (raw === "prev") goPage(scope, ui[scope].page - 1);
    else if (raw === "next") goPage(scope, ui[scope].page + 1);
    else goPage(scope, Number(raw));
  });

  onAction("jump", (t) => doJump(t.dataset.scope));
  onAction("retry", (t) => {
    const fn = retryLoaders[t.dataset.scope];
    if (fn) fn();
  });

  onAction("modal-close", closeModal);
  onAction("confirm-ok", () => {
    const fn = onConfirmOk;
    onConfirmOk = null;
    closeModal();
    if (fn) fn();
  });

  onAction("goto-order", () => showToast("订单模块建设中（FE-B 交付）", "warn"));
  onAction("restock", (t) => {
    // 去补库存：切到商品管理并拉起编辑弹窗
    switchTo("products");
    openProductEdit(Number(t.dataset.id));
  });

  onAction("new", (t) => {
    const kind = t.dataset.kind;
    if (kind === "product") {
      ensureRefData()
        .then(() => openProductModal(null))
        .catch((error) => handleApiError(error, "获取分类/标签失败，无法新增商品。"));
    } else if (kind === "bundle") {
      openBundleEdit(null);
    } else if (kind === "cat") {
      openCatModal(null);
    } else if (kind === "tag") {
      openTagModal(null);
    }
  });

  onAction("edit", (t) => {
    const kind = t.dataset.kind;
    const id = t.dataset.id;
    if (kind === "product") {
      openProductEdit(Number(id));
    } else if (kind === "bundle") {
      openBundleEdit(Number(id));
    } else if (kind === "cat") {
      const row = refData.categories.find((c) => String(c.id) === String(id));
      if (row) openCatModal(row);
    } else if (kind === "tag") {
      const row = refData.tags.find((x) => String(x.id) === String(id));
      if (row) openTagModal(row);
    }
  });

  onAction("del", (t) => {
    const kind = t.dataset.kind;
    const id = Number(t.dataset.id);
    if (kind === "product") confirmDeleteProduct(id);
    else if (kind === "bundle") confirmDeleteBundle(id);
    else if (kind === "cat") confirmDeleteCat(id);
    else if (kind === "tag") confirmDeleteTag(id);
  });

  onAction("form-submit", (t) => {
    const kind = t.dataset.kind;
    if (kind === "product") submitProduct(t);
    else if (kind === "bundle") submitBundle(t);
    else if (kind === "cat") submitCat(t);
    else if (kind === "tag") submitTag(t);
  });

  onAction("chip", (t) => {
    if (!editCtx) return;
    const group = t.dataset.g;
    const value = t.dataset.v;
    const set = group === "cats" ? editCtx.cats : editCtx.tags;
    const key = group === "cats" ? value : Number(value);
    if (set.has(key)) set.delete(key);
    else set.add(key);
    t.classList.toggle("is-on");
  });

  onAction("sw-product", toggleProductStatus);
  onAction("sw-bundle", toggleBundleStatus);
  onAction("sw-cat", toggleCatStatus);
  onAction("sw-tag", toggleTagStatus);

  onAction("bi-add", biAdd);
  onAction("bi-del", (t) => biDel(Number(t.dataset.i)));
  onAction("bi-qty", (t) => biQty(Number(t.dataset.i), Number(t.dataset.d)));
  onAction("bi-fill", biFill);
}

/* ============================================================
 * 事件绑定（根容器统一委托）
 * ============================================================ */

function bindEvents() {
  // data-action 统一委托（内容区 + 弹窗）
  rootEl.addEventListener("click", (event) => {
    const target = event.target.closest("[data-action]");
    if (!target || target.disabled) return;
    const handler = actionHandlers[target.dataset.action];
    if (handler) handler(target, event);
  });

  // 筛选下拉 / 套餐明细行商品切换
  rootEl.addEventListener("change", (event) => {
    const t = event.target;
    if (!(t instanceof Element)) return;
    const filter = t.dataset ? t.dataset.filter : null;
    if (filter === "p-cat") {
      ui.products.category = t.value;
      ui.products.page = 1;
      loadProducts();
    } else if (filter === "p-status") {
      ui.products.status = t.value;
      ui.products.page = 1;
      loadProducts();
    } else if (filter === "b-status") {
      ui.bundles.status = t.value;
      ui.bundles.page = 1;
      loadBundles();
    } else if (t.dataset && t.dataset.bi !== undefined && editCtx && editCtx.kind === "bundle") {
      const index = Number(t.dataset.bi);
      const pid = Number(t.value);
      const duplicated = editCtx.items.some((it, j) => j !== index && Number(it.productId) === pid);
      if (duplicated) {
        showToast("该商品已在明细中，请选择其他商品", "warn");
        refreshBiBox(); // 还原为状态里的选中值
        return;
      }
      editCtx.items[index].productId = pid;
      refreshBiBox();
    }
  });

  // 关键字输入同步（不重渲染，保留焦点）；SKU 自动转大写
  rootEl.addEventListener("input", (event) => {
    const t = event.target;
    if (!(t instanceof Element)) return;
    if (t.id === "f-sku") {
      t.value = t.value.toUpperCase();
      return;
    }
    if (t.classList.contains("pa-kw-inp") && t.dataset.scope && ui[t.dataset.scope]) {
      ui[t.dataset.scope].q = t.value;
    }
  });

  // Esc 关弹窗；回车触发搜索 / 跳页
  rootEl.addEventListener("keydown", (event) => {
    if (event.key === "Escape") {
      if (!overlay.hidden) closeModal();
      return;
    }
    if (event.key !== "Enter") return;
    const t = event.target;
    if (!(t instanceof Element)) return;
    if (t.classList.contains("pa-kw-inp") && t.dataset.scope && ui[t.dataset.scope]) {
      event.preventDefault();
      ui[t.dataset.scope].page = 1;
      (retryLoaders[t.dataset.scope] || (() => {}))();
    } else if (t.id && t.id.indexOf("jump-") === 0) {
      event.preventDefault();
      doJump(t.id.slice(5));
    }
  });

  // 登录表单
  loginForm.addEventListener("submit", (event) => {
    event.preventDefault();
    doLogin();
  });

  // 点击遮罩关闭弹窗
  overlay.addEventListener("mousedown", (event) => {
    if (event.target === overlay) closeModal();
  });
}

/* ============================================================
 * 内置模块注册（FE-A 实现 4 个，其余占位等待 FE-B）
 * ============================================================ */

/** 占位模块：内容区显示「建设中」卡片 */
function makePlaceholderModule(id, title, order, iconName) {
  return {
    id,
    title,
    order,
    icon: iconName,
    placeholder: true,
    mount(container) {
      container.innerHTML =
        '<div class="pa-card pa-wip-card">' +
        '<span class="pa-wip-ico">' + icon(iconName) + "</span>" +
        "<h2>该模块建设中</h2>" +
        "<p>「" + escapeHtml(title) + "」由 FE-B 阶段交付，敬请期待。</p>" +
        "</div>";
    },
  };
}

function registerBuiltinModules() {
  PawAdmin.registerModule({
    id: "dashboard",
    title: "数据看板",
    order: 10,
    icon: "dashboard",
    mount() {
      loadDashboard();
    },
  });

  const productsModule = {
    id: "products",
    title: "商品管理",
    order: 20,
    icon: "package",
    mount() {
      loadProductsSubtab();
    },
    /** 模块间互调入口：看板「去补库存」用它拉起商品编辑弹窗 */
    openEdit: openProductEdit,
  };
  PawAdmin.registerModule(productsModule);

  PawAdmin.registerModule({
    id: "bundles",
    title: "场景套餐",
    order: 30,
    icon: "gift",
    mount() {
      loadBundles();
    },
  });

  // FE-B 范围：同 id 重新 registerModule 即可覆盖占位定义
  PawAdmin.registerModule(makePlaceholderModule("orders", "订单管理", 40, "cart"));
  PawAdmin.registerModule(makePlaceholderModule("users", "用户管理", 50, "users"));
  PawAdmin.registerModule(makePlaceholderModule("reviews", "评价管理", 60, "star"));
  PawAdmin.registerModule(makePlaceholderModule("coupons", "优惠券管理", 70, "ticket"));
  PawAdmin.registerModule(makePlaceholderModule("logs", "日志管理", 80, "file-text"));
}

/* ---------------- 提供给模块 mount 的公共上下文 ---------------- */

function buildCtx() {
  PawAdmin.ctx = {
    api: apiRequest,            // 统一请求封装（Bearer + code!==0 抛错）
    escapeHtml,
    money,
    fmtTime,
    icon,
    icons: ICONS,
    toast: showToast,
    showModal,
    closeModal,
    confirmBox,
    onAuthFailure: handleAuthFailure,
    onAction,                   // 注册 data-action 处理器
    isAuthError,
    errText,
    statusPill,
    switchTo,
  };
}

/* ---------------- 启动 ---------------- */

function init() {
  // 顶栏退出按钮图标
  if (logoutButton) logoutButton.innerHTML = icon("logout");

  registerBuiltinModules();
  registerActions();
  buildCtx();
  bindEvents();
  initAuth();
}

init();
