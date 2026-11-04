# PawBites Backend

Java 21 + Spring Boot + MyBatis-Plus + MySQL backend for the PawBites pet snack shop.

## Run Locally

1. Create the database:

```sql
CREATE DATABASE pawbites DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. Start the app:

```powershell
cd E:\pet\backend
$env:DB_USERNAME="root"
$env:DB_PASSWORD="root"
mvn spring-boot:run
```

Flyway runs the schema and seed migrations automatically.

The frontend calls `http://localhost:8080/api` by default. To point it to another backend address in the browser console:

```js
localStorage.setItem("pawbites-api-base", "http://localhost:8080/api");
location.reload();
```

## Main APIs

```text
POST   /api/auth/register
POST   /api/auth/login
GET    /api/auth/me

GET    /api/categories
GET    /api/products?page=1&size=20&category=all
GET    /api/products/{id}
GET    /api/bundles
GET    /api/reviews

GET    /api/cart
POST   /api/cart/items
PUT    /api/cart/items/{productId}
DELETE /api/cart/items/{productId}
DELETE /api/cart

POST   /api/orders
GET    /api/orders
GET    /api/orders/{id}
PUT    /api/orders/{id}/cancel

POST   /api/subscriptions
```

Authenticated calls use `Authorization: Bearer <token>`. Guest cart/order calls can use `X-Guest-Id: <stable-browser-id>`.
