const products = [
  {
    id: "chicken-freeze",
    name: "原切鸡肉冻干",
    pet: "猫狗通用",
    flavor: "鸡胸肉",
    price: 39,
    rating: "4.9",
    categories: ["cat", "dog", "freeze"],
    tags: ["热销", "高蛋白"],
    image: "https://images.unsplash.com/photo-1601758125946-6ec2ef64daf8?auto=format&fit=crop&w=700&q=80",
  },
  {
    id: "beef-training",
    name: "牛肉训练奖励粒",
    pet: "狗狗适用",
    flavor: "牛肉南瓜",
    price: 32,
    rating: "4.8",
    categories: ["dog", "training"],
    tags: ["训练", "低脂"],
    image: "https://images.unsplash.com/photo-1568640347023-a616a30bc3bd?auto=format&fit=crop&w=700&q=80",
  },
  {
    id: "cod-strip",
    name: "鳕鱼鲜肉条",
    pet: "猫咪适用",
    flavor: "深海鳕鱼",
    price: 28,
    rating: "4.7",
    categories: ["cat", "training"],
    tags: ["无谷", "新品"],
    image: "https://images.unsplash.com/photo-1573865526739-10659fec78a5?auto=format&fit=crop&w=700&q=80",
  },
  {
    id: "dental-stick",
    name: "薄荷洁齿棒",
    pet: "狗狗适用",
    flavor: "鸡肉薄荷",
    price: 45,
    rating: "4.8",
    categories: ["dog", "dental"],
    tags: ["洁齿", "清新"],
    image: "https://images.unsplash.com/photo-1583337130417-3346a1be7dee?auto=format&fit=crop&w=700&q=80",
  },
  {
    id: "duck-freeze",
    name: "鸭肉梨粒冻干",
    pet: "猫狗通用",
    flavor: "鸭肉雪梨",
    price: 42,
    rating: "4.9",
    categories: ["cat", "dog", "freeze"],
    tags: ["低敏", "热销"],
    image: "https://images.unsplash.com/photo-1548199973-03cce0bbc87b?auto=format&fit=crop&w=700&q=80",
  },
  {
    id: "salmon-bites",
    name: "三文鱼软粒",
    pet: "猫咪适用",
    flavor: "三文鱼",
    price: 36,
    rating: "4.6",
    categories: ["cat", "training"],
    tags: ["亮毛", "小颗粒"],
    image: "https://images.unsplash.com/photo-1596854407944-bf87f6fdd49e?auto=format&fit=crop&w=700&q=80",
  },
  {
    id: "goat-milk",
    name: "羊奶酪脆块",
    pet: "猫狗通用",
    flavor: "羊奶酪",
    price: 30,
    rating: "4.7",
    categories: ["cat", "dog"],
    tags: ["补钙", "香脆"],
    image: "https://images.unsplash.com/photo-1583511655826-05700d52f4d9?auto=format&fit=crop&w=700&q=80",
  },
  {
    id: "rabbit-dental",
    name: "兔肉洁齿小棒",
    pet: "小型犬适用",
    flavor: "兔肉胡萝卜",
    price: 49,
    rating: "4.8",
    categories: ["dog", "dental"],
    tags: ["洁齿", "低敏"],
    image: "https://images.unsplash.com/photo-1537151608828-ea2b11777ee8?auto=format&fit=crop&w=700&q=80",
  },
];

const productGrid = document.querySelector("#productGrid");
const categoryButtons = document.querySelectorAll(".category-card");
const loginScreen = document.querySelector(".login-screen");
const loginForm = document.querySelector("#loginForm");
const usernameInput = document.querySelector("#username");
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

const AUTH_STORAGE_KEY = "pawbites-auth";
const CART_STORAGE_KEY = "pawbites-cart";
const SUBSCRIBE_STORAGE_KEY = "pawbites-subscribers";
const cart = new Map();
let activeFilter = "all";
let currentUser = null;
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
    // Ignore storage failures; the in-memory state still works.
  }
}

function getProduct(id) {
  return products.find((item) => item.id === id);
}

function formatPrice(value) {
  return `¥${value}`;
}

function updateAccount() {
  if (!accountName || !currentUser) return;
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
}

function restoreAuth() {
  const savedProfile = readStoredValue(AUTH_STORAGE_KEY);
  if (!savedProfile || !savedProfile.displayName) return;
  unlockPage(savedProfile);
}

function submitLogin(event) {
  event.preventDefault();
  if (!loginForm.checkValidity()) {
    loginForm.reportValidity();
    return;
  }

  const displayName = usernameInput.value.trim();
  if (!displayName) {
    loginMessage.textContent = "请输入有效账号。";
    usernameInput.focus();
    return;
  }

  unlockPage(
    {
      mode: "user",
      displayName,
    },
    {
      focusHome: true,
      showWelcome: true,
    }
  );
}

function enterAsGuest() {
  unlockPage(
    {
      mode: "guest",
      displayName: "游客模式",
    },
    {
      focusHome: true,
      showWelcome: true,
    }
  );
}

function renderProducts() {
  const visibleProducts =
    activeFilter === "all"
      ? products
      : products.filter((product) => product.categories.includes(activeFilter));

  productGrid.innerHTML = visibleProducts
    .map(
      (product) => `
        <article class="product-card">
          <div class="product-image">
            <img src="${product.image}" alt="${product.name}" loading="lazy">
            <div class="tag-row">
              ${product.tags.map((tag) => `<span class="tag">${tag}</span>`).join("")}
            </div>
          </div>
          <div class="product-info">
            <h3>${product.name}</h3>
            <div class="meta">${product.pet} · ${product.flavor}</div>
            <div class="rating">★ ${product.rating} / 5.0</div>
            <div class="price-row">
              <span class="price">${formatPrice(product.price)}</span>
              <button type="button" data-add="${product.id}" aria-label="将${product.name}加入购物车">加入购物车</button>
            </div>
          </div>
        </article>
      `
    )
    .join("");
}

function restoreCart() {
  const savedCart = readStoredValue(CART_STORAGE_KEY, {});
  Object.entries(savedCart).forEach(([id, quantity]) => {
    const normalizedQuantity = Number(quantity);
    if (getProduct(id) && Number.isInteger(normalizedQuantity) && normalizedQuantity > 0) {
      cart.set(id, normalizedQuantity);
    }
  });
}

function persistCart() {
  writeStoredValue(CART_STORAGE_KEY, Object.fromEntries(cart));
}

function updateCart() {
  const entries = [...cart.entries()];
  const count = entries.reduce((sum, [, quantity]) => sum + quantity, 0);
  const total = entries.reduce((sum, [id, quantity]) => {
    const product = getProduct(id);
    return product ? sum + product.price * quantity : sum;
  }, 0);

  cartCount.textContent = count;
  cartTotal.textContent = formatPrice(total);
  cartButton.setAttribute("aria-label", count > 0 ? `打开购物车，共${count}件商品` : "打开购物车");
  cartEmpty.hidden = entries.length > 0;
  clearCartButton.disabled = entries.length === 0;
  checkoutButton.disabled = entries.length === 0;
  persistCart();

  cartItems.innerHTML = entries
    .map(([id, quantity]) => {
      const product = getProduct(id);
      if (!product) return "";
      return `
        <article class="cart-item">
          <img src="${product.image}" alt="${product.name}">
          <div>
            <h3>${product.name}</h3>
            <div class="cart-line">
              <span>${formatPrice(product.price)}</span>
              <div class="stepper" aria-label="${product.name}数量">
                <button type="button" data-decrease="${id}" aria-label="减少${product.name}数量">−</button>
                <span>${quantity}</span>
                <button type="button" data-increase="${id}" aria-label="增加${product.name}数量">+</button>
              </div>
            </div>
          </div>
        </article>
      `;
    })
    .join("");
}

function showToast(message) {
  clearTimeout(toastTimer);
  toast.textContent = message;
  toast.classList.add("show");
  toastTimer = setTimeout(() => {
    toast.classList.remove("show");
  }, 1800);
}

function addToCart(id) {
  const product = getProduct(id);
  if (!product) return;
  cart.set(id, (cart.get(id) || 0) + 1);
  updateCart();
  showToast(`${product.name}已加入购物车`);
}

function clearCart() {
  if (cart.size === 0) return;
  cart.clear();
  updateCart();
  showToast("购物车已清空");
}

function checkoutCart() {
  if (cart.size === 0) {
    openCart();
    showToast("购物车为空，先挑选商品吧");
    return;
  }

  const count = [...cart.values()].reduce((sum, quantity) => sum + quantity, 0);
  cart.clear();
  updateCart();
  closeCart();
  showToast(`订单已提交，共${count}件商品`);
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

categoryButtons.forEach((button) => {
  button.addEventListener("click", () => {
    activeFilter = button.dataset.filter;
    categoryButtons.forEach((item) => item.classList.toggle("active", item === button));
    categoryButtons.forEach((item) => item.setAttribute("aria-pressed", String(item === button)));
    renderProducts();
  });
});

loginForm.addEventListener("submit", submitLogin);
guestLoginButton.addEventListener("click", enterAsGuest);
accountButton.addEventListener("click", () => {
  lockPage("已退出登录，可以重新进入。");
});

productGrid.addEventListener("click", (event) => {
  const button = event.target.closest("[data-add]");
  if (!button) return;
  addToCart(button.dataset.add);
});

cartItems.addEventListener("click", (event) => {
  const increase = event.target.closest("[data-increase]");
  const decrease = event.target.closest("[data-decrease]");

  if (increase) {
    cart.set(increase.dataset.increase, (cart.get(increase.dataset.increase) || 0) + 1);
  }

  if (decrease) {
    const id = decrease.dataset.decrease;
    const nextQuantity = (cart.get(id) || 0) - 1;
    if (nextQuantity <= 0) {
      cart.delete(id);
    } else {
      cart.set(id, nextQuantity);
    }
  }

  if (increase || decrease) {
    updateCart();
  }
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

navLinks.addEventListener("click", () => {
  closeMenu();
});

subscribeForm.addEventListener("submit", (event) => {
  event.preventDefault();
  if (!subscribeForm.checkValidity()) {
    subscribeForm.reportValidity();
    return;
  }

  const email = subscribeInput.value.trim().toLowerCase();
  if (!email) {
    subscribeMessage.textContent = "请输入邮箱后再订阅。";
    subscribeInput.focus();
    return;
  }

  const savedSubscribers = readStoredValue(SUBSCRIBE_STORAGE_KEY, []);
  const subscribers = Array.isArray(savedSubscribers) ? savedSubscribers : [];
  const alreadySubscribed = subscribers.includes(email);

  if (!alreadySubscribed) {
    subscribers.push(email);
    writeStoredValue(SUBSCRIBE_STORAGE_KEY, subscribers);
  }

  subscribeMessage.textContent = alreadySubscribed ? "这个邮箱已经订阅过优惠。" : "订阅成功，新人券稍后发送到邮箱。";
  subscribeForm.reset();
  showToast(alreadySubscribed ? "邮箱已订阅" : "订阅成功");
});

document.addEventListener("keydown", (event) => {
  if (event.key === "Escape") {
    closeCart();
    closeMenu();
  }
});

categoryButtons.forEach((button) => {
  button.setAttribute("aria-pressed", String(button.classList.contains("active")));
});

restoreAuth();
restoreCart();
renderProducts();
updateCart();
