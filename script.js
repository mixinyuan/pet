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

let currentUser = null;
let activeFilter = "all";
let cartState = { items: [], count: 0, totalAmount: 0 };
let toastTimer;

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
  accountButton.setAttribute("aria-label", `退出${currentUser.displayName}`);
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
  closeMenu();
  document.body.classList.add("auth-locked");
  loginScreen.setAttribute("aria-hidden", "false");
  loginMessage.textContent = message;
  usernameInput.focus();
  renderCart({ items: [], count: 0, totalAmount: 0 });
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

function renderProducts(products = []) {
  if (!products.length) {
    productGrid.innerHTML = `<div class="empty-state">暂时没有符合条件的商品。</div>`;
    return;
  }

  productGrid.innerHTML = products
    .map(
      (product) => `
        <article class="product-card">
          <div class="product-image">
            <img src="${escapeHtml(product.imageUrl)}" alt="${escapeHtml(product.name)}" loading="lazy">
            <div class="tag-row">
              ${(product.tags || []).map((tag) => `<span class="tag">${escapeHtml(tag)}</span>`).join("")}
            </div>
          </div>
          <div class="product-info">
            <h3>${escapeHtml(product.name)}</h3>
            <div class="meta">${escapeHtml(product.petType)} · ${escapeHtml(product.flavor)}</div>
            <div class="rating">★ ${escapeHtml(product.rating)} / 5.0</div>
            <div class="price-row">
              <span class="price">${formatPrice(product.price)}</span>
              <button type="button" data-add="${product.id}" aria-label="将${escapeHtml(product.name)}加入购物车">加入购物车</button>
            </div>
          </div>
        </article>
      `
    )
    .join("");
}

async function loadProducts() {
  productGrid.innerHTML = `<div class="empty-state">商品加载中...</div>`;
  const params = new URLSearchParams({
    page: "1",
    size: "20",
    category: activeFilter,
  });
  const result = await apiRequest(`/products?${params.toString()}`);
  renderProducts(result.records || []);
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
                  value="${item.quantity}"
                  data-quantity="${item.productId}"
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
    const order = await apiRequest("/orders", { method: "POST" });
    renderCart({ items: [], count: 0, totalAmount: 0 });
    closeCart();
    showToast(`订单${order.orderNo}已提交`);
  } catch (error) {
    showToast(normalizeError(error));
  }
}

async function loadStorefrontData() {
  await Promise.all([
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
  ]);
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
accountButton.addEventListener("click", logout);

productGrid.addEventListener("click", (event) => {
  const button = event.target.closest("[data-add]");
  if (!button) return;
  addToCart(button.dataset.add);
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

cartButton.addEventListener("click", openCart);
closeCartButton.addEventListener("click", closeCart);
drawerBackdrop.addEventListener("click", closeCart);
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
    closeCart();
    closeMenu();
  }
});

restoreAuth();
