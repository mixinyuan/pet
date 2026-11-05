const API_BASE_URL = localStorage.getItem("pawbites-api-base") || "http://localhost:8080/api";
const AUTH_STORAGE_KEY = "pawbites-auth";
const GUEST_STORAGE_KEY = "pawbites-guest-id";

const categoryGrid = document.querySelector(".category-grid");
const productGrid = document.querySelector("#productGrid");
const bundleGrid = document.querySelector(".bundle-grid");
const reviewGrid = document.querySelector(".review-grid");
const loginScreen = document.querySelector(".login-screen");
const loginForm = document.querySelector("#loginForm");
const usernameInput = document.querySelector("#username");
const passwordInput = document.querySelector("#password");
const registerButton = document.querySelector("#registerSubmit");
const guestLoginButton = document.querySelector("#guestLogin");
const loginMessage = document.querySelector(".login-message");
const accountButton = document.querySelector(".account-button");
const accountName = document.querySelector(".account-name");
const cartButton = document.querySelector(".cart-button");
const cartDrawer = document.querySelector(".cart-drawer");
const drawerBackdrop = document.querySelector(".drawer-backdrop");
const closeCartButton = document.querySelector(".close-cart");
const cartItems = document.querySelector(".cart-items");
const cartEmpty = document.querySelector(".cart-empty");
const cartCount = document.querySelector(".cart-count");
const cartTotal = document.querySelector(".cart-total");
const clearCartButton = document.querySelector(".clear-cart");
const checkoutButton = document.querySelector(".checkout-button");
const toast = document.querySelector(".toast");
const menuToggle = document.querySelector(".menu-toggle");
const navLinks = document.querySelector(".nav-links");
const subscribeForm = document.querySelector(".subscribe-form");
const subscribeInput = document.querySelector("#email");
const subscribeMessage = document.querySelector(".subscribe-message");
const ordersButton = document.querySelector(".orders-button");
const ordersDrawer = document.querySelector(".orders-drawer");
const closeOrdersButton = document.querySelector(".close-orders");
const ordersList = document.querySelector(".orders-list");
const ordersEmpty = document.querySelector(".orders-empty");
const productModal = document.querySelector(".product-modal");
const productModalBackdrop = document.querySelector(".product-modal .modal-backdrop");
const productModalContent = document.querySelector(".product-modal-content");
const closeProductModalButton = document.querySelector(".close-product-modal");
const accountDrawer = document.querySelector(".account-drawer");
const closeAccountButton = document.querySelector(".close-account");
const accountPanel = document.querySelector(".account-panel");
const productsFooter = document.querySelector("#productsFooter");
const productsLoadMoreButton = productsFooter.querySelector("[data-load-more]");
const productsEndHint = productsFooter.querySelector(".products-end-hint");
const productsLoadError = productsFooter.querySelector(".products-load-error");

let currentUser = null;
let activeFilter = "all";
let cartState = { items: [], count: 0, totalAmount: 0 };
let toastTimer;
let productModalReturnFocus = null;
let productDetailRequestId = 0;
const PRODUCT_PAGE_SIZE = 20;
let productLoadState = { page: 0, total: 0, items: [], hasMore: false, loading: false };
let productLoadRequestId = 0;
let categoryNameByCode = {
  all: "全部",
  cat: "猫咪零食",
  dog: "狗狗零食",
  freeze: "冻干",
  dental: "洁齿零食",
  training: "训练奖励",
};

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
    // Local storage can be unavailable in strict privacy modes.
  }
}

function removeStoredValue(key) {
  try {
    localStorage.removeItem(key);
  } catch {
    // Ignore storage failures; the current page state still works.
  }
}

function getGuestId() {
  const existingGuestId = localStorage.getItem(GUEST_STORAGE_KEY);
  if (existingGuestId) return existingGuestId;

  const guestId = globalThis.crypto?.randomUUID
    ? globalThis.crypto.randomUUID()
    : `guest-${Date.now()}-${Math.random().toString(16).slice(2)}`;
  localStorage.setItem(GUEST_STORAGE_KEY, guestId);
  return guestId;
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function formatPrice(value) {
  return `¥${Number(value || 0).toFixed(0)}`;
}

function formatDateTime(value) {
  if (!value) return "—";
  const date = new Date(String(value).replace(" ", "T"));
  if (Number.isNaN(date.getTime())) return String(value);
  const pad = (part) => String(part).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function normalizeError(error) {
  if (error instanceof TypeError) {
    return "无法连接后端服务，请确认 Spring Boot 已启动。";
  }
  return error.message || "操作失败，请稍后重试。";
}

async function apiRequest(path, options = {}) {
  const headers = {
    "Content-Type": "application/json",
    ...(options.headers || {}),
  };

  if (currentUser?.token) {
    headers.Authorization = `Bearer ${currentUser.token}`;
  } else if (currentUser?.mode === "guest") {
    headers["X-Guest-Id"] = currentUser.guestId || getGuestId();
  }

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
    });
  } catch (error) {
    throw new Error(normalizeError(error));
  }

  const payload = await response.json().catch(() => null);
  if (!response.ok || payload?.code !== 0) {
    throw new Error(payload?.message || `请求失败：${response.status}`);
  }
  return payload.data;
}

function setLoginLoading(isLoading) {
  loginForm.querySelectorAll("input, button").forEach((element) => {
    element.disabled = isLoading;
  });
}

function showToast(message) {
  clearTimeout(toastTimer);
  toast.textContent = message;
  toast.classList.add("show");
  toastTimer = setTimeout(() => {
    toast.classList.remove("show");
  }, 1800);
}

function updateAccount() {
  if (!currentUser) return;
  accountName.textContent = currentUser.displayName;
  accountButton.setAttribute("aria-label", `打开${currentUser.displayName}的个人中心`);
}

function unlockPage(profile, options = {}) {
  currentUser = profile;
  document.body.classList.remove("auth-locked");
  loginScreen.setAttribute("aria-hidden", "true");
  loginForm.reset();
  loginMessage.textContent = "";
  writeStoredValue(AUTH_STORAGE_KEY, profile);
  updateAccount();

  if (options.focusHome) {
    document.querySelector(".site-header .brand").focus();
  }

  if (options.showWelcome) {
    showToast(`${profile.displayName}，欢迎回来`);
  }
}

function lockPage(message = "") {
  currentUser = null;
  removeStoredValue(AUTH_STORAGE_KEY);
  closeCart();
  closeOrders();
  closeAccount();
  closeProductModal();
  closeMenu();
  document.body.classList.add("auth-locked");
  loginScreen.setAttribute("aria-hidden", "false");
  loginMessage.textContent = message;
  usernameInput.focus();
  renderCart({ items: [], count: 0, totalAmount: 0 });
  resetFavoritesAndCoupons();
}

function toAuthProfile(authResponse) {
  const user = authResponse.user;
  return {
    mode: "user",
    token: authResponse.token,
    user,
    displayName: user.nickname || user.username,
  };
}

async function restoreAuth() {
  const savedProfile = readStoredValue(AUTH_STORAGE_KEY);
  if (!savedProfile) return;

  if (savedProfile.mode === "guest") {
    unlockPage({
      mode: "guest",
      guestId: savedProfile.guestId || getGuestId(),
      displayName: "游客模式",
    });
    loadStorefrontData();
    return;
  }

  if (!savedProfile.token) {
    removeStoredValue(AUTH_STORAGE_KEY);
    return;
  }

  currentUser = savedProfile;
  try {
    const user = await apiRequest("/auth/me");
    unlockPage({
      ...savedProfile,
      user,
      displayName: user.nickname || user.username,
    });
    loadStorefrontData();
  } catch {
    lockPage("登录状态已失效，请重新登录。");
  }
}

async function submitLogin(event) {
  event.preventDefault();
  if (!loginForm.checkValidity()) {
    loginForm.reportValidity();
    return;
  }

  const username = usernameInput.value.trim();
  const password = passwordInput.value;
  if (!username || !password) {
    loginMessage.textContent = "请输入有效账号和密码。";
    return;
  }

  setLoginLoading(true);
  loginMessage.textContent = "正在登录...";
  try {
    const authResponse = await apiRequest("/auth/login", {
      method: "POST",
      body: { username, password },
    });
    unlockPage(toAuthProfile(authResponse), { focusHome: true, showWelcome: true });
    await loadStorefrontData();
  } catch (error) {
    loginMessage.textContent = normalizeError(error);
  } finally {
    setLoginLoading(false);
  }
}

async function registerAccount() {
  if (!loginForm.checkValidity()) {
    loginForm.reportValidity();
    return;
  }

  const username = usernameInput.value.trim();
  const password = passwordInput.value;
  if (!username || !password) {
    loginMessage.textContent = "请输入有效账号和密码。";
    return;
  }

  setLoginLoading(true);
  loginMessage.textContent = "正在创建账号...";
  try {
    const authResponse = await apiRequest("/auth/register", {
      method: "POST",
      body: {
        username,
        password,
        email: username.includes("@") ? username : null,
        nickname: username,
      },
    });
    unlockPage(toAuthProfile(authResponse), { focusHome: true, showWelcome: true });
    await loadStorefrontData();
    showToast("注册成功，新人专享券已发放，满99元可用");
  } catch (error) {
    loginMessage.textContent = normalizeError(error);
  } finally {
    setLoginLoading(false);
  }
}

async function enterAsGuest() {
  unlockPage(
    {
      mode: "guest",
      guestId: getGuestId(),
      displayName: "游客模式",
    },
    {
      focusHome: true,
      showWelcome: true,
    }
  );

  try {
    await loadStorefrontData();
  } catch (error) {
    showToast(normalizeError(error));
  }
}

async function logout() {
  if (currentUser?.token) {
    apiRequest("/auth/logout", { method: "POST" }).catch(() => {});
  }
  lockPage("已退出登录，可以重新进入。");
}

function renderCategories(categories = []) {
  const safeCategories = categories.length
    ? categories
    : [{ code: "all", name: "全部", description: "今日精选" }];

  categoryGrid.innerHTML = safeCategories
    .map((category) => {
      const code = category.code || "all";
      const isActive = code === activeFilter;
      categoryNameByCode[code] = category.name;
      return `
        <button class="category-card ${isActive ? "active" : ""}" type="button" data-filter="${escapeHtml(code)}" role="listitem" aria-pressed="${isActive}">
          <span>${escapeHtml(category.name)}</span>
          <small>${escapeHtml(category.description)}</small>
        </button>
      `;
    })
    .join("");
}

async function loadCategories() {
  const categories = await apiRequest("/categories");
  renderCategories(categories);
}

function renderProductCard(product) {
  return `
    <article class="product-card">
      <button class="favorite-toggle ${favoriteIds.has(Number(product.id)) ? "active" : ""}" type="button" data-favorite="${escapeHtml(product.id)}" aria-pressed="${favoriteIds.has(Number(product.id))}" aria-label="${favoriteIds.has(Number(product.id)) ? "取消收藏" : "收藏"}${escapeHtml(product.name)}">${favoriteIds.has(Number(product.id)) ? "♥" : "♡"}</button>
      <button class="product-image product-media" type="button" data-product-id="${escapeHtml(product.id)}" aria-label="查看${escapeHtml(product.name)}详情">
        <img src="${escapeHtml(product.imageUrl)}" alt="${escapeHtml(product.name)}" loading="lazy">
        <div class="tag-row">
          ${(product.tags || []).map((tag) => `<span class="tag">${escapeHtml(tag)}</span>`).join("")}
        </div>
        <span class="detail-hint">查看详情</span>
      </button>
      <div class="product-info">
        <h3>${escapeHtml(product.name)}</h3>
        <div class="meta">${escapeHtml(product.petType)} · ${escapeHtml(product.flavor)}</div>
        <div class="rating">★ ${escapeHtml(product.rating)} / 5.0</div>
        <div class="price-row">
          <span class="price">${formatPrice(product.price)}</span>
          <button type="button" data-add="${escapeHtml(product.id)}" aria-label="将${escapeHtml(product.name)}加入购物车">加入购物车</button>
        </div>
      </div>
    </article>
  `;
}

function renderProducts(products = []) {
  if (!products.length) {
    productGrid.innerHTML = `<div class="empty-state">暂时没有符合条件的商品。</div>`;
    return;
  }

  productGrid.innerHTML = products.map((product) => renderProductCard(product)).join("");
}

function appendProductCards(products = []) {
  if (!products.length) return;
  productGrid.insertAdjacentHTML("beforeend", products.map((product) => renderProductCard(product)).join(""));
}

function setProductsFooter(state, message = "") {
  productsFooter.hidden = state === "hidden";
  productsLoadMoreButton.hidden = state !== "more" && state !== "loading";
  productsLoadMoreButton.disabled = state === "loading";
  productsLoadMoreButton.textContent = state === "loading" ? "加载中..." : "加载更多商品";
  productsEndHint.hidden = state !== "end";
  productsLoadError.hidden = state !== "error";
  if (state === "error") {
    productsLoadError.querySelector("p").textContent = message;
  }
}

async function loadProducts({ reset = true } = {}) {
  if (!reset && productLoadState.loading) return;

  const requestId = ++productLoadRequestId;
  const targetPage = reset ? 1 : productLoadState.page + 1;

  if (reset) {
    productLoadState = { page: 0, total: 0, items: [], hasMore: false, loading: false };
    productGrid.innerHTML = `<div class="empty-state">商品加载中...</div>`;
    setProductsFooter("hidden");
  }

  productLoadState.loading = true;
  if (!reset) {
    setProductsFooter("loading");
  }

  try {
    const params = new URLSearchParams({
      page: String(targetPage),
      size: String(PRODUCT_PAGE_SIZE),
      category: activeFilter,
    });
    const result = await apiRequest(`/products?${params.toString()}`);
    if (requestId !== productLoadRequestId) return;

    const records = Array.isArray(result?.records) ? result.records : [];
    const total = Number(result?.total || 0);
    if (reset) {
      productLoadState.items = records.slice();
      renderProducts(records);
    } else {
      productLoadState.items = productLoadState.items.concat(records);
      appendProductCards(records);
    }
    productLoadState.page = targetPage;
    productLoadState.total = total;
    productLoadState.hasMore = records.length > 0 && productLoadState.items.length < total;

    if (!productLoadState.items.length) {
      setProductsFooter("hidden");
    } else {
      setProductsFooter(productLoadState.hasMore ? "more" : "end");
    }
  } catch (error) {
    if (requestId !== productLoadRequestId) return;

    if (reset) {
      productLoadState.items = [];
      productGrid.innerHTML = `
        <div class="empty-state">
          <p>${escapeHtml(normalizeError(error))}</p>
          <button type="button" data-retry-products>重新加载</button>
        </div>
      `;
      setProductsFooter("hidden");
    } else {
      setProductsFooter("error", normalizeError(error));
    }
  } finally {
    if (requestId === productLoadRequestId) {
      productLoadState.loading = false;
    }
  }
}

function renderBundles(bundles = []) {
  if (!bundles.length) {
    bundleGrid.innerHTML = `<div class="empty-state">暂无套餐。</div>`;
    return;
  }

  bundleGrid.innerHTML = bundles
    .map(
      (bundle) => `
        <article class="bundle-card">
          <span class="discount">${escapeHtml(bundle.discountLabel)}</span>
          <h3>${escapeHtml(bundle.name)}</h3>
          <p>${escapeHtml(bundle.description)}</p>
          <strong>${formatPrice(bundle.price)}</strong>
        </article>
      `
    )
    .join("");
}

async function loadBundles() {
  const bundles = await apiRequest("/bundles");
  renderBundles(bundles);
}

function renderReviews(reviews = []) {
  if (!reviews.length) {
    reviewGrid.innerHTML = `<div class="empty-state">暂无评价。</div>`;
    return;
  }

  reviewGrid.innerHTML = reviews
    .map(
      (review) => `
        <article class="review-card">
          <img src="${escapeHtml(review.avatarUrl)}" alt="${escapeHtml(review.petName || review.buyerName)}头像" loading="lazy" />
          <p>“${escapeHtml(review.content)}”</p>
          <strong>${escapeHtml(review.petName || review.buyerName)} · ${escapeHtml(review.petBreed)}</strong>
          <span>购买：${escapeHtml(review.purchasedProduct)}</span>
        </article>
      `
    )
    .join("");
}

async function loadReviews() {
  const reviews = await apiRequest("/reviews");
  renderReviews(reviews);
}

function renderCart(cart) {
  cartState = cart || { items: [], count: 0, totalAmount: 0 };
  const items = cartState.items || [];

  cartCount.textContent = cartState.count || 0;
  cartTotal.textContent = formatPrice(cartState.totalAmount);
  cartButton.setAttribute("aria-label", cartState.count > 0 ? `打开购物车，共${cartState.count}件商品` : "打开购物车");
  cartEmpty.hidden = items.length > 0;
  clearCartButton.disabled = items.length === 0;
  checkoutButton.disabled = items.length === 0;

  cartItems.innerHTML = items
    .map(
      (item) => `
        <article class="cart-item">
          <img src="${escapeHtml(item.productImage)}" alt="${escapeHtml(item.productName)}">
          <div>
            <h3>${escapeHtml(item.productName)}</h3>
            <div class="cart-line">
              <span>${formatPrice(item.price)}</span>
              <div class="stepper" aria-label="${escapeHtml(item.productName)}数量">
                <button type="button" data-decrease="${item.productId}" aria-label="减少${escapeHtml(item.productName)}数量">−</button>
                <input
                  type="number"
                  min="1"
                  step="1"
                  inputmode="numeric"
                  value="${escapeHtml(item.quantity)}"
                  data-quantity="${escapeHtml(item.productId)}"
                  aria-label="${escapeHtml(item.productName)}数量"
                />
                <button type="button" data-increase="${item.productId}" aria-label="增加${escapeHtml(item.productName)}数量">+</button>
              </div>
            </div>
          </div>
        </article>
      `
    )
    .join("");

  renderCouponSummary();
}

async function loadCart() {
  const cart = await apiRequest("/cart");
  renderCart(cart);
}

function findCartItem(productId) {
  return (cartState.items || []).find((item) => String(item.productId) === String(productId));
}

async function addToCart(productId) {
  try {
    const cart = await apiRequest("/cart/items", {
      method: "POST",
      body: {
        productId: Number(productId),
        quantity: 1,
      },
    });
    renderCart(cart);
    const item = findCartItem(productId);
    showToast(`${item?.productName || "商品"}已加入购物车`);
  } catch (error) {
    showToast(normalizeError(error));
  }
}

async function changeCartQuantity(productId, nextQuantity) {
  const normalizedQuantity = Number(nextQuantity);
  if (!Number.isInteger(normalizedQuantity)) {
    renderCart(cartState);
    showToast("请输入有效数量");
    return;
  }

  try {
    const cart =
      normalizedQuantity <= 0
        ? await apiRequest(`/cart/items/${productId}`, { method: "DELETE" })
        : await apiRequest(`/cart/items/${productId}`, {
            method: "PUT",
            body: { quantity: normalizedQuantity },
          });
    renderCart(cart);
  } catch (error) {
    renderCart(cartState);
    showToast(normalizeError(error));
  }
}

function submitQuantityInput(input) {
  const item = findCartItem(input.dataset.quantity);
  if (!item) return;

  const value = input.value.trim();
  if (!value) {
    input.value = item.quantity;
    showToast("请输入商品数量");
    return;
  }

  const nextQuantity = Number(value);
  if (!Number.isInteger(nextQuantity) || nextQuantity < 1) {
    input.value = item.quantity;
    showToast("数量必须是大于 0 的整数");
    return;
  }

  if (nextQuantity !== item.quantity) {
    changeCartQuantity(item.productId, nextQuantity);
  }
}

async function clearCart() {
  if (!cartState.items?.length) return;

  try {
    await apiRequest("/cart", { method: "DELETE" });
    renderCart({ items: [], count: 0, totalAmount: 0 });
    showToast("购物车已清空");
  } catch (error) {
    showToast(normalizeError(error));
  }
}

async function checkoutCart() {
  if (!cartState.items?.length) {
    openCart();
    showToast("购物车为空，先挑选商品吧");
    return;
  }

  try {
    // 已选可用券时携带 couponId（用户持券实例 id）；不使用券则不带 body，与后端约定一致
    const order = await apiRequest("/orders", {
      method: "POST",
      body: couponState.selectedCouponId ? { couponId: couponState.selectedCouponId } : undefined,
    });
    couponState.selectedCouponId = null;
    // 下单后回源刷新购物车（后端只清掉已结算条目）；刷新失败时退化为本地清空展示
    await loadCart().catch(() => renderCart({ items: [], count: 0, totalAmount: 0 }));
    closeCart();
    showToast(`订单${order.orderNo}已提交`);
    openOrders();
    // 下单成功后刷新券列表（核销后的券不再可用），购物车已在上方重置
    loadCoupons();
  } catch (error) {
    showToast(normalizeError(error));
  }
}

function openProductModal(productId, triggerElement) {
  productModalReturnFocus = triggerElement || document.activeElement;
  productModal.hidden = false;
  closeProductModalButton.focus();
  loadProductDetail(productId);
}

function closeProductModal() {
  productDetailRequestId++;
  productModal.hidden = true;
  productModalContent.innerHTML = "";
  if (productModalReturnFocus) {
    productModalReturnFocus.focus();
    productModalReturnFocus = null;
  }
}

async function loadProductDetail(productId) {
  const requestId = ++productDetailRequestId;
  productModalContent.innerHTML = `<div class="modal-state">商品详情加载中...</div>`;
  try {
    const product = await apiRequest(`/products/${productId}`);
    if (requestId !== productDetailRequestId) return;
    renderProductDetail(product);
  } catch (error) {
    if (requestId !== productDetailRequestId) return;
    productModalContent.innerHTML = `
      <div class="modal-state">
        <p>${escapeHtml(normalizeError(error))}</p>
        <button type="button" data-retry-product="${escapeHtml(productId)}">重新加载</button>
      </div>
    `;
  }
}

function renderProductDetail(product) {
  const stock = Number(product.stock || 0);
  const isFavorite = favoriteIds.has(Number(product.id));
  const categories = (product.categories || []).map((code) => escapeHtml(categoryNameByCode[code] || code)).join("、");
  productModalContent.innerHTML = `
    <div class="product-detail">
      <div class="product-detail-media">
        <img src="${escapeHtml(product.imageUrl)}" alt="${escapeHtml(product.name)}">
        ${(product.tags || []).length ? `<div class="tag-row">${(product.tags || []).map((tag) => `<span class="tag">${escapeHtml(tag)}</span>`).join("")}</div>` : ""}
        <button class="favorite-toggle detail-favorite ${isFavorite ? "active" : ""}" type="button" data-favorite="${escapeHtml(product.id)}" aria-pressed="${isFavorite}" aria-label="${isFavorite ? "取消收藏" : "收藏"}${escapeHtml(product.name)}">${isFavorite ? "♥" : "♡"}</button>
      </div>
      <div class="product-detail-info">
        <h3>${escapeHtml(product.name)}</h3>
        <div class="meta">${escapeHtml(product.petType)} · ${escapeHtml(product.flavor)}</div>
        <div class="rating">★ ${escapeHtml(product.rating)} / 5.0</div>
        <div class="product-detail-price">
          <span class="price">${formatPrice(product.price)}</span>
          <span class="stock-note ${stock > 0 ? "" : "out"}">${stock > 0 ? `库存 ${escapeHtml(stock)} 件` : "暂时缺货"}</span>
        </div>
        ${categories ? `<div class="product-detail-categories">分类：${categories}</div>` : ""}
        <p class="product-detail-desc">${escapeHtml(product.description)}</p>
        <div class="product-detail-sku">SKU：${escapeHtml(product.sku)}</div>
        <button class="detail-add-button" type="button" data-modal-add="${escapeHtml(product.id)}" ${stock > 0 ? "" : "disabled"} aria-label="将${escapeHtml(product.name)}加入购物车">
          ${stock > 0 ? "加入购物车" : "已售罄"}
        </button>
      </div>
    </div>
  `;
}

function orderStatusText(status) {
  const statusMap = {
    CREATED: "已创建",
    CANCELLED: "已取消",
  };
  return statusMap[status] || status || "未知";
}

function openOrders() {
  closeCart();
  closeAccount();
  ordersDrawer.classList.add("open");
  ordersDrawer.setAttribute("aria-hidden", "false");
  drawerBackdrop.hidden = false;
  closeOrdersButton.focus();
  loadOrders();
}

function closeOrders() {
  ordersDrawer.classList.remove("open");
  ordersDrawer.setAttribute("aria-hidden", "true");
  drawerBackdrop.hidden = true;
}

async function loadOrders() {
  ordersEmpty.hidden = true;
  ordersList.innerHTML = `<div class="empty-state">订单加载中...</div>`;
  const params = new URLSearchParams({
    page: "1",
    size: "20",
  });
  try {
    const result = await apiRequest(`/orders?${params.toString()}`);
    renderOrders(result.records || []);
  } catch (error) {
    ordersList.innerHTML = `
      <div class="empty-state">
        <p>${escapeHtml(normalizeError(error))}</p>
        <button type="button" data-retry-orders>重新加载</button>
      </div>
    `;
  }
}

function renderOrders(orders) {
  if (!orders.length) {
    ordersList.innerHTML = "";
    ordersEmpty.hidden = false;
    return;
  }

  ordersList.innerHTML = orders
    .map(
      (order) => `
        <article class="order-card">
          <button class="order-summary" type="button" data-toggle-order="${escapeHtml(order.id)}" aria-expanded="false">
            <span class="order-row">
              <span class="order-no">${escapeHtml(order.orderNo)}</span>
              <span class="order-status ${order.status === "CANCELLED" ? "cancelled" : ""}">${escapeHtml(orderStatusText(order.status))}</span>
            </span>
            <span class="order-row">
              <span class="order-time">${escapeHtml(formatDateTime(order.createdAt))}</span>
              <strong class="order-amount">${formatPrice(order.totalAmount)}</strong>
            </span>
            <span class="order-toggle-hint">查看明细 ▾</span>
          </button>
          <div class="order-detail" hidden></div>
        </article>
      `
    )
    .join("");
}

function renderOrderDetail(order) {
  const items = order.items || [];
  return `
    <div class="order-items">
      ${items
        .map(
          (item) => `
            <div class="order-item">
              <img src="${escapeHtml(item.productImage)}" alt="${escapeHtml(item.productName)}">
              <div>
                <span class="order-item-name">${escapeHtml(item.productName)}</span>
                <span class="order-item-meta">${formatPrice(item.price)} × ${escapeHtml(item.quantity)}</span>
              </div>
              <strong>${formatPrice(item.subtotal)}</strong>
            </div>
          `
        )
        .join("")}
    </div>
    <div class="order-detail-footer">
      <span>共 ${items.length} 种商品，合计 <strong class="order-amount">${formatPrice(order.totalAmount)}</strong></span>
      ${order.status === "CREATED" ? `<button type="button" class="cancel-order-button" data-cancel-order="${escapeHtml(order.id)}">取消订单</button>` : ""}
    </div>
  `;
}

async function toggleOrderDetail(orderId, summaryButton) {
  const detail = summaryButton.closest(".order-card").querySelector(".order-detail");
  const hint = summaryButton.querySelector(".order-toggle-hint");
  const shouldClose = !detail.hidden;
  detail.hidden = shouldClose;
  summaryButton.setAttribute("aria-expanded", String(!shouldClose));
  hint.textContent = shouldClose ? "查看明细 ▾" : "收起明细 ▴";
  if (shouldClose) return;

  detail.innerHTML = `<div class="empty-state">明细加载中...</div>`;
  try {
    const order = await apiRequest(`/orders/${orderId}`);
    detail.innerHTML = renderOrderDetail(order);
  } catch (error) {
    detail.innerHTML = `<div class="empty-state">${escapeHtml(normalizeError(error))}</div>`;
  }
}

async function cancelOrder(orderId) {
  if (!window.confirm("确定要取消这个订单吗？取消后不可恢复。")) return;

  try {
    await apiRequest(`/orders/${orderId}/cancel`, { method: "POST" });
    showToast("订单已取消");
    await loadOrders();
    // 取消订单后端会返还该订单核销的优惠券，刷新券列表使其恢复可用
    loadCoupons();
  } catch (error) {
    showToast(normalizeError(error));
  }
}

function accountLinksHtml() {
  return `
    <div class="account-links">
      <button class="account-link" type="button" data-account-entry="orders">
        <span class="account-link-icon" aria-hidden="true">📦</span>
        <span>我的订单</span>
        <span class="account-link-arrow" aria-hidden="true">›</span>
      </button>
      <button class="account-link" type="button" data-account-entry="favorites">
        <span class="account-link-icon" aria-hidden="true">🧡</span>
        <span>我的收藏</span>
        <span class="account-link-arrow" aria-hidden="true">›</span>
      </button>
      <button class="account-link" type="button" data-account-entry="coupons">
        <span class="account-link-icon" aria-hidden="true">🎟️</span>
        <span>我的优惠券</span>
        <span class="account-link-arrow" aria-hidden="true">›</span>
      </button>
    </div>
    <div class="account-section" data-section="favorites" hidden>
      <h4>我的收藏</h4>
      <div class="account-favorites" aria-live="polite"></div>
    </div>
    <div class="account-section" data-section="coupons" hidden>
      <h4>我的优惠券</h4>
      <div class="account-coupons" aria-live="polite"></div>
    </div>
  `;
}

function renderAccountPanel() {
  if (!currentUser) {
    accountPanel.innerHTML = `<div class="empty-state">请先登录后查看个人信息。</div>`;
    return;
  }

  if (currentUser.mode === "guest") {
    accountPanel.innerHTML = `
      <div class="account-profile guest">
        <div class="account-avatar" aria-hidden="true">游</div>
        <div class="account-profile-info">
          <h3>游客模式</h3>
          <p class="account-guest-note">你正在以游客身份浏览商品，下单的订单会以游客标识保存，可在「我的订单」中查看。</p>
        </div>
      </div>
      ${accountLinksHtml()}
      <div class="account-guest-tip">注册账号可收藏商品、领取新人优惠券、同步订单。</div>
      <div class="account-footer">
        <button class="account-logout-button" type="button" data-account-entry="logout">退出游客</button>
      </div>
    `;
    return;
  }

  const user = currentUser.user || {};
  const nickname = user.nickname || user.username || currentUser.displayName || "宠物家长";
  const username = user.username || "—";
  const email = user.email || "";
  const avatarChar = String(nickname).trim().charAt(0).toUpperCase() || "P";
  accountPanel.innerHTML = `
    <div class="account-profile">
      <div class="account-avatar" aria-hidden="true">${escapeHtml(avatarChar)}</div>
      <div class="account-profile-info">
        <h3>${escapeHtml(nickname)}</h3>
        <div class="account-profile-meta">
          <span>用户名：${escapeHtml(username)}</span>
          <span>邮箱：${email ? escapeHtml(email) : "未设置"}</span>
        </div>
      </div>
    </div>
    ${accountLinksHtml()}
    <div class="account-footer">
      <button class="account-logout-button" type="button" data-account-entry="logout">退出登录</button>
    </div>
  `;
}

function toggleAccountSection(name) {
  const target = accountPanel.querySelector(`[data-section="${name}"]`);
  if (!target) return;

  const shouldShow = target.hidden;
  accountPanel.querySelectorAll(".account-section").forEach((node) => {
    node.hidden = true;
  });
  accountPanel.querySelectorAll(".account-link").forEach((node) => {
    node.classList.toggle("active", shouldShow && node.dataset.accountEntry === name);
  });
  target.hidden = !shouldShow;
  // 展开时懒加载对应列表（收藏/优惠券）
  if (shouldShow && name === "favorites") {
    loadAccountFavorites();
  }
  if (shouldShow && name === "coupons") {
    loadAccountCoupons();
  }
}

function openAccount() {
  closeCart();
  closeOrders();
  renderAccountPanel();
  accountDrawer.classList.add("open");
  accountDrawer.setAttribute("aria-hidden", "false");
  drawerBackdrop.hidden = false;
  closeAccountButton.focus();
}

function closeAccount() {
  accountDrawer.classList.remove("open");
  accountDrawer.setAttribute("aria-hidden", "true");
  drawerBackdrop.hidden = true;
}

async function loadStorefrontData() {
  const loaders = [
    loadCategories().catch((error) => {
      renderCategories();
      showToast(normalizeError(error));
    }),
    loadProducts().catch((error) => {
      productGrid.innerHTML = `<div class="empty-state">${escapeHtml(normalizeError(error))}</div>`;
    }),
    loadBundles().catch((error) => {
      bundleGrid.innerHTML = `<div class="empty-state">${escapeHtml(normalizeError(error))}</div>`;
    }),
    loadReviews().catch((error) => {
      reviewGrid.innerHTML = `<div class="empty-state">${escapeHtml(normalizeError(error))}</div>`;
    }),
    loadCart().catch((error) => {
      renderCart({ items: [], count: 0, totalAmount: 0 });
      showToast(normalizeError(error));
    }),
  ];
  // 登录用户额外初始化收藏集合与优惠券列表（游客不可收藏/持券，内部自行跳过）
  if (currentUser?.token) {
    loaders.push(loadFavorites(), loadCoupons());
  }
  await Promise.all(loaders);
}

function openCart() {
  cartDrawer.classList.add("open");
  cartDrawer.setAttribute("aria-hidden", "false");
  drawerBackdrop.hidden = false;
  closeCartButton.focus();
}

function closeCart() {
  cartDrawer.classList.remove("open");
  cartDrawer.setAttribute("aria-hidden", "true");
  drawerBackdrop.hidden = true;
}

function closeMenu() {
  navLinks.classList.remove("open");
  menuToggle.setAttribute("aria-expanded", "false");
  menuToggle.setAttribute("aria-label", "打开导航菜单");
}

categoryGrid.addEventListener("click", async (event) => {
  const button = event.target.closest("[data-filter]");
  if (!button) return;

  activeFilter = button.dataset.filter;
  categoryGrid.querySelectorAll(".category-card").forEach((item) => {
    const isActive = item === button;
    item.classList.toggle("active", isActive);
    item.setAttribute("aria-pressed", String(isActive));
  });

  try {
    await loadProducts();
  } catch (error) {
    productGrid.innerHTML = `<div class="empty-state">${escapeHtml(normalizeError(error))}</div>`;
  }
});

loginForm.addEventListener("submit", submitLogin);
registerButton.addEventListener("click", registerAccount);
guestLoginButton.addEventListener("click", enterAsGuest);
accountButton.addEventListener("click", openAccount);
closeAccountButton.addEventListener("click", closeAccount);

accountPanel.addEventListener("click", (event) => {
  const entryButton = event.target.closest("[data-account-entry]");
  if (!entryButton) return;

  const entry = entryButton.dataset.accountEntry;
  if (entry === "orders") {
    closeAccount();
    openOrders();
    return;
  }

  if (entry === "favorites" || entry === "coupons") {
    toggleAccountSection(entry);
    return;
  }

  if (entry === "logout") {
    logout();
  }
});

productGrid.addEventListener("click", (event) => {
  const favoriteButton = event.target.closest("[data-favorite]");
  if (favoriteButton) {
    // 收藏爱心：阻止冒泡，避免误触商品详情/加购
    event.stopPropagation();
    toggleFavorite(favoriteButton.dataset.favorite);
    return;
  }

  const addButton = event.target.closest("[data-add]");
  if (addButton) {
    addToCart(addButton.dataset.add);
    return;
  }

  const retryButton = event.target.closest("[data-retry-products]");
  if (retryButton) {
    loadProducts({ reset: true });
    return;
  }

  const detailTrigger = event.target.closest("[data-product-id]");
  if (detailTrigger) {
    openProductModal(detailTrigger.dataset.productId, detailTrigger);
  }
});

productsFooter.addEventListener("click", (event) => {
  if (event.target.closest("[data-load-more]") || event.target.closest("[data-retry-more]")) {
    loadProducts({ reset: false });
  }
});

productModalContent.addEventListener("click", (event) => {
  const favoriteButton = event.target.closest("[data-favorite]");
  if (favoriteButton) {
    // 详情模态框内的收藏爱心：阻止冒泡，避免误触加购/重试按钮
    event.stopPropagation();
    toggleFavorite(favoriteButton.dataset.favorite);
    return;
  }

  const retryButton = event.target.closest("[data-retry-product]");
  if (retryButton) {
    loadProductDetail(retryButton.dataset.retryProduct);
    return;
  }

  const addButton = event.target.closest("[data-modal-add]");
  if (addButton) {
    addToCart(addButton.dataset.modalAdd);
  }
});

closeProductModalButton.addEventListener("click", closeProductModal);
productModalBackdrop.addEventListener("click", closeProductModal);

ordersButton.addEventListener("click", openOrders);
closeOrdersButton.addEventListener("click", closeOrders);

ordersList.addEventListener("click", (event) => {
  const toggleButton = event.target.closest("[data-toggle-order]");
  if (toggleButton) {
    toggleOrderDetail(toggleButton.dataset.toggleOrder, toggleButton);
    return;
  }

  const cancelButton = event.target.closest("[data-cancel-order]");
  if (cancelButton) {
    cancelOrder(cancelButton.dataset.cancelOrder);
    return;
  }

  const retryButton = event.target.closest("[data-retry-orders]");
  if (retryButton) {
    loadOrders();
  }
});

cartItems.addEventListener("click", (event) => {
  const increase = event.target.closest("[data-increase]");
  const decrease = event.target.closest("[data-decrease]");

  if (increase) {
    const item = findCartItem(increase.dataset.increase);
    if (item) {
      changeCartQuantity(item.productId, item.quantity + 1);
    }
  }

  if (decrease) {
    const item = findCartItem(decrease.dataset.decrease);
    if (item) {
      changeCartQuantity(item.productId, item.quantity - 1);
    }
  }
});

cartItems.addEventListener("change", (event) => {
  const input = event.target.closest("[data-quantity]");
  if (!input) return;
  submitQuantityInput(input);
});

cartItems.addEventListener("keydown", (event) => {
  const input = event.target.closest("[data-quantity]");
  if (!input || event.key !== "Enter") return;
  event.preventDefault();
  input.blur();
});

cartButton.addEventListener("click", () => {
  closeOrders();
  closeAccount();
  openCart();
});
closeCartButton.addEventListener("click", closeCart);
drawerBackdrop.addEventListener("click", () => {
  closeCart();
  closeOrders();
  closeAccount();
});
clearCartButton.addEventListener("click", clearCart);
checkoutButton.addEventListener("click", checkoutCart);

menuToggle.addEventListener("click", () => {
  const isOpen = navLinks.classList.toggle("open");
  menuToggle.setAttribute("aria-expanded", String(isOpen));
  menuToggle.setAttribute("aria-label", isOpen ? "关闭导航菜单" : "打开导航菜单");
});

navLinks.addEventListener("click", closeMenu);

subscribeForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  if (!subscribeForm.checkValidity()) {
    subscribeForm.reportValidity();
    return;
  }

  const email = subscribeInput.value.trim().toLowerCase();
  subscribeMessage.textContent = "正在订阅...";
  try {
    await apiRequest("/subscriptions", {
      method: "POST",
      body: { email },
    });
    subscribeMessage.textContent = "订阅成功，新人券稍后发送到邮箱。";
    subscribeForm.reset();
    showToast("订阅成功");
  } catch (error) {
    subscribeMessage.textContent = normalizeError(error);
  }
});

document.addEventListener("keydown", (event) => {
  if (event.key === "Escape") {
    closeProductModal();
    closeCart();
    closeOrders();
    closeAccount();
    closeMenu();
  }
});

restoreAuth();

/* ===== 收藏 / 优惠券 ===== */

/** 已收藏商品 id 集合（登录后经 GET /api/favorites 初始化，游客为空集合） */
let favoriteIds = new Set();
/** 优惠券状态：items 为「我的优惠券」列表，selectedCouponId 为结算时选中的持券实例 id（null 表示不使用） */
let couponState = { items: [], selectedCouponId: null };

/** 是否为携带 token 的登录用户（游客模式返回 false，游客不可收藏/持券/用券） */
function isLoggedInUser() {
  return Boolean(currentUser?.token);
}

/* ---------- 收藏：爱心按钮 / 收藏列表 ---------- */

/** 登录后拉取收藏列表，初始化 favoriteIds 并同步页面上所有爱心按钮态 */
async function loadFavorites() {
  if (!isLoggedInUser()) return;
  try {
    const favorites = await apiRequest("/favorites");
    favoriteIds = new Set((favorites || []).map((favorite) => Number(favorite.productId)));
    syncFavoriteButtons();
  } catch {
    // 收藏加载失败不打断首页，爱心按钮保持默认未收藏态
  }
}

/** 同步范围内所有爱心按钮的选中态（商品卡片 / 详情模态框） */
function syncFavoriteButtons() {
  document.querySelectorAll("[data-favorite]").forEach((button) => {
    const isFavorite = favoriteIds.has(Number(button.dataset.favorite));
    button.classList.toggle("active", isFavorite);
    button.setAttribute("aria-pressed", String(isFavorite));
    button.textContent = isFavorite ? "♥" : "♡";
  });
}

/** 切换收藏：登录用户 POST/DELETE /api/favorites/{id}；游客 toast 引导登录 */
async function toggleFavorite(productId) {
  if (!isLoggedInUser()) {
    showToast("登录后可收藏商品");
    return;
  }

  const productIdNumber = Number(productId);
  const willFavorite = !favoriteIds.has(productIdNumber);
  // 乐观更新爱心态，失败时回滚
  if (willFavorite) {
    favoriteIds.add(productIdNumber);
  } else {
    favoriteIds.delete(productIdNumber);
  }
  syncFavoriteButtons();

  try {
    await apiRequest(`/favorites/${productIdNumber}`, { method: willFavorite ? "POST" : "DELETE" });
    showToast(willFavorite ? "已加入收藏" : "已取消收藏");
    // 个人中心收藏列表展开时同步刷新（取消收藏后卡片应移除）
    if (isAccountSectionVisible("favorites")) {
      loadAccountFavorites();
    }
  } catch (error) {
    if (willFavorite) {
      favoriteIds.delete(productIdNumber);
    } else {
      favoriteIds.add(productIdNumber);
    }
    syncFavoriteButtons();
    showToast(normalizeError(error));
  }
}

/** 收藏接口数据 → 商品卡片渲染所需的商品形状 */
function favoriteToProduct(favorite) {
  return {
    id: favorite.productId,
    name: favorite.productName,
    imageUrl: favorite.productImage,
    price: favorite.price,
    petType: favorite.petType,
    flavor: favorite.flavor,
    rating: favorite.rating,
    tags: [],
  };
}

/** 判断个人中心某个板块（favorites/coupons）当前是否展开 */
function isAccountSectionVisible(name) {
  const section = accountPanel.querySelector(`[data-section="${name}"]`);
  return Boolean(section && !section.hidden);
}

/** 加载个人中心「我的收藏」列表：复用商品卡片渲染（爱心取消收藏、加购、点开详情） */
async function loadAccountFavorites() {
  const container = accountPanel.querySelector(".account-favorites");
  if (!container) return;

  if (!isLoggedInUser()) {
    container.innerHTML = `<p class="account-section-empty">登录后可收藏喜欢的商品，游客模式暂不支持收藏。</p>`;
    return;
  }

  container.innerHTML = `<div class="empty-state">收藏加载中...</div>`;
  try {
    const favorites = await apiRequest("/favorites");
    favoriteIds = new Set((favorites || []).map((favorite) => Number(favorite.productId)));
    syncFavoriteButtons();
    if (!Array.isArray(favorites) || !favorites.length) {
      container.innerHTML = `<p class="account-section-empty">还没有收藏，去商品列表点 ♥ 收藏喜欢的零食吧。</p>`;
      return;
    }
    container.innerHTML = `<div class="account-product-grid">${favorites
      .map((favorite) => renderProductCard(favoriteToProduct(favorite)))
      .join("")}</div>`;
  } catch (error) {
    container.innerHTML = `
      <div class="empty-state">
        <p>${escapeHtml(normalizeError(error))}</p>
        <button type="button" data-retry-favorites>重新加载</button>
      </div>
    `;
  }
}

/** 个人中心内商品卡片的交互（收藏/加购/详情/重试），与首页商品网格的委托逻辑对齐 */
accountPanel.addEventListener("click", (event) => {
  const favoriteButton = event.target.closest("[data-favorite]");
  if (favoriteButton) {
    event.stopPropagation();
    toggleFavorite(favoriteButton.dataset.favorite);
    return;
  }

  const addButton = event.target.closest("[data-add]");
  if (addButton) {
    addToCart(addButton.dataset.add);
    return;
  }

  const retryFavoritesButton = event.target.closest("[data-retry-favorites]");
  if (retryFavoritesButton) {
    loadAccountFavorites();
    return;
  }

  const retryCouponsButton = event.target.closest("[data-retry-coupons]");
  if (retryCouponsButton) {
    loadAccountCoupons();
    return;
  }

  const detailTrigger = event.target.closest("[data-product-id]");
  if (detailTrigger) {
    openProductModal(detailTrigger.dataset.productId, detailTrigger);
  }
});

/* ---------- 优惠券：我的券列表 / 购物车选券 ---------- */

/** 券状态展示文案 */
function couponStatusText(status) {
  const statusMap = { 0: "可用", 1: "已使用", 2: "已过期" };
  return statusMap[Number(status)] || "未知";
}

/** 拉取「我的优惠券」列表并刷新购物车券行 */
async function loadCoupons() {
  if (!isLoggedInUser()) {
    couponState = { items: [], selectedCouponId: null };
    renderCouponSummary();
    return;
  }
  try {
    const coupons = await apiRequest("/coupons");
    couponState.items = Array.isArray(coupons) ? coupons : [];
    renderCouponSummary();
  } catch {
    couponState.items = [];
    renderCouponSummary();
  }
}

/** 参与结算的勾选条目小计之和（与后端下单口径一致：仅结算 checked 条目） */
function couponCheckedTotal() {
  return (cartState.items || [])
    .filter((item) => item.checked !== false)
    .reduce((sum, item) => sum + Number(item.subtotal || 0), 0);
}

/** 券的金额门槛是否满足（前端预判，最终以后端下单校验为准） */
function couponThresholdMet(coupon) {
  return couponCheckedTotal() >= Number(coupon.minSpend || 0);
}

/** 券当前是否可选：未使用且未过期（后端 available 标记）且购物车金额满门槛 */
function isCouponUsable(coupon) {
  return Number(coupon.status) === 0 && Boolean(coupon.available) && couponThresholdMet(coupon);
}

/** 当前选中的券对象（未选中或已失效时返回 null） */
function selectedCoupon() {
  if (!couponState.selectedCouponId) return null;
  const coupon = couponState.items.find((item) => Number(item.id) === Number(couponState.selectedCouponId));
  return coupon && isCouponUsable(coupon) ? coupon : null;
}

/** 渲染购物车摘要中的优惠券行：登录且可展示时出现「优惠券选择 + 抵扣 + 实付」，游客不显示 */
function renderCouponSummary() {
  const couponBox = document.querySelector(".cart-coupon");
  if (!couponBox) return;

  // 一致做法：游客不显示券行（登录引导在个人中心/收藏入口处提示）
  couponBox.hidden = !isLoggedInUser();
  if (!isLoggedInUser()) return;

  const select = couponBox.querySelector(".coupon-select");
  const discountLine = couponBox.querySelector(".coupon-discount-line");
  const payableLine = couponBox.querySelector(".coupon-payable-line");

  // 未用且未过期的券进入下拉；门槛未满足的禁用并说明差价
  const candidateCoupons = couponState.items.filter(
    (coupon) => Number(coupon.status) === 0 && Boolean(coupon.available)
  );
  const options = [`<option value="">不使用优惠券</option>`];
  candidateCoupons.forEach((coupon) => {
    const met = couponThresholdMet(coupon);
    const remain = Math.max(Number(coupon.minSpend || 0) - couponCheckedTotal(), 0);
    const label = met
      ? `${coupon.name}：减¥${coupon.amount}`
      : `${coupon.name}：减¥${coupon.amount}（差¥${remain.toFixed(0)}可用）`;
    options.push(
      `<option value="${escapeHtml(coupon.id)}" ${met ? "" : "disabled"}>${escapeHtml(label)}</option>`
    );
  });
  select.innerHTML = options.join("");

  // 选中的券不再可用（已被使用/过期/门槛不满足）时回到「不使用」
  const activeCoupon = selectedCoupon();
  if (!activeCoupon) {
    couponState.selectedCouponId = null;
  }
  select.value = activeCoupon ? String(activeCoupon.id) : "";

  // 抵扣金额与实付金额（实付 = 小计 - 抵扣，不为负）
  const subtotal = Number(cartState.totalAmount || 0);
  const discount = activeCoupon ? Math.min(Number(activeCoupon.amount || 0), couponCheckedTotal()) : 0;
  const payable = Math.max(subtotal - discount, 0);
  couponBox.querySelector(".coupon-discount").textContent = discount > 0 ? `-¥${discount.toFixed(0)}` : "-¥0";
  couponBox.querySelector(".coupon-payable").textContent = formatPrice(payable);
  discountLine.hidden = !activeCoupon;
  payableLine.hidden = !activeCoupon;
}

/** 购物车券下拉切换选中券 */
document.querySelector(".coupon-select")?.addEventListener("change", (event) => {
  couponState.selectedCouponId = event.target.value ? Number(event.target.value) : null;
  renderCouponSummary();
});

/** 加载个人中心「我的优惠券」列表 */
async function loadAccountCoupons() {
  const container = accountPanel.querySelector(".account-coupons");
  if (!container) return;

  if (!isLoggedInUser()) {
    container.innerHTML = `<p class="account-section-empty">登录后可领取新人优惠券，游客模式暂不支持。</p>`;
    return;
  }

  container.innerHTML = `<div class="empty-state">优惠券加载中...</div>`;
  try {
    const coupons = await apiRequest("/coupons");
    couponState.items = Array.isArray(coupons) ? coupons : [];
    renderCouponSummary();
    if (!couponState.items.length) {
      container.innerHTML = `<p class="account-section-empty">暂无优惠券，注册/订阅可领新人券</p>`;
      return;
    }
    container.innerHTML = couponState.items.map(renderCouponCard).join("");
  } catch (error) {
    container.innerHTML = `
      <div class="empty-state">
        <p>${escapeHtml(normalizeError(error))}</p>
        <button type="button" data-retry-coupons>重新加载</button>
      </div>
    `;
  }
}

/** 单张优惠券卡片（金额、门槛、有效期、状态） */
function renderCouponCard(coupon) {
  const status = Number(coupon.status);
  const statusClass = status === 0 ? "usable" : status === 1 ? "used" : "expired";
  return `
    <article class="coupon-card ${statusClass}">
      <div class="coupon-amount">
        <strong>¥${escapeHtml(coupon.amount)}</strong>
        <small>满¥${escapeHtml(coupon.minSpend)}可用</small>
      </div>
      <div class="coupon-info">
        <span class="coupon-name">${escapeHtml(coupon.name)}</span>
        <span class="coupon-expire">有效期至 ${escapeHtml(formatDateTime(coupon.expiresAt))}</span>
      </div>
      <span class="coupon-status">${escapeHtml(couponStatusText(status))}</span>
    </article>
  `;
}

/** 退出登录/登录失效时清空收藏与优惠券状态并复位爱心按钮 */
function resetFavoritesAndCoupons() {
  favoriteIds = new Set();
  couponState = { items: [], selectedCouponId: null };
  syncFavoriteButtons();
  renderCouponSummary();
}
