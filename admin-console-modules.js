"use strict";

/* ============================================================
 * PawBites 后台管理控制台 · 业务模块扩展 · FE-B
 * ------------------------------------------------------------
 * 在 FE-A 框架（admin-console.js）之上补齐五个业务模块：
 *   订单管理 orders / 用户管理 users / 评价管理 reviews /
 *   优惠券管理 coupons（接口为 /admin/coupon-templates）/
 *   日志管理 logs，并接通看板「最近订单」行点击跳转定位。
 *
 * 接入方式（全部走 FE-A 预留扩展点，不改动 admin-console.js）：
 *   · PawAdmin.registerModule({id,...,mount}) 按 id 覆盖占位模块；
 *   · PawAdmin.onAction(name, handler) 注册 data-action 处理器；
 *   · PawAdmin.ctx 提供基础设施（api/escapeHtml/money/fmtTime/
 *     toast/showModal/closeModal/confirmBox/onAuthFailure/statusPill/
 *     switchTo/icon/isAuthError/errText）。
 *
 * 命名约定（避免与 FE-A 冲突）：
 *   · 本文件包在 IIFE 内，顶层零全局污染；
 *   · 新 data-action 一律 feb- 前缀（feb-search/feb-page/feb-new/
 *     feb-edit/feb-del/feb-submit/feb-sw-*）。FE-A 的 search/page/
 *     jump/new/edit/del/form-submit 处理器按 data-scope 查它自己的
 *     ui 状态表，对本模块 scope（orders/users/...）天然 no-op，
 *     但重新注册同名 action 会整体覆盖 FE-A 处理器，故必须换名；
 *   · 「goto-order」是唯一刻意覆盖的 action：FE-A 在看板留的是
 *     「订单模块建设中」占位 toast，此处按其预留的互调方式接通
 *     （switchTo + modules.get("orders").locate(orderNo)）。
 *
 * 接口契约备忘（与 backend controller/admin 逐一核对）：
 *   · 订单 GET /admin/orders?page&size&orderNo&keyword&status&from&to
 *     → PageResult{records,total}；GET /{id} 详情含 items/itemsSubtotal/
 *     user；POST /{id}/cancel 取消（回补库存 + 退券）。orderNo 为精确
 *     匹配、keyword 为下单人模糊，单搜索框按「PB 开头视为订单号」分流。
 *   · 用户 GET /admin/users → PageResult；GET /{id} 聚合详情；
 *     PUT /{id}/status {status:ACTIVE|INACTIVE}（禁用 ADMIN 被 400 拒绝）。
 *   · 评价 GET/POST/PUT/DELETE /admin/reviews(+/{id}/status)，
 *     状态为字符串 ACTIVE/INACTIVE。
 *   · 券模板 /admin/coupon-templates：GET 全量列表（不分页），
 *     面额/门槛/天数为整数；PUT /{id}/status 字符串状态。
 *   · 日志 /admin/logs：列表参数名是 pageSize（不是 size），响应为
 *     {total,list}（不是 PageResult）；发布状态是数字 0/1，
 *     PUT /{id}/status body {status:0|1}，与其它模块字符串状态不同。
 * ============================================================ */

(function () {
  const PA = window.PawAdmin;
  if (!PA || !PA.ctx || typeof PA.registerModule !== "function") {
    return; // 框架未就绪（不应发生）：静默退出，不影响宿主页
  }
  const ctx = PA.ctx;
  const esc = ctx.escapeHtml;

  /* ---------------- 常量 ---------------- */

  /** 列表每页条数（与 FE-A/旧日志页一致：10 条/页） */
  const FEB_PAGE_SIZE = 10;
  /** 弹窗商品下拉一次性拉取条数（后端 size clamp 到 [1,100]） */
  const FEB_OPTION_SIZE = 100;
  /** 日志类型 datalist 候选 */
  const FEB_LOG_TYPES = ["AI自习室通知", "版本更新", "运营公告"];
  /** 缩略图加载失败时的占位色块调色板（与 FE-A 一致） */
  const FEB_THUMB_COLORS = [
    "#7f9cf5", "#68d391", "#f6ad55", "#f687b3", "#63b3ed", "#f6e05e",
    "#b794f4", "#4fd1c5", "#fc8181", "#9ae6b4", "#fbd38d", "#90cdf4",
    "#ecc94b", "#fed7d7",
  ];

  /* ---------------- 补充样式（styles.css 未覆盖的少量类） ---------------- */

  /**
   * 注入 FE-B 需要的少量样式：右侧抽屉（pa-dwrap/pa-dpanel/pa-kv/pa-mini-item）、
   * 日期输入（pa-date）、黄色警示条（pa-warnbanner）。全部 scoped 到 #pa-console，
   * 取值与原型/现有配色一致；不改动 styles.css 与 admin-console.js。
   */
  function febInjectStyles() {
    if (document.getElementById("pa-feb-styles")) return;
    const style = document.createElement("style");
    style.id = "pa-feb-styles";
    style.textContent = [
      "#pa-console .pa-date{height:30px;border:1px solid var(--pa-border2);border-radius:4px;padding:0 8px;font-size:12.5px;color:var(--pa-text2);outline:none;background:#fff;font-family:inherit;}",
      "#pa-console .pa-date:focus{border-color:var(--pa-primary);}",
      "#pa-console .pa-warnbanner{display:flex;gap:8px;align-items:flex-start;background:#fdf6ec;border:1px solid #faecd8;color:#b88230;border-radius:4px;padding:9px 11px;font-size:12.5px;margin-bottom:12px;line-height:1.5;}",
      "#pa-console .pa-warnbanner svg.pa-ico{color:#E6A23C;margin-top:2px;}",
      "#pa-console .pa-dwrap{position:fixed;inset:0;z-index:70;}",
      "#pa-console .pa-dmask{position:absolute;inset:0;background:rgba(31,45,61,.35);}",
      "#pa-console .pa-dpanel{position:absolute;top:0;right:0;bottom:0;width:470px;max-width:92%;background:#fff;box-shadow:-8px 0 26px rgba(0,0,0,.14);display:flex;flex-direction:column;animation:pa-feb-drawer-in .22s ease;}",
      "@keyframes pa-feb-drawer-in{from{transform:translateX(30px);opacity:.4}to{transform:none;opacity:1}}",
      "#pa-console .pa-dhd{display:flex;align-items:center;padding:15px 20px;border-bottom:1px solid var(--pa-border);font-size:15.5px;font-weight:600;}",
      "#pa-console .pa-dbd{padding:16px 20px;overflow-y:auto;}",
      "#pa-console .pa-dsec{margin-bottom:18px;}",
      "#pa-console .pa-dsec:last-child{margin-bottom:0;}",
      "#pa-console .pa-dsec h4{margin:0 0 8px;font-size:13px;color:var(--pa-text3);font-weight:500;letter-spacing:.5px;}",
      "#pa-console dl.pa-kv{display:grid;grid-template-columns:76px 1fr;gap:6px 10px;font-size:13px;margin:0;}",
      "#pa-console dl.pa-kv dt{color:var(--pa-text3);}",
      "#pa-console dl.pa-kv dd{margin:0;}",
      "#pa-console .pa-mini-item{display:flex;align-items:center;gap:9px;padding:7px 0;border-bottom:1px dashed var(--pa-border);font-size:12.5px;}",
      "#pa-console .pa-mini-item:last-child{border-bottom:0;}",
    ].join("\n");
    document.head.appendChild(style);
  }

  /* ---------------- 通用小组件（写法照抄 FE-A，action 换 feb- 前缀） ---------------- */

  /** 下拉筛选框（change 由本文件根容器监听路由） */
  function febSel(filter, options, current) {
    return '<select class="pa-sel" data-filter="' + esc(filter) + '">' +
      options.map((o) =>
        '<option value="' + esc(o.v) + '"' + (o.v === current ? " selected" : "") + ">" +
        esc(o.t) + "</option>").join("") +
      "</select>";
  }

  /** 日期输入（订单时间范围筛选） */
  function febDateBox(filter, value, label) {
    return '<input type="date" class="pa-date" data-filter="' + esc(filter) + '" value="' + esc(value || "") + '"' +
      (label ? ' aria-label="' + esc(label) + '"' : "") + " />";
  }

  /** 关键字搜索框（按钮/回车触发 feb-search） */
  function febKw(scope, placeholder, value) {
    return '<span class="pa-kw">' +
      '<input class="pa-kw-inp" data-scope="' + esc(scope) + '" type="text" maxlength="64" ' +
      'value="' + esc(value || "") + '" placeholder="' + esc(placeholder) + '" ' +
      'aria-label="' + esc(placeholder) + '" autocomplete="off" />' +
      '<button class="pa-btn" type="button" data-action="feb-search" data-scope="' + esc(scope) + '">' +
      ctx.icon("search") + "搜索</button></span>";
  }

  /** Switch 按钮（乐观更新由各处理器负责） */
  function febSwitch(on, action, id) {
    return '<button type="button" class="pa-switch" role="switch" aria-checked="' + (on ? "true" : "false") +
      '" data-action="' + esc(action) + '" data-id="' + esc(id) + '" aria-label="状态开关"></button>';
  }

  /** 生成页码序列（与 FE-A 同款算法） */
  function febBuildPages(page, totalPages) {
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

  /** 分页条（跳页输入框 id 用 febjump- 前缀，避免命中 FE-A 的 jump- 前缀监听） */
  function febPager(scope, page, totalPages, total) {
    const pages = Math.max(1, totalPages);
    const parts = [];
    parts.push('<button class="pa-pgbtn" type="button" data-action="feb-page" data-scope="' + esc(scope) +
      '" data-p="prev"' + (page <= 1 ? " disabled" : "") + ' aria-label="上一页">‹</button>');
    febBuildPages(page, pages).forEach((n) => {
      if (n === "...") {
        parts.push('<span class="pa-pg-dots">…</span>');
        return;
      }
      parts.push('<button class="pa-pgbtn' + (n === page ? " is-on" : "") + '" type="button" data-action="feb-page" ' +
        'data-scope="' + esc(scope) + '" data-p="' + n + '"' + (n === page ? ' aria-current="page"' : "") + ">" + n + "</button>");
    });
    parts.push('<button class="pa-pgbtn" type="button" data-action="feb-page" data-scope="' + esc(scope) +
      '" data-p="next"' + (page >= pages ? " disabled" : "") + ' aria-label="下一页">›</button>');
    parts.push('<span class="pa-jump">前往<input id="febjump-' + esc(scope) + '" value="' + page +
      '" inputmode="numeric" aria-label="跳转页码" />页' +
      '<button class="pa-pgbtn" type="button" data-action="feb-jump" data-scope="' + esc(scope) + '">GO</button></span>');
    parts.push('<span class="pa-total">共 ' + total + " 条记录</span>");
    return '<div class="pa-pager">' + parts.join("") + "</div>";
  }

  /** 列表状态行：loading / empty / error（error 带 feb-retry 重试按钮） */
  function febStateRow(colspan, kind, message, scope) {
    if (kind === "error") {
      return '<tr class="pa-empty"><td colspan="' + colspan + '"><div class="pa-state is-error"><span>' + esc(message) + "</span>" +
        '<button class="pa-btn" type="button" data-action="feb-retry" data-scope="' + esc(scope) + '">重试</button></div></td></tr>';
    }
    return '<tr class="pa-empty"><td colspan="' + colspan + '">' + esc(message) + "</td></tr>";
  }

  /** 表单字段（id 前缀 f-，与 FE-A 弹窗字段同一取值函数 febVal 兼容） */
  function febFi(label, id, value, hint, req, type, attrs) {
    return '<div class="pa-fi"><label>' + (req ? '<span class="req">*</span>' : "") + esc(label) + "</label>" +
      '<input id="f-' + esc(id) + '" type="' + (type || "text") + '" ' + (attrs || "") +
      ' value="' + esc(value ?? "") + '" />' +
      (hint ? '<span class="pa-hint">' + hint + "</span>" : "") + "</div>";
  }

  /** 表单取值（去空白） */
  function febVal(id) {
    const el = document.getElementById("f-" + id);
    return el ? String(el.value).trim() : "";
  }

  /** 缩略图：img 加载失败 onerror 换占位色块（首字），写法同 FE-A thumbHtml */
  function febThumb(seedId, name, imageUrl) {
    const seed = Number(seedId ?? 0);
    const color = FEB_THUMB_COLORS[Math.abs(Number.isFinite(seed) ? seed : 0) % FEB_THUMB_COLORS.length];
    const label = esc(String(name || "").slice(0, 1) || "·");
    return '<span class="pa-thumb is-img" style="background:' + color + '">' +
      (imageUrl
        ? '<img src="' + esc(imageUrl) + '" alt="" loading="lazy" ' +
          'onerror="this.onerror=null;var w=this.closest(\'.pa-thumb\');if(w){w.classList.add(\'is-broken\');}this.remove();" />'
        : "") +
      '<span class="pa-thumb-fb">' + label + "</span></span>";
  }

  /** 用户持券展示状态（后端输出展示态 int：0 可用 / 1 已使用 / 2 已过期） */
  function febCouponStatePill(status) {
    const n = Number(status);
    if (n === 0) return '<span class="pa-pill p-green">可用</span>';
    if (n === 1) return '<span class="pa-pill p-gray">已使用</span>';
    if (n === 2) return '<span class="pa-pill p-red">已过期</span>';
    return '<span class="pa-pill p-gray">—</span>';
  }

  /* ---------------- 模块状态（切换模块保留筛选/页码） ---------------- */

  const febSt = {
    orders: { seq: 0, status: "all", from: "", to: "", q: "", page: 1, size: FEB_PAGE_SIZE, rows: [], total: 0, totalPages: 1 },
    users: { seq: 0, status: "all", q: "", page: 1, size: FEB_PAGE_SIZE, rows: [], total: 0, totalPages: 1 },
    reviews: { seq: 0, status: "all", q: "", page: 1, size: FEB_PAGE_SIZE, rows: [], total: 0, totalPages: 1 },
    logs: { seq: 0, q: "", page: 1, size: FEB_PAGE_SIZE, rows: [], total: 0, totalPages: 1 },
  };
  /** 券模板接口不分页（全量列表），单独存行数据 */
  let febCouponRows = [];
  let febCouponSeq = 0;

  /** 当前挂载的内容区容器（switchTo 清空其 innerHTML 后重新 mount 赋值） */
  let febEl = null;
  /** 当前展示的模块 id（nav 捕获监听与各模块 mount 双向维护） */
  let febCurrentView = null;
  /** 本文件弹窗的编辑上下文（与 FE-A 的 editCtx 互不影响） */
  let febEdit = null;
  /** 用户详情右侧抽屉元素（懒创建，挂在 #pa-console 下，不随模块切换销毁） */
  let febDrawer = null;

  /** 各 scope 的重新加载器（搜索 / 翻页 / 重试共用，翻页保留条件） */
  const febLoaders = {
    orders: () => loadOrders(),
    users: () => loadUsers(),
    reviews: () => loadReviews(),
    coupons: () => loadCoupons(),
    logs: () => loadLogs(),
  };

  /* ---------------- 通用：鉴权失败 / 列表加载失败 ---------------- */

  function febAuthFail() {
    febCloseDrawer();
    ctx.onAuthFailure("登录状态已失效，请重新登录。");
  }

  function febLoadFailed(scope, error, render, fallback) {
    if (ctx.isAuthError(error)) {
      febAuthFail();
      return;
    }
    const st = febSt[scope];
    if (st) {
      st.rows = [];
      st.total = 0;
      st.totalPages = 1;
    } else if (scope === "coupons") {
      febCouponRows = [];
    }
    render("error", "加载失败：" + ctx.errText(error, fallback));
  }

  /* ---------------- 通用：Switch 乐观更新（失败回滚） ---------------- */

  /**
   * 布尔型开关切换（展示状态类）：先翻转视觉，成功同步本地行，失败回滚。
   * @param request  (nextOn:boolean) => Promise
   * @param applyRow (nextOn:boolean) => void 同步本地行状态
   * @param message  (nextOn:boolean) => toast 文案
   */
  function febToggleSwitch(btn, request, applyRow, message) {
    if (btn.disabled) return;
    const wasOn = btn.getAttribute("aria-checked") === "true";
    const next = !wasOn;
    btn.setAttribute("aria-checked", String(next)); // 乐观更新
    btn.disabled = true;
    request(next)
      .then(() => {
        if (applyRow) applyRow(next);
        ctx.toast(message(next));
      })
      .catch((error) => {
        btn.setAttribute("aria-checked", String(wasOn)); // 回滚
        if (ctx.isAuthError(error)) febAuthFail();
        else ctx.toast(ctx.errText(error, "状态更新失败，请稍后重试。"), "err");
      })
      .finally(() => {
        btn.disabled = false;
      });
  }

  /** 删除当前页最后一条后自动回退一页（各删除回调共用） */
  function febStepBackPage(scope) {
    const st = febSt[scope];
    if (st && st.rows.length === 1 && st.page > 1) st.page -= 1;
  }

  /** 使所有在途列表请求失效（seq 前移，晚到的响应对不上号即被丢弃） */
  function febInvalidateRequests() {
    Object.keys(febSt).forEach((key) => {
      febSt[key].seq += 1;
    });
    febCouponSeq += 1;
  }

  /** 渲染前校验：仅当该模块仍是当前展示模块时才允许写入内容区 */
  function febActive(moduleId) {
    return febCurrentView === moduleId;
  }

  /**
   * mount 前置：记录容器并使所有在途列表请求失效。
   * switchTo 只清空内容区不取消请求，若不失效，上一模块的慢响应返回后
   * 会把渲染结果覆盖到新模块的内容区上。
   */
  function febPrepareMount(container, moduleId) {
    febEl = container;
    febCurrentView = moduleId;
    febInvalidateRequests();
  }

  /* ============================================================
   * 模块：订单管理
   * ============================================================ */

  function renderOrdersCard(kind, message) {
    if (!febEl || !febActive("orders")) return; // 已切走则丢弃本次渲染
    const st = febSt.orders;
    febEl.innerHTML =
      '<div class="pa-card"><div class="pa-card-hd"><h2>订单管理</h2><p>只读查阅 + 管理员兜底取消；取消将回补库存并退还优惠券，不可逆</p></div>' +
      '<div class="pa-toolbar">' +
      febSel("o-status", [{ v: "all", t: "全部状态" }, { v: "CREATED", t: "已创建" }, { v: "CANCELLED", t: "已取消" }], st.status) +
      febDateBox("o-from", st.from, "开始日期") +
      '<span class="pa-dim" style="font-size:12px;">至</span>' +
      febDateBox("o-to", st.to, "结束日期") +
      '<span class="pa-sp"></span>' + febKw("orders", "订单号 / 用户", st.q) +
      "</div>" +
      '<div class="pa-tblwrap"><table class="pa-tbl"><thead><tr><th>订单号</th><th>下单用户</th><th class="pa-num">件数</th>' +
      '<th class="pa-num">总额</th><th>优惠</th><th>状态</th><th>下单时间</th><th class="pa-nowrap">操作</th>' +
      "</tr></thead><tbody>" +
      (kind === "ok"
        ? st.rows.map((o) => febOrderRowHtml(o)).join("")
        : febStateRow(8, kind, message, "orders")) +
      "</tbody></table></div>" +
      (kind === "ok" ? febPager("orders", st.page, st.totalPages, st.total) : "") +
      "</div>";
  }

  /** 优惠列：用券显示券名 + 抵扣额；未用券 / 取消后退券（券字段为 null）显示 — */
  function febOrderCouponCell(o) {
    if (o.couponName) {
      return '<span class="pa-pill p-orange" title="' + esc(o.couponName) + '">' + esc(o.couponName) +
        " −¥" + Number(o.couponAmount || 0).toFixed(2) + "</span>";
    }
    return '<span class="pa-dim">—</span>';
  }

  function febOrderRowHtml(o) {
    return "<tr>" +
      '<td class="pa-mono">' + esc(o.orderNo) + "</td>" +
      "<td>" + esc(o.ownerName || "—") + "</td>" +
      '<td class="pa-num">' + Number(o.itemCount || 0) + "</td>" +
      '<td class="pa-num">' + ctx.money(o.totalAmount) + "</td>" +
      "<td>" + febOrderCouponCell(o) + "</td>" +
      "<td>" + ctx.statusPill(o.status) + "</td>" +
      "<td>" + esc(ctx.fmtTime(o.createdAt)) + "</td>" +
      '<td class="pa-nowrap">' +
      '<button class="pa-linkbtn" type="button" data-action="order-detail" data-id="' + esc(o.id) + '">详情</button>' +
      (o.status === "CREATED"
        ? '<button class="pa-linkbtn is-danger" type="button" data-action="order-cancel" data-id="' + esc(o.id) + '">取消</button>'
        : "") +
      "</td></tr>";
  }

  /**
   * 单搜索框参数分流：订单号为 PB 前缀（后端 eq 精确匹配），其余视为下单人关键字
   * （后端按用户名/昵称模糊 + 游客 guestId 前缀匹配）。
   */
  function febOrderSearchParams(st) {
    const query = new URLSearchParams({ page: String(st.page), size: String(st.size) });
    const kw = st.q.trim();
    if (kw) {
      if (/^pb/i.test(kw)) query.set("orderNo", kw.toUpperCase());
      else query.set("keyword", kw);
    }
    if (st.status !== "all") query.set("status", st.status);
    if (st.from) query.set("from", st.from);
    if (st.to) query.set("to", st.to);
    return query;
  }

  async function loadOrders() {
    const st = febSt.orders;
    const seq = ++st.seq;
    renderOrdersCard("loading", "加载中…");
    // 日期闭区间前置校验（后端同样校验 400，前端先拦截体验更好）
    if (st.from && st.to && st.from > st.to) {
      st.rows = [];
      st.total = 0;
      st.totalPages = 1;
      renderOrdersCard("error", "开始日期不能晚于结束日期");
      return;
    }
    try {
      const data = await ctx.api("/admin/orders?" + febOrderSearchParams(st).toString());
      if (seq !== st.seq) return; // 过期响应，丢弃
      st.rows = Array.isArray(data?.records) ? data.records : [];
      st.total = Number(data?.total || 0);
      st.totalPages = Math.max(1, Math.ceil(st.total / st.size));
      const filtered = st.q.trim() || st.status !== "all" || st.from || st.to;
      renderOrdersCard(st.rows.length ? "ok" : "empty", filtered ? "未找到匹配的订单" : "暂无订单");
    } catch (error) {
      if (seq !== st.seq) return;
      febLoadFailed("orders", error, renderOrdersCard, "请稍后重试。");
    }
  }

  /** 订单详情弹窗（lg）：头部 / 用户信息 / 商品快照表 / 金额汇总 / 取消信息 */
  function febOpenOrderDetailModal(d) {
    febEdit = { kind: "order", detail: d };
    const items = Array.isArray(d.items) ? d.items : [];
    const user = d.user;
    const userLine = user
      ? esc(d.ownerName) + "（" + esc(user.username || "—") + " · " + esc(user.email || "邮箱未填") + "）"
      : esc(d.ownerName) + (d.guestId ? "（游客单 · guestId " + esc(String(d.guestId)) + "）" : "");
    const cancelBy = d.cancelBy === "ADMIN" ? "由管理员取消" : d.cancelBy === "USER" ? "由用户取消" : (d.cancelBy || "—");

    const body =
      '<dl class="pa-kv" style="margin:0 0 14px;">' +
      "<dt>状态</dt><dd>" + ctx.statusPill(d.status) + "</dd>" +
      "<dt>下单用户</dt><dd>" + userLine + "</dd>" +
      "<dt>下单时间</dt><dd>" + esc(ctx.fmtTime(d.createdAt)) + "</dd>" +
      (d.status === "CANCELLED"
        ? "<dt>取消信息</dt><dd>" + esc(ctx.fmtTime(d.cancelTime)) + " · " + esc(cancelBy) + "</dd>"
        : "") +
      "</dl>" +
      '<div class="pa-tblwrap"><table class="pa-tbl"><thead><tr><th>商品快照</th><th class="pa-num">单价</th>' +
      '<th class="pa-ctr">数量</th><th class="pa-num">小计</th></tr></thead><tbody>' +
      (items.length
        ? items.map((it) =>
          "<tr><td><span class=\"pa-prod\" style=\"min-width:180px;\">" + febThumb(0, it.productName, it.productImage) +
          "<span>" + esc(it.productName) + "</span></span></td>" +
          '<td class="pa-num">' + ctx.money(it.price) + "</td>" +
          '<td class="pa-ctr">' + Number(it.quantity || 0) + "</td>" +
          '<td class="pa-num">' + ctx.money(it.subtotal) + "</td></tr>").join("")
        : febStateRow(4, "empty", "无商品快照")) +
      "</tbody></table></div>" +
      '<div style="margin-top:12px;font-size:13px;text-align:right;line-height:1.9;">' +
      "<div>商品合计 <b>" + ctx.money(d.itemsSubtotal) + "</b></div>" +
      (d.couponName
        ? '<div style="color:#E6A23C;">优惠券「' + esc(d.couponName) + "」抵扣 −" + ctx.money(d.couponAmount) + "</div>"
        : '<div class="pa-dim">优惠 —</div>') +
      '<div style="font-size:15px;font-weight:600;">实付总额 ' + ctx.money(d.totalAmount) + "</div>" +
      "</div>" +
      '<div class="pa-hint" style="margin-top:10px;">* 商品信息为下单时快照，此后改价改名不影响本订单；已取消订单的优惠券已退还，优惠显示为 —</div>';

    const foot = d.status === "CREATED"
      ? '<button class="pa-btn" type="button" data-action="modal-close">关闭</button>' +
        '<button class="pa-btn pa-btn-danger" type="button" data-action="order-cancel" data-id="' + esc(d.id) + '">' +
        ctx.icon("x") + "取消订单</button>"
      : '<button class="pa-btn pa-btn-pri" type="button" data-action="modal-close">关闭</button>';
    ctx.showModal("lg", "订单详情 " + d.orderNo, body, foot);
  }

  async function febOpenOrderDetail(id) {
    try {
      const d = await ctx.api("/admin/orders/" + id);
      febOpenOrderDetailModal(d);
    } catch (error) {
      if (ctx.isAuthError(error)) febAuthFail();
      else ctx.toast(ctx.errText(error, "获取订单详情失败。"), "err");
    }
  }

  /** 管理员取消订单：红色二次确认 → POST cancel → 刷新列表 + toast */
  function febCancelOrder(id) {
    // 详情弹窗内发起时取详情数据（含券信息），列表行发起时取行数据
    const detail = febEdit && febEdit.kind === "order" && String(febEdit.detail.id) === String(id) ? febEdit.detail : null;
    const row = febSt.orders.rows.find((r) => String(r.id) === String(id));
    const src = detail || row || {};
    const couponHtml = src.couponName
      ? "并<strong>退还用户优惠券「" + esc(src.couponName) + "」</strong>"
      : "";
    ctx.confirmBox(
      "取消订单 " + (src.orderNo || "#" + id) + "？",
      "取消后不可恢复；将自动<strong>回补商品库存</strong>" + couponHtml + "。",
      "确认取消",
      async () => {
        try {
          await ctx.api("/admin/orders/" + id + "/cancel", { method: "POST" });
          ctx.toast("订单已取消：库存已回补" + (src.couponName ? "、优惠券已退还" : ""));
          loadOrders(); // 刷新列表（confirm 关闭时详情弹窗已一并关闭）
        } catch (error) {
          if (ctx.isAuthError(error)) febAuthFail();
          else ctx.toast(ctx.errText(error, "取消失败，请稍后重试。"), "err");
        }
      }
    );
  }

  /* ============================================================
   * 模块：用户管理
   * ============================================================ */

  function febColorOf(id) {
    const seed = Number(id ?? 0);
    return FEB_THUMB_COLORS[Math.abs(Number.isFinite(seed) ? seed : 0) % FEB_THUMB_COLORS.length];
  }

  function renderUsersCard(kind, message) {
    if (!febEl || !febActive("users")) return;
    const st = febSt.users;
    febEl.innerHTML =
      '<div class="pa-card"><div class="pa-card-hd"><h2>用户管理</h2><p>只读查阅 + 禁用/启用；禁用不清数据、不删订单，已签发登录凭证在过期前仍有效</p></div>' +
      '<div class="pa-toolbar">' +
      febSel("u-status", [{ v: "all", t: "全部状态" }, { v: "ACTIVE", t: "正常" }, { v: "INACTIVE", t: "已禁用" }], st.status) +
      '<span class="pa-sp"></span>' + febKw("users", "用户名 / 昵称 / 邮箱", st.q) +
      "</div>" +
      '<div class="pa-tblwrap"><table class="pa-tbl"><thead><tr><th class="pa-ctr">ID</th><th>用户</th><th>邮箱</th>' +
      '<th class="pa-num">订单数</th><th class="pa-num">消费金额</th><th class="pa-ctr">持券</th><th>账号状态</th>' +
      '<th>注册时间</th><th class="pa-nowrap">操作</th></tr></thead><tbody>' +
      (kind === "ok"
        ? st.rows.map((u) => febUserRowHtml(u)).join("")
        : febStateRow(9, kind, message, "users")) +
      "</tbody></table></div>" +
      (kind === "ok" ? febPager("users", st.page, st.totalPages, st.total) : "") +
      "</div>";
  }

  function febUserRowHtml(u) {
    const nickname = u.nickname || u.username || ("用户 #" + u.id);
    return "<tr>" +
      '<td class="pa-ctr">' + esc(u.id) + "</td>" +
      '<td><span class="pa-prod" style="min-width:120px;">' +
      '<span class="pa-avatar" style="background:' + febColorOf(u.id) + ';">' + esc(String(nickname).slice(0, 1)) + "</span>" +
      "<span><span class=\"pa-cell-main\">" + esc(nickname) + "</span>" +
      '<div class="pa-cell-sub">' + esc(u.username || "—") + "</div></span></span></td>" +
      "<td>" + esc(u.email || "—") + "</td>" +
      '<td class="pa-num">' + Number(u.orderCount || 0) + "</td>" +
      '<td class="pa-num">' + ctx.money(u.totalSpend) + "</td>" +
      '<td class="pa-ctr">' +
      (Number(u.couponTotal || 0) > 0
        ? '<span class="pa-pill p-orange">' + Number(u.couponAvailable || 0) + " 可用 / " + Number(u.couponTotal || 0) + "</span>"
        : '<span class="pa-dim">—</span>') +
      "</td>" +
      "<td>" + febSwitch(u.status === "ACTIVE", "feb-sw-user", u.id) + "</td>" +
      "<td>" + esc(ctx.fmtTime(u.createdAt)) + "</td>" +
      '<td class="pa-nowrap">' +
      '<button class="pa-linkbtn" type="button" data-action="user-detail" data-id="' + esc(u.id) + '">详情</button>' +
      (u.status === "ACTIVE"
        ? '<button class="pa-linkbtn is-danger" type="button" data-action="user-disable" data-id="' + esc(u.id) + '">禁用</button>'
        : '<button class="pa-linkbtn" type="button" data-action="user-enable" data-id="' + esc(u.id) + '">启用</button>') +
      "</td></tr>";
  }

  async function loadUsers() {
    const st = febSt.users;
    const seq = ++st.seq;
    renderUsersCard("loading", "加载中…");
    const query = new URLSearchParams({ page: String(st.page), size: String(st.size) });
    const kw = st.q.trim();
    if (kw) query.set("keyword", kw);
    if (st.status !== "all") query.set("status", st.status);
    try {
      const data = await ctx.api("/admin/users?" + query.toString());
      if (seq !== st.seq) return;
      st.rows = Array.isArray(data?.records) ? data.records : [];
      st.total = Number(data?.total || 0);
      st.totalPages = Math.max(1, Math.ceil(st.total / st.size));
      renderUsersCard(st.rows.length ? "ok" : "empty", kw || st.status !== "all" ? "未找到匹配的用户" : "暂无用户");
    } catch (error) {
      if (seq !== st.seq) return;
      febLoadFailed("users", error, renderUsersCard, "请稍后重试。");
    }
  }

  /** 更新账号状态（Switch 乐观翻转由调用方传入的 btn 决定）；成功后按本地行重绘 */
  function febPutUserStatus(id, next, switchBtn) {
    const row = febSt.users.rows.find((r) => String(r.id) === String(id));
    if (switchBtn) {
      switchBtn.setAttribute("aria-checked", String(next === "ACTIVE")); // 乐观更新
      switchBtn.disabled = true;
    }
    ctx.api("/admin/users/" + id + "/status", { method: "PUT", body: { status: next } })
      .then(() => {
        if (row) row.status = next;
        ctx.toast(next === "ACTIVE" ? "账号已启用" : "账号已禁用");
        renderUsersCard("ok"); // 重绘行内 Switch 与 禁用/启用 按钮
      })
      .catch((error) => {
        if (switchBtn) switchBtn.setAttribute("aria-checked", String(next !== "ACTIVE")); // 回滚
        if (ctx.isAuthError(error)) febAuthFail();
        else ctx.toast(ctx.errText(error, "状态更新失败，请稍后重试。"), "err"); // 如禁用 ADMIN：后端 400 文案直接展示
      })
      .finally(() => {
        if (switchBtn) switchBtn.disabled = false;
      });
  }

  /** 禁用需二次确认；启用直接执行 */
  function febRequestUserStatus(id, next, switchBtn) {
    if (next !== "INACTIVE") {
      febPutUserStatus(id, next, switchBtn);
      return;
    }
    const row = febSt.users.rows.find((r) => String(r.id) === String(id));
    const name = row ? (row.nickname || row.username || "#" + id) : "#" + id;
    ctx.confirmBox(
      "禁用账号「" + name + "」？",
      "禁用后该用户无法登录与下单，数据与订单保留；已签发的登录凭证在过期前仍有效。",
      "确认禁用",
      () => febPutUserStatus(id, "INACTIVE", switchBtn)
    );
  }

  /* ---------- 用户详情右侧抽屉 ---------- */

  function febEnsureDrawer() {
    if (febDrawer) return;
    febDrawer = document.createElement("div");
    febDrawer.className = "pa-dwrap";
    febDrawer.hidden = true;
    febDrawer.innerHTML =
      '<div class="pa-dmask" data-action="drawer-close"></div>' +
      '<div class="pa-dpanel" role="dialog" aria-modal="true" aria-label="用户详情"></div>';
    document.getElementById("pa-console").appendChild(febDrawer);
  }

  function febCloseDrawer() {
    if (febDrawer) febDrawer.hidden = true;
  }

  function febRenderUserDrawer(d) {
    febEnsureDrawer();
    const panel = febDrawer.querySelector(".pa-dpanel");
    const orders = Array.isArray(d.orders) ? d.orders : [];
    const validOrders = orders.filter((o) => o.status === "CREATED");
    const spend = validOrders.reduce((sum, o) => sum + Number(o.totalAmount || 0), 0);
    const favorites = d.favorites || {};
    const favProducts = Array.isArray(favorites.products) ? favorites.products : [];
    const favTotal = Number(favorites.total || 0);
    const coupons = Array.isArray(d.coupons) ? d.coupons : [];
    const nickname = d.nickname || d.username || ("用户 #" + d.id);

    panel.innerHTML =
      '<div class="pa-dhd">' + esc(nickname) +
      '<button class="pa-iconbtn" type="button" data-action="drawer-close" aria-label="关闭抽屉" style="margin-left:auto;">' +
      ctx.icon("x") + "</button></div>" +
      '<div class="pa-dbd">' +
      '<div class="pa-dsec"><div style="display:flex;align-items:center;gap:12px;">' +
      '<span class="pa-avatar" style="width:44px;height:44px;font-size:17px;background:' + febColorOf(d.id) + ';">' +
      esc(String(nickname).slice(0, 1)) + "</span>" +
      "<span><b style=\"font-size:15px;\">" + esc(nickname) + "</b>　" +
      (d.status === "ACTIVE" ? '<span class="pa-pill p-green">正常</span>' : '<span class="pa-pill p-red">已禁用</span>') +
      '<div class="pa-cell-sub" style="margin-top:3px;">' + esc(d.username || "—") + "</div></span></div></div>" +
      '<div class="pa-dsec"><h4>基本资料</h4><dl class="pa-kv">' +
      "<dt>邮箱</dt><dd>" + esc(d.email || "—") + "</dd>" +
      "<dt>注册时间</dt><dd>" + esc(ctx.fmtTime(d.createdAt)) + "</dd>" +
      "<dt>角色</dt><dd>" + (d.role === "ADMIN" ? '<span class="pa-pill p-blue">管理员</span>' : "普通用户") + "</dd>" +
      "<dt>有效订单</dt><dd>" + validOrders.length + " 笔 · 累计 " + ctx.money(spend) + "</dd>" +
      "</dl></div>" +
      '<div class="pa-dsec"><h4>订单记录（' + orders.length + "）</h4>" +
      (orders.length
        ? orders.map((o) =>
          '<div class="pa-mini-item"><span class="pa-mono" style="flex:1;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" title="' +
          esc(o.orderNo) + '">' + esc(o.orderNo) + "</span>" +
          "<span style=\"flex:none;\">" + ctx.money(o.totalAmount) + "</span>" +
          ctx.statusPill(o.status) +
          '<span class="pa-dim" style="flex:none;">' + esc(ctx.fmtTime(o.createdAt)) + "</span></div>").join("")
        : '<div class="pa-dim">暂无订单</div>') +
      "</div>" +
      '<div class="pa-dsec"><h4>收藏（' + favTotal + "）</h4>" +
      (favProducts.length
        ? favProducts.map((p) =>
          '<div class="pa-mini-item">' + febThumb(p.productId, p.name, p.imageUrl) +
          '<span style="flex:1;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" title="' + esc(p.name) + '">' +
          esc(p.name) + "</span><b style=\"flex:none;\">" + ctx.money(p.price) + "</b></div>").join("") +
          (favTotal > favProducts.length
            ? '<div class="pa-dim" style="padding:6px 0;">… 共 ' + favTotal + " 件收藏，仅展示最近 " + favProducts.length + " 件</div>"
            : "")
        : '<div class="pa-dim">暂无收藏</div>') +
      "</div>" +
      '<div class="pa-dsec"><h4>优惠券（' + coupons.length + "）</h4>" +
      (coupons.length
        ? coupons.map((c) =>
          '<div class="pa-mini-item"><span style="flex:1;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" title="' +
          esc(c.name) + '">' + esc(c.name) + "</span>" +
          '<span class="pa-dim" style="flex:none;">至 ' + esc(ctx.fmtTime(c.expiresAt)) + "</span>" +
          febCouponStatePill(c.status) + "</div>").join("")
        : '<div class="pa-dim">暂无持券</div>') +
      "</div>" +
      '<div class="pa-dsec"><h4>邮箱订阅</h4><div>' +
      (d.subscription && d.subscription.subscribed
        ? '<span class="pa-pill p-green">已订阅</span> <span class="pa-dim">（「订阅回馈券」发放来源）</span>'
        : '<span class="pa-dim">未订阅</span>') +
      "</div></div>" +
      "</div>";
    febDrawer.hidden = false;
  }

  async function febOpenUserDetail(id) {
    try {
      const d = await ctx.api("/admin/users/" + id);
      febRenderUserDrawer(d);
    } catch (error) {
      if (ctx.isAuthError(error)) febAuthFail();
      else ctx.toast(ctx.errText(error, "获取用户详情失败。"), "err");
    }
  }

  /* ============================================================
   * 模块：评价管理
   * ============================================================ */

  function renderReviewsCard(kind, message) {
    if (!febEl || !febActive("reviews")) return;
    const st = febSt.reviews;
    febEl.innerHTML =
      '<div class="pa-card"><div class="pa-card-hd"><h2>评价管理</h2><p>隐藏后 C 端立即不展示；删除为逻辑删除，C 端评价由后台统一维护</p></div>' +
      '<div class="pa-toolbar">' +
      '<button class="pa-btn pa-btn-pri" type="button" data-action="feb-new" data-kind="review">' + ctx.icon("plus") + "新增评价</button>" +
      febSel("r-status", [{ v: "all", t: "全部状态" }, { v: "ACTIVE", t: "展示中" }, { v: "INACTIVE", t: "已隐藏" }], st.status) +
      '<span class="pa-sp"></span>' + febKw("reviews", "内容 / 评价人", st.q) +
      "</div>" +
      '<div class="pa-tblwrap"><table class="pa-tbl"><thead><tr><th>评价人</th><th>内容</th><th>关联商品</th>' +
      '<th>评分</th><th>展示状态</th><th class="pa-ctr">排序号</th><th>时间</th><th class="pa-nowrap">操作</th>' +
      "</tr></thead><tbody>" +
      (kind === "ok"
        ? st.rows.map((r) => febReviewRowHtml(r)).join("")
        : febStateRow(8, kind, message, "reviews")) +
      "</tbody></table></div>" +
      (kind === "ok" ? febPager("reviews", st.page, st.totalPages, st.total) : "") +
      "</div>";
  }

  function febReviewRowHtml(r) {
    const rating = r.rating == null || r.rating === "" ? "—" : '<span class="pa-stars">★</span> ' + Number(r.rating).toFixed(1);
    return "<tr>" +
      '<td><span class="pa-cell-main">' + esc(r.buyerName) + "</span>" +
      ((r.petName || r.petBreed)
        ? '<div class="pa-cell-sub">' + esc(r.petName || "—") + " · " + esc(r.petBreed || "—") + "</div>"
        : "") +
      "</td>" +
      '<td style="max-width:260px;"><div style="display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;" title="' +
      esc(r.content) + '">' + esc(r.content) + "</div></td>" +
      "<td>" + (r.productName ? esc(r.productName) : '<span class="pa-dim">通用</span>') + "</td>" +
      "<td>" + rating + "</td>" +
      "<td>" + febSwitch(r.status === "ACTIVE", "feb-sw-review", r.id) + "</td>" +
      '<td class="pa-ctr">' + esc(r.sortOrder ?? 0) + "</td>" +
      "<td>" + esc(ctx.fmtTime(r.createdAt)) + "</td>" +
      '<td class="pa-nowrap">' +
      '<button class="pa-linkbtn" type="button" data-action="feb-edit" data-kind="review" data-id="' + esc(r.id) + '">编辑</button>' +
      '<button class="pa-linkbtn is-danger" type="button" data-action="feb-del" data-kind="review" data-id="' + esc(r.id) + '">删除</button>' +
      "</td></tr>";
  }

  async function loadReviews() {
    const st = febSt.reviews;
    const seq = ++st.seq;
    renderReviewsCard("loading", "加载中…");
    const query = new URLSearchParams({ page: String(st.page), size: String(st.size) });
    const kw = st.q.trim();
    if (kw) query.set("keyword", kw);
    if (st.status !== "all") query.set("status", st.status);
    try {
      const data = await ctx.api("/admin/reviews?" + query.toString());
      if (seq !== st.seq) return;
      st.rows = Array.isArray(data?.records) ? data.records : [];
      st.total = Number(data?.total || 0);
      st.totalPages = Math.max(1, Math.ceil(st.total / st.size));
      renderReviewsCard(st.rows.length ? "ok" : "empty", kw || st.status !== "all" ? "未找到匹配的评价" : "暂无评价，点击「新增评价」创建");
    } catch (error) {
      if (seq !== st.seq) return;
      febLoadFailed("reviews", error, renderReviewsCard, "请稍后重试。");
    }
  }

  /** 拉取商品下拉数据源后打开新增/编辑弹窗（列表行已含全部字段，编辑直接回填） */
  async function febOpenReviewEdit(row) {
    let products = [];
    try {
      const data = await ctx.api("/admin/products?page=1&size=" + FEB_OPTION_SIZE);
      products = Array.isArray(data?.records) ? data.records : [];
    } catch (error) {
      if (ctx.isAuthError(error)) febAuthFail();
      else ctx.toast(ctx.errText(error, "获取商品列表失败，无法选择关联商品。"), "err");
      return;
    }
    febEdit = { kind: "review", id: row ? row.id : null, products };
    const productOptions =
      '<option value="">通用（不关联商品）</option>' +
      products.map((p) =>
        '<option value="' + esc(p.id) + '"' + (row && String(row.productId) === String(p.id) ? " selected" : "") + ">" +
        esc(p.name) + (p.status === "INACTIVE" ? "（已下架）" : "") + "</option>").join("");
    ctx.showModal(
      "",
      row ? "编辑评价" : "新增评价",
      '<div class="pa-form">' +
      febFi("评价人", "vbuyer", row ? row.buyerName : "", "最长 64 字符", true, "text", 'maxlength="64"') +
      febFi("宠物名", "vpet", row ? row.petName : "", "", false, "text", "maxlength=\"64\"") +
      febFi("宠物品种", "vbreed", row ? row.petBreed : "", "", false, "text", "maxlength=\"64\"") +
      febFi("头像 URL", "vavatar", row ? row.avatarUrl : "", "外链地址，最长 500 字符", false, "text", 'maxlength="500" autocomplete="off"') +
      '<div class="pa-fi"><label>关联商品</label><select id="f-vpid">' + productOptions + "</select>" +
      '<span class="pa-hint">仅展示前 ' + FEB_OPTION_SIZE + " 个商品；已下架商品带标记</span></div>" +
      febFi("评分", "vrating", row ? row.rating : "4.8", "0.0 – 5.0，一位小数", true, "number", 'step="0.1" min="0" max="5" inputmode="decimal"') +
      febFi("排序号", "vsort", row ? row.sortOrder : 0, "越小越靠前，0 – 999999", false, "number", 'step="1" min="0" max="999999" inputmode="numeric"') +
      '<div class="pa-fi"><label>展示状态</label><select id="f-vstatus">' +
      '<option value="ACTIVE"' + (!row || row.status === "ACTIVE" ? " selected" : "") + ">展示</option>" +
      '<option value="INACTIVE"' + (row && row.status === "INACTIVE" ? " selected" : "") + ">隐藏</option>" +
      "</select></div>" +
      '<div class="pa-fi full"><label><span class="req">*</span>评价内容</label>' +
      '<textarea id="f-vcontent" maxlength="1000" rows="5">' + esc(row ? row.content : "") + "</textarea>" +
      '<span class="pa-hint">最长 1000 字符</span></div>' +
      "</div>",
      '<button class="pa-btn" type="button" data-action="modal-close">取消</button>' +
      '<button class="pa-btn pa-btn-pri" type="button" data-action="feb-submit" data-kind="review">' +
      (row ? "保存修改" : "创建评价") + "</button>"
    );
  }

  async function febSubmitReview(btn) {
    const buyerName = febVal("vbuyer");
    const petName = febVal("vpet");
    const petBreed = febVal("vbreed");
    const avatarUrl = febVal("vavatar");
    const content = febVal("vcontent");
    const ratingRaw = febVal("vrating") || "4.8";
    const sortRaw = febVal("vsort") || "0";
    const productId = febVal("vpid");
    const status = febVal("vstatus");

    if (!buyerName) return ctx.toast("请填写评价人姓名", "err");
    if (buyerName.length > 64) return ctx.toast("评价人姓名长度不能超过 64 字符", "err");
    if (petName.length > 64) return ctx.toast("宠物名长度不能超过 64 字符", "err");
    if (petBreed.length > 64) return ctx.toast("宠物品种长度不能超过 64 字符", "err");
    if (!content) return ctx.toast("请填写评价内容", "err");
    if (content.length > 1000) return ctx.toast("评价内容长度不能超过 1000 字符", "err");
    if (avatarUrl.length > 500) return ctx.toast("头像 URL 长度不能超过 500 字符", "err");
    if (!/^\d(\.\d)?$/.test(ratingRaw) || Number(ratingRaw) > 5) {
      return ctx.toast("评分须为 0.0 – 5.0 的一位小数", "err");
    }
    if (!/^\d+$/.test(sortRaw) || Number(sortRaw) > 999999) return ctx.toast("排序号须为 0 – 999999 的整数", "err");

    const body = {
      buyerName,
      petName: petName || null,
      petBreed: petBreed || null,
      content,
      avatarUrl: avatarUrl || null,
      rating: Number(ratingRaw),
      sortOrder: Number(sortRaw),
      productId: productId ? Number(productId) : null,
      status,
    };

    btn.disabled = true;
    btn.textContent = "保存中…";
    try {
      if (febEdit && febEdit.id) {
        await ctx.api("/admin/reviews/" + febEdit.id, { method: "PUT", body });
        ctx.toast("评价已保存");
      } else {
        await ctx.api("/admin/reviews", { method: "POST", body });
        ctx.toast("评价已创建");
      }
      ctx.closeModal();
      loadReviews(); // 保持当前页刷新
    } catch (error) {
      if (ctx.isAuthError(error)) febAuthFail();
      else ctx.toast(ctx.errText(error, "保存失败，请稍后重试。"), "err");
      btn.disabled = false;
      btn.textContent = febEdit && febEdit.id ? "保存修改" : "创建评价";
    }
  }

  function febDeleteReview(id) {
    ctx.confirmBox(
      "删除这条评价？",
      "逻辑删除，删除后 C 端立即不再展示。",
      "确认删除",
      async () => {
        try {
          await ctx.api("/admin/reviews/" + id, { method: "DELETE" });
          ctx.toast("评价已删除");
          febStepBackPage("reviews");
          loadReviews();
        } catch (error) {
          if (ctx.isAuthError(error)) febAuthFail();
          else ctx.toast(ctx.errText(error, "删除失败，请稍后重试。"), "err");
        }
      }
    );
  }

  /* ============================================================
   * 模块：优惠券模板管理（接口 /admin/coupon-templates，全量不分页）
   * ============================================================ */

  function renderCouponsCard(kind, message) {
    if (!febEl || !febActive("coupons")) return;
    febEl.innerHTML =
      '<div class="pa-card"><div class="pa-card-hd"><h2>优惠券模板</h2><p>注册 / 邮箱订阅自动发放；停用后不再自动发放，存量持券展示与核销不受影响</p></div>' +
      '<div class="pa-toolbar">' +
      '<button class="pa-btn pa-btn-pri" type="button" data-action="feb-new" data-kind="coupon">' + ctx.icon("plus") + "新建模板</button>" +
      '<span class="pa-dim pa-toolbar-tip">固定金额券；已发放 &gt; 0 的模板后端拒绝删除，仅支持停用</span>' +
      "</div>" +
      '<div class="pa-tblwrap"><table class="pa-tbl"><thead><tr><th>券名称</th><th>面额</th><th>使用门槛</th>' +
      '<th class="pa-ctr">有效天数</th><th class="pa-ctr">已发放</th><th class="pa-ctr">已使用</th><th class="pa-ctr">使用率</th>' +
      '<th>状态</th><th class="pa-nowrap">操作</th></tr></thead><tbody>' +
      (kind === "ok"
        ? febCouponRows.map((c) => febCouponRowHtml(c)).join("")
        : febStateRow(9, kind, message, "coupons")) +
      "</tbody></table></div></div>";
  }

  function febCouponRowHtml(c) {
    const issued = Number(c.issuedCount || 0);
    const used = Number(c.usedCount || 0);
    const rate = issued > 0 ? Math.round((used / issued) * 100) + "%" : "—";
    return "<tr>" +
      '<td><span class="pa-cell-main">' + esc(c.name) + "</span>" +
      (c.remark ? '<div class="pa-cell-sub" style="max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" title="' + esc(c.remark) + '">' + esc(c.remark) + "</div>" : "") +
      "</td>" +
      '<td><b>' + ctx.money(c.amount) + "</b></td>" +
      "<td>" + (Number(c.minSpend || 0) > 0 ? "满 " + ctx.money(c.minSpend) + " 可用" : "无门槛") + "</td>" +
      '<td class="pa-ctr">' + Number(c.validDays || 0) + " 天</td>" +
      '<td class="pa-ctr">' + issued + "</td>" +
      '<td class="pa-ctr">' + used + "</td>" +
      '<td class="pa-ctr">' + rate + "</td>" +
      "<td>" + febSwitch(c.status === "ACTIVE", "feb-sw-coupon", c.id) + "</td>" +
      '<td class="pa-nowrap">' +
      '<button class="pa-linkbtn" type="button" data-action="feb-edit" data-kind="coupon" data-id="' + esc(c.id) + '">编辑</button>' +
      '<button class="pa-linkbtn is-danger" type="button" data-action="feb-del" data-kind="coupon" data-id="' + esc(c.id) + '">删除</button>' +
      "</td></tr>";
  }

  async function loadCoupons() {
    const seq = ++febCouponSeq;
    renderCouponsCard("loading", "加载中…");
    try {
      const data = await ctx.api("/admin/coupon-templates");
      if (seq !== febCouponSeq) return;
      febCouponRows = Array.isArray(data) ? data : [];
      renderCouponsCard(febCouponRows.length ? "ok" : "empty", "暂无券模板，点击「新建模板」创建");
    } catch (error) {
      if (seq !== febCouponSeq) return;
      febLoadFailed("coupons", error, renderCouponsCard, "请稍后重试。");
    }
  }

  /** 券模板弹窗：编辑时顶部必须展示黄色警示条（user_coupon 无金额快照，改动同步作用存量持券） */
  function febOpenCouponModal(c) {
    febEdit = { kind: "coupon", id: c ? c.id : null };
    const banner = c
      ? '<div class="pa-warnbanner">' + ctx.icon("warn") +
        "<span><b>修改模板将同步作用于已发放到用户手中的存量优惠券（系统无金额快照）。</b>持券展示与核销实时读取模板，请谨慎调整面额与门槛。</span></div>"
      : "";
    ctx.showModal(
      "",
      c ? "编辑优惠券模板" : "新建优惠券模板",
      banner +
      '<div class="pa-form">' +
      febFi("券名称", "cname", c ? c.name : "", "全局唯一，最长 64 字符", true, "text", "maxlength=\"64\"") +
      febFi("面额（元）", "camount", c ? c.amount : "", "固定金额券，须为大于 0 的整数", true, "number", 'step="1" min="1" inputmode="numeric"') +
      febFi("使用门槛（元）", "cmin", c ? c.minSpend : "", "满 X 可用，0 为无门槛", true, "number", 'step="1" min="0" inputmode="numeric"') +
      febFi("领取后有效天数", "cdays", c ? c.validDays : 30, "发放之日起算，须为大于 0 的整数", true, "number", 'step="1" min="1" inputmode="numeric"') +
      '<div class="pa-fi full"><label>备注</label><textarea id="f-cremark" maxlength="255" rows="2">' +
      esc(c ? c.remark || "" : "") + '</textarea><span class="pa-hint">发放场景说明，如「注册自动发放」</span></div>' +
      "</div>",
      '<button class="pa-btn" type="button" data-action="modal-close">取消</button>' +
      '<button class="pa-btn pa-btn-pri" type="button" data-action="feb-submit" data-kind="coupon">' +
      (c ? "保存修改" : "创建模板") + "</button>"
    );
  }

  async function febSubmitCoupon(btn) {
    const name = febVal("cname");
    const amount = febVal("camount");
    const minSpend = febVal("cmin");
    const validDays = febVal("cdays");
    const remark = febVal("cremark");

    if (!name) return ctx.toast("请填写券名称", "err");
    if (name.length > 64) return ctx.toast("券名称长度不能超过 64 字符", "err");
    if (!/^\d+$/.test(amount) || Number(amount) < 1) return ctx.toast("面额须为大于 0 的整数（元）", "err");
    if (!/^\d+$/.test(minSpend)) return ctx.toast("使用门槛须为不小于 0 的整数（元）", "err");
    if (!/^\d+$/.test(validDays) || Number(validDays) < 1) return ctx.toast("有效天数须为大于 0 的整数", "err");
    if (remark.length > 255) return ctx.toast("备注长度不能超过 255 字符", "err");

    const body = { name, amount: Number(amount), minSpend: Number(minSpend), validDays: Number(validDays), remark: remark || null };
    const isEdit = !!(febEdit && febEdit.id);
    btn.disabled = true;
    btn.textContent = "保存中…";
    try {
      if (isEdit) {
        await ctx.api("/admin/coupon-templates/" + febEdit.id, { method: "PUT", body });
        ctx.toast("模板已保存（已同步生效至存量持券）");
      } else {
        await ctx.api("/admin/coupon-templates", { method: "POST", body });
        ctx.toast("模板已创建，将按发放规则自动发放");
      }
      ctx.closeModal();
      loadCoupons();
    } catch (error) {
      if (ctx.isAuthError(error)) febAuthFail();
      else ctx.toast(ctx.errText(error, "保存失败，请稍后重试。"), "err"); // 如名称重复 409 直接展示后端文案
      btn.disabled = false;
      btn.textContent = isEdit ? "保存修改" : "创建模板";
    }
  }

  /** 停用方向需确认（文案固定）；启用直接执行 */
  function febPutCouponStatus(id, next, switchBtn) {
    const row = febCouponRows.find((r) => String(r.id) === String(id));
    if (switchBtn) {
      switchBtn.setAttribute("aria-checked", String(next === "ACTIVE")); // 乐观更新
      switchBtn.disabled = true;
    }
    ctx.api("/admin/coupon-templates/" + id + "/status", { method: "PUT", body: { status: next } })
      .then(() => {
        if (row) row.status = next;
        ctx.toast(next === "ACTIVE" ? "模板已启用，将恢复自动发放" : "模板已停用，不再自动发放");
        renderCouponsCard("ok");
      })
      .catch((error) => {
        if (switchBtn) switchBtn.setAttribute("aria-checked", String(next !== "ACTIVE")); // 回滚
        if (ctx.isAuthError(error)) febAuthFail();
        else ctx.toast(ctx.errText(error, "状态更新失败，请稍后重试。"), "err");
      })
      .finally(() => {
        if (switchBtn) switchBtn.disabled = false;
      });
  }

  function febDeleteCoupon(id) {
    const row = febCouponRows.find((r) => String(r.id) === String(id));
    const name = row ? row.name : "#" + id;
    ctx.confirmBox(
      "删除模板「" + name + "」？",
      "已发放数 &gt; 0 时后端将拒绝删除（仅支持停用），失败原因将直接提示。",
      "确认删除",
      async () => {
        try {
          await ctx.api("/admin/coupon-templates/" + id, { method: "DELETE" });
          ctx.toast("模板已删除");
          loadCoupons();
        } catch (error) {
          if (ctx.isAuthError(error)) febAuthFail();
          else ctx.toast(ctx.errText(error, "删除失败，请稍后重试。"), "err"); // 已发放时展示后端 400 文案
        }
      }
    );
  }

  /* ============================================================
   * 模块：日志管理（复刻旧 admin.html 能力；注意 {total,list} 与 status 0/1）
   * ============================================================ */

  function renderLogsCard(kind, message) {
    if (!febEl || !febActive("logs")) return;
    const st = febSt.logs;
    febEl.innerHTML =
      '<div class="pa-card"><div class="pa-card-hd"><h2>更新日志 / 通知</h2><p>C 端「AI 自习室」更新日志；发布状态 Switch 即时上下线，接口已迁移至 /api/admin/logs</p></div>' +
      '<div class="pa-toolbar">' +
      '<button class="pa-btn pa-btn-pri" type="button" data-action="feb-new" data-kind="log">' + ctx.icon("plus") + "新增日志</button>" +
      '<span class="pa-sp"></span>' + febKw("logs", "标题关键字", st.q) +
      "</div>" +
      '<div class="pa-tblwrap"><table class="pa-tbl"><thead><tr><th class="pa-ctr">序号</th><th>标题</th><th>类型</th>' +
      '<th>时间</th><th>发布状态</th><th class="pa-ctr">点击量</th><th class="pa-nowrap">操作</th></tr></thead><tbody>' +
      (kind === "ok"
        ? st.rows.map((l, i) => febLogRowHtml(l, (st.page - 1) * st.size + i + 1)).join("") // 序号跨页连续
        : febStateRow(7, kind, message, "logs")) +
      "</tbody></table></div>" +
      (kind === "ok" ? febPager("logs", st.page, st.totalPages, st.total) : "") +
      "</div>";
  }

  function febLogRowHtml(l, index) {
    const published = Number(l.publishStatus) === 1;
    return "<tr>" +
      '<td class="pa-ctr">' + index + "</td>" +
      '<td style="max-width:280px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" title="' + esc(l.title) + '">' +
      esc(l.title) + "</td>" +
      "<td>" + esc(l.type || "—") + "</td>" +
      "<td>" + esc(ctx.fmtTime(l.createTime)) + "</td>" +
      "<td>" + febSwitch(published, "feb-sw-log", l.id) + "</td>" +
      '<td class="pa-ctr">' + Number(l.viewCount || 0) + "</td>" +
      '<td class="pa-nowrap">' +
      '<button class="pa-linkbtn" type="button" data-action="feb-edit" data-kind="log" data-id="' + esc(l.id) + '">编辑</button>' +
      '<button class="pa-linkbtn is-danger" type="button" data-action="feb-del" data-kind="log" data-id="' + esc(l.id) + '">删除</button>' +
      "</td></tr>";
  }

  async function loadLogs() {
    const st = febSt.logs;
    const seq = ++st.seq;
    renderLogsCard("loading", "加载中…");
    const query = new URLSearchParams({ page: String(st.page), pageSize: String(st.size) }); // 注意参数名是 pageSize
    const kw = st.q.trim();
    if (kw) query.set("keyword", kw);
    try {
      const data = await ctx.api("/admin/logs?" + query.toString());
      if (seq !== st.seq) return;
      // 日志接口返回 {total,list}（UpdateLogPageVO），与其余模块的 PageResult{records,total} 不同
      st.rows = Array.isArray(data?.list) ? data.list : [];
      st.total = Number(data?.total || 0);
      st.totalPages = Math.max(1, Math.ceil(st.total / st.size));
      renderLogsCard(st.rows.length ? "ok" : "empty", kw ? "未找到匹配的日志" : "暂无日志，点击「新增日志」创建");
    } catch (error) {
      if (seq !== st.seq) return;
      febLoadFailed("logs", error, renderLogsCard, "请稍后重试。");
    }
  }

  function febOpenLogModal(l) {
    febEdit = { kind: "log", id: l ? l.id : null };
    ctx.showModal(
      "",
      l ? "编辑日志" : "新增日志",
      '<div class="pa-form">' +
      febFi("标题", "ltitle", l ? l.title : "", "最长 128 字符", true, "text", "maxlength=\"128\"") +
      '<div class="pa-fi"><label><span class="req">*</span>类型</label>' +
      '<input id="f-ltype" list="febLogTypes" maxlength="64" value="' + esc(l ? l.type || FEB_LOG_TYPES[0] : FEB_LOG_TYPES[0]) + '" autocomplete="off" />' +
      '<datalist id="febLogTypes">' + FEB_LOG_TYPES.map((t) => '<option value="' + esc(t) + '"></option>').join("") + "</datalist>" +
      '<span class="pa-hint">可从常用类型中选择或自定义</span></div>' +
      '<div class="pa-fi full"><label><span class="req">*</span>内容</label>' +
      '<textarea id="f-lcontent" rows="5">' + esc(l ? l.content : "") + "</textarea></div>" +
      "</div>",
      '<button class="pa-btn" type="button" data-action="modal-close">取消</button>' +
      '<button class="pa-btn pa-btn-pri" type="button" data-action="feb-submit" data-kind="log">保存</button>'
    );
  }

  /** 编辑需先拉详情（列表行不含 content） */
  async function febOpenLogEdit(id) {
    try {
      const detail = await ctx.api("/admin/logs/" + id);
      febOpenLogModal(detail);
    } catch (error) {
      if (ctx.isAuthError(error)) febAuthFail();
      else ctx.toast(ctx.errText(error, "获取日志详情失败。"), "err");
    }
  }

  async function febSubmitLog(btn) {
    const title = febVal("ltitle");
    const type = febVal("ltype") || FEB_LOG_TYPES[0];
    const content = febVal("lcontent");
    if (!title) return ctx.toast("请填写标题", "err");
    if (title.length > 128) return ctx.toast("标题长度不能超过 128 字符", "err");
    if (type.length > 64) return ctx.toast("类型长度不能超过 64 字符", "err");
    if (!content) return ctx.toast("请填写内容", "err");

    const isEdit = !!(febEdit && febEdit.id);
    btn.disabled = true;
    btn.textContent = "保存中…";
    try {
      if (isEdit) {
        await ctx.api("/admin/logs/" + febEdit.id, { method: "PUT", body: { title, type, content } });
        ctx.toast("日志已保存");
      } else {
        // 新增默认未发布（与旧日志页一致）
        await ctx.api("/admin/logs", { method: "POST", body: { title, type, content, status: 0 } });
        ctx.toast("日志已创建（默认未发布）");
        febSt.logs.page = 1; // 新日志按时间倒序排在最前
      }
      ctx.closeModal();
      loadLogs();
    } catch (error) {
      if (ctx.isAuthError(error)) febAuthFail();
      else ctx.toast(ctx.errText(error, "保存失败，请稍后重试。"), "err");
      btn.disabled = false;
      btn.textContent = "保存";
    }
  }

  function febDeleteLog(id) {
    ctx.confirmBox(
      "删除这条日志？",
      "物理删除，不可恢复。",
      "确认删除",
      async () => {
        try {
          await ctx.api("/admin/logs/" + id, { method: "DELETE" });
          ctx.toast("删除成功");
          febStepBackPage("logs");
          loadLogs();
        } catch (error) {
          if (ctx.isAuthError(error)) febAuthFail();
          else ctx.toast(ctx.errText(error, "删除失败，请稍后重试。"), "err");
        }
      }
    );
  }

  /* ============================================================
   * data-action 处理器注册（feb- 前缀 + 看板联动 goto-order）
   * ============================================================ */

  function febGoPage(scope, target) {
    const st = febSt[scope];
    if (!st) return;
    const clamped = Math.min(Math.max(target, 1), st.totalPages);
    if (clamped === st.page) return;
    st.page = clamped;
    (febLoaders[scope] || (() => {}))();
  }

  function febDoJump(scope) {
    const input = document.getElementById("febjump-" + scope);
    if (!input) return;
    const raw = String(input.value).trim();
    if (!/^\d+$/.test(raw)) {
      input.value = febSt[scope] ? febSt[scope].page : 1;
      return;
    }
    const clamped = Math.min(Math.max(Number(raw), 1), febSt[scope] ? febSt[scope].totalPages : 1);
    input.value = clamped;
    febGoPage(scope, clamped);
  }

  /** 搜索前从输入框兜底同步一次关键字（input 事件已实时同步，双保险） */
  function febSyncKw(scope) {
    const st = febSt[scope];
    if (!st || !febEl) return;
    const input = febEl.querySelector('.pa-kw-inp[data-scope="' + scope + '"]');
    if (input) st.q = input.value;
  }

  function febRegisterActions() {
    ctx.onAction("feb-search", (t) => {
      const scope = t.dataset.scope;
      if (!febSt[scope]) return;
      febSyncKw(scope);
      febSt[scope].page = 1; // 搜索重置到第 1 页
      (febLoaders[scope] || (() => {}))();
    });

    ctx.onAction("feb-page", (t) => {
      const scope = t.dataset.scope;
      const raw = t.dataset.p;
      if (!febSt[scope]) return;
      if (raw === "prev") febGoPage(scope, febSt[scope].page - 1);
      else if (raw === "next") febGoPage(scope, febSt[scope].page + 1);
      else febGoPage(scope, Number(raw));
    });

    ctx.onAction("feb-jump", (t) => febDoJump(t.dataset.scope));
    ctx.onAction("feb-retry", (t) => {
      const fn = febLoaders[t.dataset.scope];
      if (fn) fn();
    });

    // ---------- 订单 ----------
    ctx.onAction("order-detail", (t) => febOpenOrderDetail(t.dataset.id));
    ctx.onAction("order-cancel", (t) => febCancelOrder(t.dataset.id));

    // ---------- 用户 ----------
    ctx.onAction("user-detail", (t) => febOpenUserDetail(t.dataset.id));
    ctx.onAction("user-disable", (t) => febRequestUserStatus(t.dataset.id, "INACTIVE", null));
    ctx.onAction("user-enable", (t) => febRequestUserStatus(t.dataset.id, "ACTIVE", null));
    ctx.onAction("feb-sw-user", (t) => {
      const next = t.getAttribute("aria-checked") === "true" ? "INACTIVE" : "ACTIVE";
      febRequestUserStatus(t.dataset.id, next, t);
    });
    ctx.onAction("drawer-close", febCloseDrawer);

    // ---------- 评价 ----------
    ctx.onAction("feb-sw-review", (t) => {
      const id = t.dataset.id;
      const row = febSt.reviews.rows.find((r) => String(r.id) === String(id));
      febToggleSwitch(
        t,
        (next) => ctx.api("/admin/reviews/" + id + "/status", { method: "PUT", body: { status: next ? "ACTIVE" : "INACTIVE" } }),
        (next) => {
          if (row) row.status = next ? "ACTIVE" : "INACTIVE";
        },
        (next) => (next ? "评价已展示" : "评价已隐藏，C 端将不再展示")
      );
    });

    // ---------- 优惠券 ----------
    ctx.onAction("feb-sw-coupon", (t) => {
      const id = t.dataset.id;
      const next = t.getAttribute("aria-checked") === "true" ? "INACTIVE" : "ACTIVE";
      if (next === "INACTIVE") {
        const row = febCouponRows.find((r) => String(r.id) === String(id));
        const name = row ? row.name : "#" + id;
        ctx.confirmBox(
          "停用券模板「" + name + "」？",
          "停用后新用户注册/邮箱订阅将不再自动发放该券，存量持券不受影响。",
          "确认停用",
          () => febPutCouponStatus(id, "INACTIVE", t)
        );
      } else {
        febPutCouponStatus(id, "ACTIVE", t);
      }
    });

    // ---------- 日志（注意：发布状态是数字 0/1，与其它模块的字符串状态不同） ----------
    ctx.onAction("feb-sw-log", (t) => {
      const id = t.dataset.id;
      const row = febSt.logs.rows.find((r) => String(r.id) === String(id));
      febToggleSwitch(
        t,
        (next) => ctx.api("/admin/logs/" + id + "/status", { method: "PUT", body: { status: next ? 1 : 0 } }),
        (next) => {
          if (row) row.publishStatus = next ? 1 : 0;
        },
        (next) => (next ? "已发布" : "已下线")
      );
    });

    // ---------- 弹窗：新增 / 编辑 / 删除 / 提交 ----------
    ctx.onAction("feb-new", (t) => {
      const kind = t.dataset.kind;
      if (kind === "review") febOpenReviewEdit(null);
      else if (kind === "coupon") febOpenCouponModal(null);
      else if (kind === "log") febOpenLogModal(null);
    });

    ctx.onAction("feb-edit", (t) => {
      const kind = t.dataset.kind;
      const id = t.dataset.id;
      if (kind === "review") {
        const row = febSt.reviews.rows.find((r) => String(r.id) === String(id));
        if (row) febOpenReviewEdit(row); // 列表行与详情 VO 同构，直接回填
      } else if (kind === "coupon") {
        const row = febCouponRows.find((r) => String(r.id) === String(id));
        if (row) febOpenCouponModal(row);
      } else if (kind === "log") {
        febOpenLogEdit(id);
      }
    });

    ctx.onAction("feb-del", (t) => {
      const kind = t.dataset.kind;
      const id = t.dataset.id;
      if (kind === "review") febDeleteReview(id);
      else if (kind === "coupon") febDeleteCoupon(id);
      else if (kind === "log") febDeleteLog(id);
    });

    ctx.onAction("feb-submit", (t) => {
      const kind = t.dataset.kind;
      if (kind === "review") febSubmitReview(t);
      else if (kind === "coupon") febSubmitCoupon(t);
      else if (kind === "log") febSubmitLog(t);
    });

    // ---------- 看板联动：最近订单行点击 → 切订单模块并按订单号定位 ----------
    // FE-A 在此处留的是「订单模块建设中」占位 toast，本注册按其预留的
    // 模块互调方式接通（同「去补库存」→ products.openEdit 的做法）。
    ctx.onAction("goto-order", (t) => {
      const no = String(t.dataset.no || "");
      ctx.switchTo("orders");
      const mod = PA.modules.get("orders");
      if (mod && typeof mod.locate === "function") mod.locate(no);
      if (no) ctx.toast("已按订单号定位：" + no, "warn");
    });
  }

  /* ---------------- 根容器级监听（一次性挂载，随模块切换不重复） ---------------- */

  function febBindRoot() {
    const root = document.getElementById("pa-console");
    if (!root) return;

    // 筛选下拉 / 日期范围（data-filter 路由到对应模块状态）
    root.addEventListener("change", (event) => {
      const t = event.target;
      if (!(t instanceof Element) || !t.dataset || !t.dataset.filter) return;
      const filter = t.dataset.filter;
      const route = (scope, patch) => {
        const st = febSt[scope];
        if (!st) return;
        Object.assign(st, patch);
        st.page = 1; // 筛选变化重置到第 1 页
        (febLoaders[scope] || (() => {}))();
      };
      if (filter === "o-status") route("orders", { status: t.value });
      else if (filter === "o-from") route("orders", { from: t.value });
      else if (filter === "o-to") route("orders", { to: t.value });
      else if (filter === "u-status") route("users", { status: t.value });
      else if (filter === "r-status") route("reviews", { status: t.value });
    });

    // 关键字实时同步（重渲染分页时输入框按状态回填）
    root.addEventListener("input", (event) => {
      const t = event.target;
      if (!(t instanceof Element)) return;
      if (t.classList.contains("pa-kw-inp") && t.dataset.scope && febSt[t.dataset.scope]) {
        febSt[t.dataset.scope].q = t.value;
      }
    });

    // Esc / 回车。用捕获 phase：Esc 时先于 FE-A 的气泡监听读取弹窗开闭状态，
    // 保证「弹窗与抽屉同开」时 Esc 只关最上层的弹窗、抽屉不被误关。
    root.addEventListener("keydown", (event) => {
      if (event.key === "Escape") {
        if (febDrawer && !febDrawer.hidden) {
          const ov = document.getElementById("paOverlay");
          if (!ov || ov.hidden) febCloseDrawer(); // 弹窗优先（FE-A 已在气泡 phase 关闭弹窗）
        }
        return;
      }
      if (event.key !== "Enter") return;
      const t = event.target;
      if (!(t instanceof Element)) return;
      if (t.classList.contains("pa-kw-inp") && t.dataset.scope && febSt[t.dataset.scope]) {
        event.preventDefault();
        febSyncKw(t.dataset.scope);
        febSt[t.dataset.scope].page = 1;
        (febLoaders[t.dataset.scope] || (() => {}))();
      } else if (t.id && t.id.indexOf("febjump-") === 0) {
        event.preventDefault();
        febDoJump(t.id.slice(8));
      }
    }, true);

    // 切换导航 / 退出登录时收起抽屉（switchTo 只清内容区，抽屉挂在其外）；
    // 同时使本文件在途列表请求失效——含切到 FE-A 模块（看板/商品/套餐）的
    // 场景。用捕获 phase：须先于 FE-A 的气泡 phase nav 处理器执行，否则
    // 新模块 mount 后刚发起的请求会被这次失效误伤（seq 对不上被丢弃）。
    root.addEventListener("click", (event) => {
      const hit = event.target.closest('[data-action="nav"],[data-action="logout"]');
      if (!hit) return;
      if (febDrawer && !febDrawer.hidden) febCloseDrawer();
      febCurrentView = hit.dataset.view || null; // FE-A 模块切换也记录，用于丢弃迟到渲染
      febInvalidateRequests(); // nav 切换与退出登录均丢弃在途响应
    }, true);
  }

  /* ============================================================
   * 模块注册（同 id 覆盖 FE-A 占位定义，导航自动重渲染）
   * ============================================================ */

  febInjectStyles();
  febRegisterActions();
  febBindRoot();

  PA.registerModule({
    id: "orders",
    title: "订单管理",
    order: 40,
    icon: "cart",
    mount(container) {
      febPrepareMount(container, "orders");
      renderOrdersCard("loading", "加载中…");
      loadOrders();
    },
    /** 看板「最近订单」跳转定位入口：填入订单号并触发查询（跨模块互调） */
    locate(orderNo) {
      const st = febSt.orders;
      st.q = String(orderNo || "");
      st.status = "all";
      st.from = "";
      st.to = "";
      st.page = 1;
      loadOrders(); // mount 已发起的旧请求由 seq 序号丢弃
    },
  });

  PA.registerModule({
    id: "users",
    title: "用户管理",
    order: 50,
    icon: "users",
    mount(container) {
      febPrepareMount(container, "users");
      renderUsersCard("loading", "加载中…");
      loadUsers();
    },
  });

  PA.registerModule({
    id: "reviews",
    title: "评价管理",
    order: 60,
    icon: "star",
    mount(container) {
      febPrepareMount(container, "reviews");
      renderReviewsCard("loading", "加载中…");
      loadReviews();
    },
  });

  PA.registerModule({
    id: "coupons",
    title: "优惠券管理",
    order: 70,
    icon: "ticket",
    mount(container) {
      febPrepareMount(container, "coupons");
      renderCouponsCard("loading", "加载中…");
      loadCoupons();
    },
  });

  PA.registerModule({
    id: "logs",
    title: "日志管理",
    order: 80,
    icon: "file-text",
    mount(container) {
      febPrepareMount(container, "logs");
      renderLogsCard("loading", "加载中…");
      loadLogs();
    },
  });
})();
