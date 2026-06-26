# Mini Commerce Kampus

Backend sistem e-commerce kampus berbasis microservices menggunakan Spring Boot.

SKPI BackEnd Java Springboot

Muhammad Azharuddin Hamid
221524018
---

## Deskripsi

Proyek ini terdiri dari tiga service yang berjalan secara mandiri:

- **Catalog Service** — mengelola data produk (CRUD, stok, status)
- **Order Service** — mengelola order, pembayaran, dan pembatalan
- **Gateway Service** — single entry point untuk semua request dari client

Order Service berkomunikasi dengan Catalog Service melalui HTTP REST. Tidak ada akses langsung antar database.

---

## Arsitektur

```
Client / Postman
      |
      | HTTP
      v
+------------------+
|  Gateway Service |  localhost:8080
+--------+---------+
         |
         +-----------------------------------+
         |                                   |
         v                                   v
+------------------+               +------------------+
|  Catalog Service |               |  Order Service   |
|  localhost:8081  | <-- HTTP/REST |  localhost:8082  |
+--------+---------+               +--------+---------+
         |                                  |
         v                                  v
+------------------+               +------------------+
|    catalog_db    |               |     order_db     |
|   PostgreSQL     |               |   PostgreSQL     |
+------------------+               +------------------+
```

**Aturan utama:**
- Catalog Service hanya mengakses `catalog_db`
- Order Service hanya mengakses `order_db`
- Order Service mengambil data produk dan mengubah stok melalui HTTP ke Catalog Service
- Nama dan harga produk disimpan sebagai **snapshot** di order_items — tidak berubah walaupun produk diperbarui

---

## Teknologi

| Komponen | Detail |
|---|---|
| Bahasa | Java 17 |
| Framework | Spring Boot 3.5.15 |
| API Gateway | Spring Cloud Gateway 2024.0.0 |
| Build Tool | Maven |
| Database | PostgreSQL 16 |
| ORM | Spring Data JPA / Hibernate |
| Validasi | Jakarta Bean Validation |
| HTTP Client | Spring RestClient |
| Library | Lombok |
| Dokumentasi API | Springdoc OpenAPI (Swagger UI) |
| Testing | JUnit 5 + Mockito + H2 (in-memory) |
| Containerisasi | Docker + Docker Compose |

---

## Struktur Folder

```
221524018_Muhammad Azharuddin Hamid_SKPI2026-BE/
├── campus_commerce/
│   ├── catalog/                    ← Catalog Service (port 8081)
│   │   └── src/main/java/com/campus_commerce/catalog/
│   │       ├── config/
│   │       ├── controller/
│   │       ├── dto/
│   │       │   ├── request/
│   │       │   └── response/
│   │       ├── exception/
│   │       ├── model/
│   │       │   ├── entity/
│   │       │   └── enums/
│   │       ├── repository/
│   │       └── service/
│   │           └── impl/
│   ├── order/                      ← Order Service (port 8082)
│   │   └── src/main/java/com/campus_commerce/order/
│   │       ├── client/
│   │       │   └── dto/
│   │       ├── config/
│   │       ├── controller/
│   │       ├── dto/
│   │       │   ├── request/
│   │       │   └── response/
│   │       ├── exception/
│   │       ├── mapper/
│   │       ├── model/
│   │       │   ├── entity/
│   │       │   └── enums/
│   │       ├── repository/
│   │       └── service/
│   │           └── impl/
│   ├── gateway/                    ← API Gateway (port 8080)
│   │   └── src/main/java/com/campus_commerce/gateway/
│   │       ├── config/
│   │       └── filter/
│   └── docker-compose.yml
├── database/
│   ├── catalog-ddl.sql
│   └── order-ddl.sql
├── postman/
│   └── campus-commerce.postman_collection.json
├── README.md
└── AI_USAGE.md
```

---

## Requirement Sistem

**Lokal:**
- Java 17+
- Maven 3.6+
- PostgreSQL 14+

**Docker:**
- Docker Desktop (atau Docker Engine + Docker Compose v2)

---

## Setup Database (Lokal)

Jalankan perintah berikut di psql atau pgAdmin sebagai superuser:

```sql
-- Buat user
CREATE ROLE catalog_user WITH LOGIN PASSWORD 'Catalog123!';
CREATE ROLE order_user   WITH LOGIN PASSWORD 'Order123!';

-- Buat database
CREATE DATABASE catalog_db OWNER catalog_user;
CREATE DATABASE order_db   OWNER order_user;
```

Tabel dibuat otomatis saat service pertama kali dijalankan (`ddl-auto=update`).

DDL manual tersedia di folder `database/` untuk referensi.

---

## Konfigurasi Environment

Credentials database dibaca dari environment variable. Jika tidak di-set, service menggunakan nilai default.

| Variable | Default | Digunakan oleh |
|---|---|---|
| `CATALOG_DB_HOST` | `localhost` | Catalog Service |
| `CATALOG_DB_USERNAME` | `catalog_user` | Catalog Service |
| `CATALOG_DB_PASSWORD` | `Catalog123!` | Catalog Service |
| `ORDER_DB_HOST` | `localhost` | Order Service |
| `ORDER_DB_USERNAME` | `order_user` | Order Service |
| `ORDER_DB_PASSWORD` | `Order123!` | Order Service |
| `CATALOG_SERVICE_URL` | `http://localhost:8081` | Order Service |
| `CATALOG_SERVICE_URL` | `http://localhost:8081` | Gateway Service |
| `ORDER_SERVICE_URL` | `http://localhost:8082` | Gateway Service |

---

## Cara Menjalankan

### Opsi 1: Docker Compose (Direkomendasikan)

```bash
# Jalankan semua service sekaligus
docker compose up --build

# Jalankan di background
docker compose up --build -d

# Hentikan semua service
docker compose down
```

Semua service, database, dan network dibuat otomatis. Client cukup gunakan satu base URL:

```
http://localhost:8080
```

### Opsi 2: Lokal (tanpa Docker)

**1. Catalog Service:**
```bash
cd campus_commerce/catalog
./mvnw spring-boot:run
```
Service berjalan di `http://localhost:8081`

**2. Order Service (jalankan setelah Catalog Service aktif):**
```bash
cd campus_commerce/order
./mvnw spring-boot:run
```
Service berjalan di `http://localhost:8082`

**3. Gateway Service (opsional, jalankan terakhir):**
```bash
cd campus_commerce/gateway
./mvnw spring-boot:run
```
Service berjalan di `http://localhost:8080`

> Urutan penting: Catalog Service harus aktif sebelum Order Service, karena Order Service memanggil Catalog Service saat pembuatan order.

---

## Daftar Endpoint

### Via API Gateway `http://localhost:8080`

Semua endpoint di bawah dapat diakses melalui gateway dengan mengganti base URL ke `http://localhost:8080`.

### Catalog Service `http://localhost:8081`

| Method | Endpoint | Fungsi |
|---|---|---|
| POST | `/api/products` | Buat produk baru |
| GET | `/api/products` | Daftar semua produk (paginasi + filter) |
| GET | `/api/products/{id}` | Detail produk |
| PATCH | `/api/products/{id}/stock` | Update stok (INCREASE/DECREASE) |
| PATCH | `/api/products/{id}/status` | Ubah status (ACTIVE/INACTIVE) |
| PATCH | `/api/products/{id}/reduce-stock` | Kurangi stok (internal) |
| PATCH | `/api/products/{id}/restore-stock` | Kembalikan stok (internal) |

**Query parameter GET `/api/products`:**

| Parameter | Tipe | Deskripsi |
|---|---|---|
| `name` | String | Filter nama produk (case-insensitive, partial match) |
| `status` | String | Filter status (`ACTIVE` / `INACTIVE`) |
| `page` | int | Halaman (default 0) |
| `size` | int | Ukuran halaman (default 10) |

### Order Service `http://localhost:8082`

| Method | Endpoint | Fungsi |
|---|---|---|
| POST | `/api/orders` | Buat order baru |
| GET | `/api/orders` | Daftar semua order (paginasi + filter) |
| GET | `/api/orders/{id}` | Detail order |
| PATCH | `/api/orders/{id}/pay` | Bayar order |
| PATCH | `/api/orders/{id}/cancel` | Batalkan order |

**Query parameter GET `/api/orders`:**

| Parameter | Tipe | Deskripsi |
|---|---|---|
| `customerName` | String | Filter nama customer (partial match) |
| `status` | String | Filter status (`PENDING` / `PAID` / `CANCELLED`) |
| `page` | int | Halaman (default 0) |
| `size` | int | Ukuran halaman (default 10) |

### Swagger UI

| Service | URL |
|---|---|
| Catalog Service | `http://localhost:8081/swagger-ui.html` |
| Order Service | `http://localhost:8082/swagger-ui.html` |

---

## API Gateway

Gateway Service (port 8080) berfungsi sebagai single entry point. Client tidak perlu tahu port spesifik masing-masing service.

**Routing:**

| Path | Diteruskan ke |
|---|---|
| `/api/products/**` | Catalog Service `:8081` |
| `/api/orders/**` | Order Service `:8082` |

**Fitur gateway:**
- CORS configured untuk semua origin
- Request/response logging otomatis (method + path + status code)
- Environment variable `CATALOG_SERVICE_URL` dan `ORDER_SERVICE_URL` dapat diubah saat deploy

---

## Idempotency (Create Order)

`POST /api/orders` mendukung **Idempotency-Key** header untuk mencegah duplikasi order akibat retry dari client.

**Cara pakai:**
```http
POST http://localhost:8080/api/orders
Content-Type: application/json
Idempotency-Key: order-abc-12345

{
  "customerName": "Siti Rahayu",
  "customerEmail": "siti@example.com",
  "items": [{ "productId": 1, "quantity": 2 }]
}
```

**Perilaku:**
- Request pertama dengan key tertentu → order dibuat, stok dikurangi, response 201
- Request ulang dengan key yang sama → order yang sudah ada dikembalikan, response 200
- Request tanpa key → perilaku normal (tanpa idempotency)

Lihat `docs/IDEMPOTENCY.md` untuk detail lengkap.

---

## Contoh Request dan Response

### POST /api/products

**Request:**
```json
{
  "sku": "PROD-001",
  "name": "Nasi Goreng Spesial",
  "price": 15000,
  "stock": 50
}
```

**Response 201:**
```json
{
  "id": 1,
  "sku": "PROD-001",
  "name": "Nasi Goreng Spesial",
  "price": 15000.00,
  "stock": 50,
  "status": "ACTIVE",
  "createdAt": "2026-06-13T10:00:00",
  "updatedAt": "2026-06-13T10:00:00"
}
```

### POST /api/orders

**Request:**
```json
{
  "customerName": "Siti Rahayu",
  "customerEmail": "siti@example.com",
  "items": [
    { "productId": 1, "quantity": 2 },
    { "productId": 2, "quantity": 1 }
  ]
}
```

**Response 201:**
```json
{
  "id": 1,
  "customerName": "Siti Rahayu",
  "customerEmail": "siti@example.com",
  "status": "PENDING",
  "totalAmount": 40000.00,
  "items": [
    {
      "id": 1,
      "productId": 1,
      "productName": "Nasi Goreng Spesial",
      "unitPrice": 15000.00,
      "quantity": 2,
      "subtotal": 30000.00
    },
    {
      "id": 2,
      "productId": 2,
      "productName": "Es Teh Manis",
      "unitPrice": 5000.00,
      "quantity": 1,
      "subtotal": 5000.00
    }
  ],
  "createdAt": "2026-06-13T10:05:00",
  "updatedAt": "2026-06-13T10:05:00"
}
```

### Format Error Response

```json
{
  "timestamp": "2026-06-13T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/products",
  "errors": {
    "sku": "SKU is required",
    "price": "Price must be greater than 0"
  }
}
```

Stack trace tidak pernah dikembalikan ke client.

---

## Business Rules

### Catalog Service

- SKU harus unik — duplikat menghasilkan 409 Conflict
- Harga harus lebih dari 0
- Stok minimal 0 (tidak boleh negatif)
- Status default saat dibuat adalah `ACTIVE`
- Status hanya boleh `ACTIVE` atau `INACTIVE`
- Stok tidak bisa dikurangi melebihi stok tersedia
- Produk `INACTIVE` tidak bisa dikurangi stoknya

### Order Service

- Customer name dan email wajib diisi
- Email harus valid
- Order minimal memiliki satu item
- Quantity per item minimal 1
- Produk harus ditemukan di Catalog Service
- Produk harus berstatus `ACTIVE`
- Stok harus mencukupi quantity yang dipesan
- Harga dan nama produk diambil dari Catalog Service — bukan dari request
- Total dan subtotal dihitung oleh backend
- Order baru selalu berstatus `PENDING`
- Hanya order `PENDING` yang dapat dibayar → status menjadi `PAID`
- Hanya order `PENDING` yang dapat dibatalkan → status menjadi `CANCELLED`
- Saat order dibatalkan, stok seluruh item dikembalikan melalui Catalog Service

---

## Pemisahan Database

Setiap service memiliki database sendiri yang tidak dapat diakses service lain:

```
Catalog Service  →  catalog_db  (tabel: products)
Order Service    →  order_db    (tabel: orders, order_items, idempotency_records)
```

`order_items.product_id` adalah kolom biasa (bukan foreign key ke `catalog_db`). Data produk disimpan sebagai snapshot saat order dibuat, sehingga perubahan produk di kemudian hari tidak memengaruhi data order yang sudah ada.

---

## Komunikasi HTTP Antar Service

Order Service memanggil Catalog Service menggunakan **Spring RestClient**:

| Aksi | HTTP Call |
|---|---|
| Ambil data produk | `GET /api/products/{id}` |
| Kurangi stok | `PATCH /api/products/{id}/reduce-stock` |
| Kembalikan stok | `PATCH /api/products/{id}/restore-stock` |

Base URL Catalog Service dikonfigurasi melalui `catalog.service.base-url` di `application.properties` dan dapat di-override menggunakan environment variable `CATALOG_SERVICE_URL`.

---

## Import Postman Collection

1. Buka Postman
2. Klik **Import**
3. Pilih file `postman/campus-commerce.postman_collection.json`
4. Collection variables `catalog_base_url` dan `order_base_url` sudah dikonfigurasi otomatis
5. Jalankan request dari atas ke bawah, atau gunakan **Collection Runner**

---

## Bonus yang Dikerjakan

| Fitur | Detail |
|---|---|
| Pagination + Search/Filter | GET `/api/products` dan GET `/api/orders` mendukung paginasi, filter nama, dan filter status |
| Swagger / OpenAPI | Dokumentasi interaktif tersedia di `/swagger-ui.html` di masing-masing service |
| Logging | SLF4J + Logback — INFO pada operasi bisnis, WARN pada error katalog, ERROR pada exception tak terduga |
| Unit Test | JUnit 5 + Mockito untuk `ProductServiceImpl` (7 test) dan `OrderServiceImpl` (9 test) |
| Integration Test | MockMvc + H2 in-memory untuk `ProductController` (9 test) dan `OrderController` (13 test) |
| Docker Compose | Satu perintah `docker compose up --build` menjalankan semua service dan database |
| Idempotency | Header `Idempotency-Key` mencegah duplikasi order akibat retry |
| API Gateway | Spring Cloud Gateway pada port 8080 — routing, CORS, request logging |
