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
const cartButton = document.querySelector(".cart-button");
const cartDrawer = document.querySelector(".cart-drawer");
const drawerBackdrop = document.querySelector(".drawer-backdrop");
const closeCartButton = document.querySelector(".close-cart");
const cartItems = document.querySelector(".cart-items");
const cartEmpty = document.querySelector(".cart-empty");
const cartCount = document.querySelector(".cart-count");
const cartTotal = document.querySelector(".cart-total");
const toast = document.querySelector(".toast");
const menuToggle = document.querySelector(".menu-toggle");
const navLinks = document.querySelector(".nav-links");

const cart = new Map();
let activeFilter = "all";
let toastTimer;

function formatPrice(value) {
  return `¥${value}`;
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

function updateCart() {
  const entries = [...cart.entries()];
  const count = entries.reduce((sum, [, quantity]) => sum + quantity, 0);
  const total = entries.reduce((sum, [id, quantity]) => {
    const product = products.find((item) => item.id === id);
    return sum + product.price * quantity;
  }, 0);

  cartCount.textContent = count;
  cartTotal.textContent = formatPrice(total);
  cartEmpty.hidden = entries.length > 0;

  cartItems.innerHTML = entries
    .map(([id, quantity]) => {
      const product = products.find((item) => item.id === id);
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
  cart.set(id, (cart.get(id) || 0) + 1);
  updateCart();
  const product = products.find((item) => item.id === id);
  showToast(`${product.name}已加入购物车`);
}

function openCart() {
  cartDrawer.classList.add("open");
  cartDrawer.setAttribute("aria-hidden", "false");
  drawerBackdrop.hidden = false;
}

function closeCart() {
  cartDrawer.classList.remove("open");
  cartDrawer.setAttribute("aria-hidden", "true");
  drawerBackdrop.hidden = true;
}

categoryButtons.forEach((button) => {
  button.addEventListener("click", () => {
    activeFilter = button.dataset.filter;
    categoryButtons.forEach((item) => item.classList.toggle("active", item === button));
    renderProducts();
  });
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

menuToggle.addEventListener("click", () => {
  const isOpen = navLinks.classList.toggle("open");
  menuToggle.setAttribute("aria-expanded", String(isOpen));
  menuToggle.setAttribute("aria-label", isOpen ? "关闭导航菜单" : "打开导航菜单");
});

navLinks.addEventListener("click", () => {
  navLinks.classList.remove("open");
  menuToggle.setAttribute("aria-expanded", "false");
  menuToggle.setAttribute("aria-label", "打开导航菜单");
});

document.addEventListener("keydown", (event) => {
  if (event.key === "Escape") {
    closeCart();
    navLinks.classList.remove("open");
    menuToggle.setAttribute("aria-expanded", "false");
  }
});

renderProducts();
updateCart();
