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
$env:JWT_SECRET="<your-secret-of-at-least-32-bytes>"
$env:DB_URL="jdbc:mysql://localhost:3306/pawbites?useSSL=false&serverTimezone=Asia/Shanghai"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="root"
mvn spring-boot:run
```

Environment variables:

| Variable | Required | Description |
| --- | --- | --- |
| `JWT_SECRET` | Yes | Secret used to sign JWT tokens. Must be at least 32 bytes long; the application fails to start if it is missing or too short. |
| `DB_URL` | Yes | JDBC URL of the MySQL database (e.g. `jdbc:mysql://localhost:3306/pawbites?useSSL=false&serverTimezone=Asia/Shanghai`). |
| `DB_USERNAME` | Yes | MySQL username. |
| `DB_PASSWORD` | Yes | MySQL password. |

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
GET    /api/orders?page=1&size=20
GET    /api/orders/{id}
POST   /api/orders/{id}/cancel

POST   /api/subscriptions
```

Authenticated calls use `Authorization: Bearer <token>`. Guest cart/order calls can use `X-Guest-Id: <stable-browser-id>`.
