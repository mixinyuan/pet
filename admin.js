"use strict";

/* ============================================================
 * PawBites 日志管理后台（admin.html 配套脚本）
 * 自带轻量基础设施：apiRequest（Bearer 头 + 统一 code!==0 抛错）、
 * escapeHtml、toast；登录态复用 localStorage 的 pawbites-auth
 * （存法与 script.js 一致：JSON 字符串 {mode,token,user,displayName}）。
 * 不引入 script.js 本体。
 *
 * 2026-11 迁移说明：日志接口已由 /api/logs 整体迁至 /api/admin/logs
 * （仅 ADMIN 角色可访问）；本页所有请求路径同步更新，非管理员访问
 * 接口返回 403 时提示并停留登录门。
 * ============================================================ */

const API_BASE_URL = localStorage.getItem("pawbites-api-base") || "http://localhost:8080/api";
const AUTH_STORAGE_KEY = "pawbites-auth";
const DEFAULT_PAGE_SIZE = 10;
const DEFAULT_LOG_TYPE = "AI自习室通知";

const adminGate = document.querySelector("#adminGate");
const gateMessage = document.querySelector("#gateMessage");
const adminShell = document.querySelector("#adminShell");
const adminAccountName = document.querySelector("#adminAccountName");
const createLogButton = document.querySelector("#createLogButton");
const searchInput = document.querySelector("#searchInput");
const searchButton = document.querySelector("#searchButton");
const tableWrap = document.querySelector("#tableWrap");
const logTable = document.querySelector("#logTable");
const logTableBody = document.querySelector("#logTableBody");
const tableState = document.querySelector("#tableState");
const pagination = document.querySelector("#pagination");

const logModal = document.querySelector("#logModal");
const logForm = document.querySelector("#logForm");
const logModalTitle = document.querySelector("#logModalTitle");
const logTitleInput = document.querySelector("#logTitleInput");
const logTypeInput = document.querySelector("#logTypeInput");
const logContentInput = document.querySelector("#logContentInput");
const logFormMessage = document.querySelector("#logFormMessage");
const logSubmitButton = document.querySelector("#logSubmitButton");

const confirmModal = document.querySelector("#confirmModal");
const confirmDeleteButton = document.querySelector("#confirmDeleteButton");

const adminToast = document.querySelector("#adminToast");

/** 当前登录态（{mode,token,user,displayName}），未登录为 null */
let authProfile = null;
/** 列表页状态 */
let listState = {
  page: 1,
  pageSize: DEFAULT_PAGE_SIZE,
  keyword: "",
  total: 0,
  totalPages: 1,
  rows: [],
};
/** 列表请求序号：丢弃慢返回的过期响应 */
let listRequestId = 0;
/** 发布状态切换中的日志 id 集合（防抖：请求期间该开关不可再点） */
const statusPendingIds = new Set();
/** 正在编辑的日志 id（null 表示新增） */
let editingLogId = null;
/** 待删除的日志 id */
let deletingLogId = null;
/** 弹窗打开前的焦点元素，关闭后归还 */
let modalReturnFocus = null;
let toastTimer;

/* ---------------- 基础工具 ---------------- */

function readStoredValue(key, fallbackValue = null) {
  try {
    const value = localStorage.getItem(key);
    return value ? JSON.parse(value) : fallbackValue;
  } catch {
    return fallbackValue;
  }
}

function removeStoredValue(key) {
  try {
    localStorage.removeItem(key);
  } catch {
    // 隐私模式下 localStorage 可能不可用，忽略
  }
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function showToast(message) {
  clearTimeout(toastTimer);
  adminToast.textContent = message;
  adminToast.classList.add("show");
  toastTimer = setTimeout(() => {
    adminToast.classList.remove("show");
  }, 1800);
}

function normalizeError(error) {
  if (error instanceof TypeError) {
    return "无法连接后端服务，请确认 Spring Boot 已启动。";
  }
  return error.message || "操作失败，请稍后重试。";
}

/** 是否权限不足（HTTP 403 / 业务码 403；/api/admin/** 要求 ADMIN 角色） */
function isForbidden(error) {
  return error && (error.code === 403 || error.status === 403);
}

/**
 * 轻量 apiRequest：仅登录场景（Bearer 头）。
 * 约定与 script.js 对齐：code!==0 时抛出 payload.message；
 * fetch 网络异常转为友好文案；401 一律按登录失效处理。
 */
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
    throw Object.assign(new Error(normalizeError(error)), { network: true });
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

/* ---------------- 登录门 ---------------- */

function showGate(message) {
  adminShell.hidden = true;
  adminGate.hidden = false;
  gateMessage.textContent = message;
}

function handleAuthFailure(message) {
  authProfile = null;
  removeStoredValue(AUTH_STORAGE_KEY);
  showGate(message);
}

async function initAuth() {
  const savedProfile = readStoredValue(AUTH_STORAGE_KEY);

  if (!savedProfile || savedProfile.mode !== "user" || !savedProfile.token) {
    showGate(savedProfile?.mode === "guest"
      ? "日志管理需要登录账号，游客模式无法访问。"
      : "该页面需要登录后访问。");
    return;
  }

  authProfile = savedProfile;
  try {
    const user = await apiRequest("/auth/me");
    authProfile = { ...savedProfile, user, displayName: user.nickname || user.username };
    adminAccountName.textContent = authProfile.displayName;
    adminShell.hidden = false;
    adminGate.hidden = true;
    loadLogs();
  } catch (error) {
    if (error.network) {
      // 网络异常不代表令牌失效：保留登录态，仅提示
      authProfile = null;
      showGate(`无法确认登录状态：${error.message}`);
      return;
    }
    handleAuthFailure("登录状态已失效，请重新登录。");
  }
}

/* ---------------- 列表加载与渲染 ---------------- */

async function loadLogs() {
  const requestId = ++listRequestId;
  const { page, pageSize, keyword } = listState;
  const query = new URLSearchParams({ page: String(page), pageSize: String(pageSize) });
  if (keyword) {
    query.set("keyword", keyword);
  }

  showTableState("loading", "加载中…");

  try {
    const data = await apiRequest(`/admin/logs?${query.toString()}`);
    if (requestId !== listRequestId) return; // 过期响应，丢弃

    listState.total = Number(data?.total || 0);
    listState.rows = Array.isArray(data?.list) ? data.list : [];
    listState.totalPages = Math.max(1, Math.ceil(listState.total / pageSize));

    renderTable();
    renderPagination();
    if (listState.rows.length === 0) {
      showTableState("empty", listState.keyword ? "未找到匹配的日志" : "暂无数据");
    } else {
      hideTableState();
    }
  } catch (error) {
    if (requestId !== listRequestId) return;
    if (error.code === 401 || error.status === 401) {
      handleAuthFailure("登录状态已失效，请重新登录。");
      return;
    }
    if (isForbidden(error)) {
      // 非管理员：日志接口已迁至 /api/admin/logs 并要求 ADMIN 角色，提示后停留登录门
      adminShell.hidden = true;
      showGate("该账号无管理员权限，请使用管理员账号登录。");
      showToast("该账号无管理员权限");
      return;
    }
    listState.rows = [];
    listState.total = 0;
    listState.totalPages = 1;
    renderTable();
    pagination.hidden = true; // 错误态下隐藏分页，避免“共 0 条”与错误提示矛盾
    showTableState("error", `加载失败：${error.message}`);
  }
}

function showTableState(kind, message) {
  logTable.hidden = kind === "loading" || kind === "error" || kind === "empty";
  tableState.hidden = false;
  tableState.dataset.kind = kind;
  if (kind === "error") {
    tableState.innerHTML =
      `<p class="admin-state-text">${escapeHtml(message)}</p>` +
      `<button class="admin-btn admin-btn-plain" type="button" id="retryButton">重试</button>`;
    tableState.querySelector("#retryButton").addEventListener("click", () => loadLogs());
  } else {
    tableState.innerHTML = `<p class="admin-state-text">${escapeHtml(message)}</p>`;
  }
}

function hideTableState() {
  tableState.hidden = true;
  tableState.innerHTML = "";
  logTable.hidden = false;
}

function renderTable() {
  const startIndex = (listState.page - 1) * listState.pageSize;
  logTableBody.innerHTML = listState.rows
    .map((row, index) => {
      const published = Number(row.publishStatus) === 1;
      return `<tr>
        <td class="col-index">${startIndex + index + 1}</td>
        <td class="cell-title"><span class="cell-title-text" title="${escapeHtml(row.title)}">${escapeHtml(row.title)}</span></td>
        <td class="col-type">${escapeHtml(row.type)}</td>
        <td class="col-time">${escapeHtml(row.createTime || "—")}</td>
        <td class="col-status">
          <button
            type="button"
            class="admin-switch${published ? " is-on" : ""}"
            role="switch"
            aria-checked="${published}"
            aria-label="${published ? "已发布，点击下线" : "未发布，点击发布"}：${escapeHtml(row.title)}"
            data-id="${escapeHtml(row.id)}"
            data-status="${published ? 1 : 0}"
          ><span class="admin-switch-knob" aria-hidden="true"></span></button>
        </td>
        <td class="col-views">${Number(row.viewCount || 0)}</td>
        <td class="col-actions">
          <button class="admin-btn admin-btn-small admin-btn-primary" type="button" data-action="edit" data-id="${escapeHtml(row.id)}">编辑</button>
          <button class="admin-btn admin-btn-small admin-btn-danger" type="button" data-action="delete" data-id="${escapeHtml(row.id)}">删除</button>
        </td>
      </tr>`;
    })
    .join("");
}

/* ---------------- 发布状态 Switch ---------------- */

async function togglePublishStatus(switchButton) {
  const id = switchButton.dataset.id;
  if (statusPendingIds.has(id)) return; // 请求进行中，防抖

  const currentStatus = Number(switchButton.dataset.status);
  const nextStatus = currentStatus === 1 ? 0 : 1;

  // 先翻转视觉状态，失败再回滚
  applySwitchState(switchButton, nextStatus);
  switchButton.classList.add("is-loading");
  switchButton.disabled = true;
  statusPendingIds.add(id);

  try {
    await apiRequest(`/admin/logs/${id}/status`, { method: "PUT", body: { status: nextStatus } });
    showToast(nextStatus === 1 ? "发布成功" : "已下线");
  } catch (error) {
    applySwitchState(switchButton, currentStatus); // 回滚开关状态
    if (error.code === 401 || error.status === 401) {
      handleAuthFailure("登录状态已失效，请重新登录。");
      return;
    }
    showToast(error.message || "状态更新失败，请稍后重试。");
  } finally {
    statusPendingIds.delete(id);
    switchButton.classList.remove("is-loading");
    switchButton.disabled = false;
  }
}

function applySwitchState(switchButton, status) {
  const published = Number(status) === 1;
  switchButton.dataset.status = published ? "1" : "0";
  switchButton.classList.toggle("is-on", published);
  switchButton.setAttribute("aria-checked", String(published));
  switchButton.setAttribute("aria-label", published ? "已发布，点击下线" : "未发布，点击发布");
}

/* ---------------- 分页 ---------------- */

function renderPagination() {
  pagination.hidden = false;
  const { page, total, totalPages } = listState;
  const pageButtons = buildPageNumbers(page, totalPages)
    .map((item) => {
      if (item === "...") {
        return `<span class="admin-page-ellipsis">…</span>`;
      }
      return `<button
        class="admin-page${item === page ? " is-current" : ""}"
        type="button"
        data-page="${item}"
        ${item === page ? 'aria-current="page"' : ""}>${item}</button>`;
    })
    .join("");

  pagination.innerHTML = `
    <div class="admin-pages">
      <button class="admin-page-nav" type="button" data-page="${page - 1}" ${page <= 1 ? "disabled" : ""} aria-label="上一页">&lt;</button>
      ${pageButtons}
      <button class="admin-page-nav" type="button" data-page="${page + 1}" ${page >= totalPages ? "disabled" : ""} aria-label="下一页">&gt;</button>
    </div>
    <div class="admin-page-meta">
      <span class="admin-jump">前往 <input id="jumpPageInput" type="text" inputmode="numeric" value="${page}" aria-label="跳转页码" /> 页</span>
      <span class="admin-total">共 ${total} 条记录</span>
    </div>`;

  const jumpInput = pagination.querySelector("#jumpPageInput");
  jumpInput.addEventListener("keydown", (event) => {
    if (event.key === "Enter") {
      event.preventDefault();
      jumpToPage(jumpInput.value);
      jumpInput.blur();
    }
  });
  jumpInput.addEventListener("change", () => jumpToPage(jumpInput.value));
}

/** 生成页码按钮序列：总页数 ≤ 7 全量展示，否则展示首尾页与当前页附近窗口 */
function buildPageNumbers(page, totalPages) {
  if (totalPages <= 7) {
    return Array.from({ length: totalPages }, (_, i) => i + 1);
  }
  const numbers = new Set([1, totalPages, page - 1, page, page + 1]);
  const sorted = [...numbers].filter((n) => n >= 1 && n <= totalPages).sort((a, b) => a - b);
  const result = [];
  sorted.forEach((n, i) => {
    if (i > 0 && n - sorted[i - 1] > 1) {
      result.push("...");
    }
    result.push(n);
  });
  return result;
}

function goToPage(nextPage) {
  const target = Math.min(Math.max(nextPage, 1), listState.totalPages);
  if (target === listState.page) return;
  listState.page = target;
  loadLogs(); // 翻页保留搜索词
  tableWrap.scrollIntoView({ block: "start", behavior: "smooth" }); // 回到列表顶部
}

function jumpToPage(rawValue) {
  const trimmed = String(rawValue ?? "").trim();
  if (!/^\d+$/.test(trimmed)) {
    // 非数字不发起请求，还原为当前页
    const jumpInput = pagination.querySelector("#jumpPageInput");
    if (jumpInput) jumpInput.value = listState.page;
    return;
  }
  const target = Math.min(Math.max(Number(trimmed), 1), listState.totalPages);
  const jumpInput = pagination.querySelector("#jumpPageInput");
  if (jumpInput) jumpInput.value = target;
  if (target !== listState.page) {
    goToPage(target);
  }
}

/* ---------------- 新增 / 编辑弹窗 ---------------- */

function openLogModal(mode, detail = null) {
  editingLogId = mode === "edit" ? detail.id : null;
  logModalTitle.textContent = mode === "edit" ? "编辑日志" : "新增日志";
  logTitleInput.value = detail?.title ?? "";
  logTypeInput.value = detail?.type ?? DEFAULT_LOG_TYPE;
  logContentInput.value = detail?.content ?? "";
  logFormMessage.textContent = "";
  logSubmitButton.disabled = false;
  logSubmitButton.textContent = "保存";
  modalReturnFocus = document.activeElement;
  logModal.hidden = false;
  logTitleInput.focus();
}

function closeLogModal() {
  logModal.hidden = true;
  editingLogId = null;
  if (modalReturnFocus instanceof HTMLElement) {
    modalReturnFocus.focus();
    modalReturnFocus = null;
  }
}

async function openEditModal(id) {
  try {
    const detail = await apiRequest(`/admin/logs/${id}`);
    openLogModal("edit", detail);
  } catch (error) {
    if (error.code === 401 || error.status === 401) {
      handleAuthFailure("登录状态已失效，请重新登录。");
      return;
    }
    showToast(error.message || "获取日志详情失败。");
  }
}

async function submitLogForm(event) {
  event.preventDefault();
  if (!logForm.checkValidity()) {
    logForm.reportValidity();
    return;
  }

  const title = logTitleInput.value.trim();
  const type = logTypeInput.value.trim() || DEFAULT_LOG_TYPE;
  const content = logContentInput.value.trim();
  if (!title || !content) {
    logFormMessage.textContent = "请填写标题和内容。";
    return;
  }

  logSubmitButton.disabled = true;
  logSubmitButton.textContent = "保存中…";
  logFormMessage.textContent = "";

  try {
    if (editingLogId) {
      await apiRequest(`/admin/logs/${editingLogId}`, { method: "PUT", body: { title, type, content } });
      showToast("保存成功");
      closeLogModal();
      await loadLogs(); // 编辑后刷新当前页（保留搜索词与页码）
    } else {
      await apiRequest("/admin/logs", { method: "POST", body: { title, type, content, status: 0 } });
      showToast("新增成功");
      closeLogModal();
      listState.page = 1; // 新日志按时间倒序排在最前，回到第 1 页
      await loadLogs();
    }
  } catch (error) {
    if (error.code === 401 || error.status === 401) {
      handleAuthFailure("登录状态已失效，请重新登录。");
      return;
    }
    logFormMessage.textContent = error.message || "保存失败，请稍后重试。";
    logSubmitButton.disabled = false;
    logSubmitButton.textContent = "保存";
  }
}

/* ---------------- 删除确认 ---------------- */

function openConfirmModal(id) {
  deletingLogId = id;
  confirmDeleteButton.disabled = false;
  confirmDeleteButton.textContent = "确定删除";
  modalReturnFocus = document.activeElement;
  confirmModal.hidden = false;
  confirmDeleteButton.focus();
}

function closeConfirmModal() {
  confirmModal.hidden = true;
  deletingLogId = null;
  if (modalReturnFocus instanceof HTMLElement) {
    modalReturnFocus.focus();
    modalReturnFocus = null;
  }
}

async function confirmDelete() {
  if (deletingLogId == null) return;
  confirmDeleteButton.disabled = true;
  confirmDeleteButton.textContent = "删除中…";

  try {
    await apiRequest(`/admin/logs/${deletingLogId}`, { method: "DELETE" });
    showToast("删除成功");
    const wasLastOnPage = listState.rows.length === 1 && listState.page > 1;
    closeConfirmModal();
    if (wasLastOnPage) {
      listState.page -= 1; // 删除当前页最后一条后回退上一页
    }
    await loadLogs();
  } catch (error) {
    if (error.code === 401 || error.status === 401) {
      handleAuthFailure("登录状态已失效，请重新登录。");
      return;
    }
    showToast(error.message || "删除失败，请稍后重试。");
    confirmDeleteButton.disabled = false;
    confirmDeleteButton.textContent = "确定删除";
  }
}

/* ---------------- 事件绑定 ---------------- */

function doSearch() {
  listState.keyword = searchInput.value.trim();
  listState.page = 1; // 搜索/清空搜索均重置到第 1 页（清空后即恢复全量）
  loadLogs();
}

function bindEvents() {
  createLogButton.addEventListener("click", () => openLogModal("create"));

  searchButton.addEventListener("click", doSearch);
  searchInput.addEventListener("keydown", (event) => {
    if (event.key === "Enter") {
      event.preventDefault();
      doSearch();
    }
  });

  logTableBody.addEventListener("click", (event) => {
    const target = event.target.closest("button");
    if (!target) return;

    if (target.classList.contains("admin-switch")) {
      togglePublishStatus(target);
      return;
    }
    const action = target.dataset.action;
    if (action === "edit") {
      openEditModal(target.dataset.id);
    } else if (action === "delete") {
      openConfirmModal(target.dataset.id);
    }
  });

  pagination.addEventListener("click", (event) => {
    const target = event.target.closest("button[data-page]");
    if (!target || target.disabled) return;
    goToPage(Number(target.dataset.page));
  });

  logForm.addEventListener("submit", submitLogForm);
  confirmDeleteButton.addEventListener("click", confirmDelete);

  // data-close-modal 的取消按钮 / 关闭按钮 / 遮罩统一关闭
  document.querySelectorAll("[data-close-modal]").forEach((element) => {
    element.addEventListener("click", () => {
      const modal = document.getElementById(element.dataset.closeModal);
      if (!modal || modal.hidden) return;
      if (modal === logModal) closeLogModal();
      if (modal === confirmModal) closeConfirmModal();
    });
  });

  // Esc 关闭最上层弹窗
  document.addEventListener("keydown", (event) => {
    if (event.key !== "Escape") return;
    if (!confirmModal.hidden) closeConfirmModal();
    else if (!logModal.hidden) closeLogModal();
  });
}

/* ---------------- 启动 ---------------- */

bindEvents();
initAuth();
