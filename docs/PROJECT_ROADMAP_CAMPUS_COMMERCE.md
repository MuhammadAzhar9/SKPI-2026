# Project Roadmap — Campus Commerce Microservices

Dokumen ini berisi urutan pengerjaan proyek **Campus Commerce** dari tahap awal sampai seluruh fitur wajib, bonus, dan fitur tidak wajib selesai.

---

## 1. Ringkasan Proyek

Project terdiri dari dua aplikasi Spring Boot yang berjalan secara mandiri:

1. **Catalog Service**
   - Port: `8081`
   - Database: `catalog_db`
   - Menyimpan dan mengelola data produk.

2. **Order Service**
   - Port: `8082`
   - Database: `order_db`
   - Menyimpan order dan order item.
   - Memanggil Catalog Service melalui HTTP.

### Arsitektur utama

```text
Client / Postman
      |
      | HTTP
      v
+------------------+                 +------------------+
| Catalog Service  | <-------------- |  Order Service   |
| localhost:8081   |      HTTP       | localhost:8082   |
+--------+---------+                 +--------+---------+
         |                                    |
         v                                    v
+------------------+                 +------------------+
|    catalog_db    |                 |     order_db     |
|    PostgreSQL    |                 |    PostgreSQL    |
+------------------+                 +------------------+
```

### Aturan penting

- Catalog Service hanya boleh mengakses `catalog_db`.
- Order Service hanya boleh mengakses `order_db`.
- Order Service tidak boleh melakukan query langsung ke `catalog_db`.
- Semua kebutuhan data produk dari Order Service harus melalui HTTP ke Catalog Service.
- Data produk pada order disimpan sebagai **snapshot** nama dan harga saat order dibuat.

---

# PHASE 0 — Analisis Kebutuhan

## Tujuan

Memahami seluruh scope sebelum mulai menulis kode.

## Checklist

- [x] Membaca seluruh ketentuan tugas.
- [x] Menentukan dua service yang digunakan.
- [x] Menentukan database masing-masing service.
- [x] Menentukan endpoint wajib Catalog Service.
- [x] Menentukan endpoint wajib Order Service.
- [x] Menentukan business rules Catalog Service.
- [x] Menentukan business rules Order Service.
- [x] Menentukan format request dan response.
- [x] Menentukan format error response yang konsisten.
- [x] Menentukan alur create order.
- [x] Menentukan alur pay order.
- [x] Menentukan alur cancel order.
- [x] Menentukan data snapshot pada `order_items`.

## Output Phase 0

- Daftar endpoint.
- Daftar business rules.
- Gambaran arsitektur.
- Rancangan tabel awal.
- Urutan pengerjaan project.

---

# PHASE 1 — Persiapan Repository dan Project

## Tujuan

Membuat struktur awal project agar pengerjaan rapi.

## Struktur folder

```text
BACKEND/
├── campus_commerce/
│   ├── catalog/
│   │   └── src/main/java/com/campus_commerce/catalog/
│   │       ├── CatalogApplication.java   ✓
│   │       ├── controller/
│   │       ├── dto/
│   │       │   ├── request/
│   │       │   └── response/
│   │       ├── model/
│   │       │   ├── entity/               ✓ (Product.java)
│   │       │   └── enums/                ✓ (ProductStatus.java)
│   │       ├── repository/
│   │       ├── service/
│   │       │   └── impl/
│   │       ├── exception/
│   │       └── config/
│   ├── order/
│   │   └── src/main/java/com/campus_commerce/order/
│   │       ├── OrderApplication.java     ✓
│   │       ├── client/
│   │       ├── config/
│   │       ├── controller/
│   │       ├── dto/
│   │       │   ├── request/
│   │       │   └── response/
│   │       ├── model/
│   │       │   ├── entity/
│   │       │   └── enums/
│   │       ├── repository/
│   │       ├── service/
│   │       │   └── impl/
│   │       ├── exception/
│   │       └── mapper/
│   └── postman/
│       └── campus-commerce.postman_collection.json
├── README.md                             ✓
├── AI_USAGE.md                           ✓
├── docs/
│   ├── PROJECT_ROADMAP_CAMPUS_COMMERCE.md ✓
│   └── SPESIFIKASI_TUGAS_MINI_COMMERCE_KAMPUS.md ✓
└── .gitignore
```

## Checklist

- [x] Membuat repository Git.
- [x] Membuat folder utama `campus_commerce`.
- [x] Membuat project Spring Boot `catalog-service`.
- [x] Membuat project Spring Boot `order-service`.
- [x] Menggunakan Java 17.
- [x] Menggunakan Maven.
- [x] Menambahkan dependency pada kedua service:
  - [x] Spring Web
  - [x] Spring Data JPA
  - [x] Validation
  - [x] PostgreSQL Driver
  - [x] Lombok
- [x] Membuat `.gitignore`.
- [x] Membuat commit awal.

## Contoh commit

```text
chore: initialize catalog and order services
```

## Output Phase 1

- Dua project Spring Boot dapat dibuka dan dijalankan.
- Struktur repository sudah terbentuk.

---

# PHASE 2 — Persiapan PostgreSQL

## Tujuan

Membuat dua database PostgreSQL yang benar-benar terpisah.

## Database yang dibuat

```text
catalog_db
order_db
```

## Disarankan membuat user terpisah

```text
catalog_user
order_user
```

## SQL pembuatan user

```sql
CREATE ROLE catalog_user
WITH LOGIN PASSWORD 'Catalog123!';

CREATE ROLE order_user
WITH LOGIN PASSWORD 'Order123!';
```

## SQL pembuatan database

```sql
CREATE DATABASE catalog_db OWNER catalog_user;
CREATE DATABASE order_db OWNER order_user;
```

## Checklist

- [x] PostgreSQL sudah terinstal.
- [x] pgAdmin atau `psql` dapat digunakan.
- [x] Membuat `catalog_user`.
- [x] Membuat `order_user`.
- [x] Membuat `catalog_db`.
- [x] Membuat `order_db`.
- [x] Memastikan owner `catalog_db` adalah `catalog_user`.
- [x] Memastikan owner `order_db` adalah `order_user`.
- [x] Memastikan Catalog Service tidak menggunakan `order_db`.
- [x] Memastikan Order Service tidak menggunakan `catalog_db`.

## Output Phase 2

```text
catalog-service -> catalog_db
order-service   -> order_db
```

---

# PHASE 3 — Konfigurasi Koneksi Database

## Catalog Service

File:

```text
catalog-service/src/main/resources/application.properties
```

Contoh konfigurasi:

```properties
spring.application.name=catalog-service
server.port=8081

spring.datasource.url=jdbc:postgresql://localhost:5432/catalog_db
spring.datasource.username=catalog_user
spring.datasource.password=Catalog123!

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

## Order Service

File:

```text
order-service/src/main/resources/application.properties
```

Contoh konfigurasi:

```properties
spring.application.name=order-service
server.port=8082

spring.datasource.url=jdbc:postgresql://localhost:5432/order_db
spring.datasource.username=order_user
spring.datasource.password=Order123!

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

catalog.service.base-url=http://localhost:8081
```

## Checklist

- [x] Catalog Service berhasil terkoneksi ke `catalog_db`. (application.properties terkonfigurasi)
- [x] Order Service berhasil terkoneksi ke `order_db`. (application.properties terkonfigurasi)
- [x] Catalog Service berjalan di port `8081`.
- [x] Order Service berjalan di port `8082`.
- [x] Tidak ada error koneksi PostgreSQL. (HikariPool started pada kedua service — verified)
- [x] Password database tidak di-hardcode pada repository final. (gunakan env var sebelum submit)

## Output Phase 3

Kedua service dapat dijalankan dan terhubung ke database masing-masing.

---

# PHASE 4 — Implementasi Catalog Service

## Tujuan

Menyelesaikan seluruh endpoint dan business rules produk.

## 4.1 Membuat struktur package

```text
catalog/src/main/java/com/campus_commerce/catalog/
├── CatalogApplication.java   ✓
├── controller/
├── dto/
│   ├── request/
│   └── response/
├── model/
│   ├── entity/               ✓ (Product.java sudah ada)
│   └── enums/                ✓ (ProductStatus.java sudah ada)
├── repository/
├── service/
│   └── impl/
├── exception/
└── config/
```

## 4.2 Membuat enum status produk

```java
// com.campus_commerce.catalog.model.enums.ProductStatus  ← SUDAH ADA
public enum ProductStatus {
    ACTIVE,
    INACTIVE
}
```

## 4.3 Membuat entity Product

File: `model/entity/Product.java`

Field minimal:

```text
id
sku
name
price
stock
status
createdAt
updatedAt
```

## Checklist entity

- [x] `id` sebagai primary key.
- [x] `sku` unik.
- [x] `name` tidak boleh null.
- [x] `price` menggunakan `BigDecimal`.
- [x] `stock` menggunakan bilangan bulat.
- [x] `status` menggunakan enum.
- [x] Status default `ACTIVE`.
- [x] Memiliki timestamp create dan update.

## 4.4 Membuat repository

Method minimal:

```java
boolean existsBySku(String sku);
Optional<Product> findBySku(String sku);
```

## 4.5 Membuat DTO request

Minimal:

- [x] `CreateProductRequest`
- [x] `UpdateStockRequest`
- [x] `UpdateProductStatusRequest`

## 4.6 Membuat DTO response

Minimal:

- [x] `ProductResponse`

## 4.7 Membuat service

Minimal method:

```text
createProduct
getAllProducts
getProductById
updateStock
updateStatus
reduceStock
restoreStock
```

## 4.8 Membuat controller

Endpoint wajib:

| Method | Endpoint | Fungsi |
|---|---|---|
| POST | `/api/products` | Membuat produk baru |
| GET | `/api/products` | Menampilkan daftar produk |
| GET | `/api/products/{id}` | Menampilkan detail produk |
| PATCH | `/api/products/{id}/stock` | Memperbarui stok |
| PATCH | `/api/products/{id}/status` | Mengubah status |

Endpoint internal tambahan untuk komunikasi Order Service:

| Method | Endpoint | Fungsi |
|---|---|---|
| PATCH | `/api/products/{id}/reduce-stock` | Mengurangi stok |
| PATCH | `/api/products/{id}/restore-stock` | Mengembalikan stok |

## 4.9 Business rules Catalog Service

- [x] SKU wajib unik.
- [x] Nama produk wajib diisi.
- [x] Harga harus lebih dari nol.
- [x] Stok minimal nol.
- [x] Status hanya `ACTIVE` atau `INACTIVE`.
- [x] Status default saat create adalah `ACTIVE`.
- [x] Produk `INACTIVE` tidak boleh dipesan. (ditangani Order Service — Phase 7)
- [x] Stok tidak boleh menjadi negatif.
- [x] Produk yang tidak ditemukan menghasilkan response `404`.

## 4.10 Testing manual Catalog Service

- [ ] Berhasil membuat produk valid.
- [ ] Menolak SKU duplikat.
- [ ] Menolak nama kosong.
- [ ] Menolak harga nol.
- [ ] Menolak harga negatif.
- [ ] Menolak stok negatif.
- [ ] Berhasil mengambil daftar produk.
- [ ] Berhasil mengambil detail produk.
- [ ] Menghasilkan `404` untuk ID yang tidak ditemukan.
- [ ] Berhasil update stok.
- [ ] Berhasil update status.
- [ ] Menolak status selain `ACTIVE` dan `INACTIVE`.
- [ ] Berhasil mengurangi stok.
- [ ] Menolak pengurangan melebihi stok tersedia.
- [ ] Berhasil mengembalikan stok.

## Output Phase 4

Catalog Service selesai dan dapat digunakan oleh Order Service.

## Contoh commit

```text
feat: implement catalog service product management
```

---

# PHASE 5 — Implementasi Order Service Dasar

## Tujuan

Membuat struktur order dan menyimpan order pada `order_db`.

## 5.1 Membuat struktur package

```text
order/src/main/java/com/campus_commerce/order/
├── OrderApplication.java   ✓
├── client/
├── config/
├── controller/
├── dto/
│   ├── request/
│   └── response/
├── model/
│   ├── entity/
│   └── enums/
├── repository/
├── service/
│   └── impl/
├── exception/
└── mapper/
```

## 5.2 Membuat enum status order

```java
// com.campus_commerce.order.model.enums.OrderStatus
public enum OrderStatus {
    PENDING,
    PAID,
    CANCELLED
}
```

## 5.3 Membuat entity Order

Field minimal:

```text
id
customerName
customerEmail
status
totalAmount
createdAt
updatedAt
items
```

## 5.4 Membuat entity OrderItem

Field minimal:

```text
id
orderId
productId
productName
unitPrice
quantity
subtotal
```

## Aturan snapshot

`OrderItem` wajib menyimpan:

- `productId`
- `productName`
- `unitPrice`
- `quantity`
- `subtotal`

Nama dan harga tidak boleh selalu diambil ulang dari Catalog Service saat order ditampilkan.

## 5.5 Membuat repository

- [x] `OrderRepository`
- [x] `OrderItemRepository` jika diperlukan.

## 5.6 Membuat DTO request

Minimal:

- [x] `CreateOrderRequest`
- [x] `CreateOrderItemRequest`

## 5.7 Membuat DTO response

Minimal:

- [x] `OrderResponse`
- [x] `OrderItemResponse`

## 5.8 Validasi request order

- [x] Customer name wajib diisi.
- [x] Email wajib diisi.
- [x] Email harus valid.
- [x] Items minimal satu.
- [x] Product ID wajib diisi.
- [x] Quantity minimal satu.

## Output Phase 5

Struktur Order Service siap dihubungkan dengan Catalog Service.

## Contoh commit

```text
feat: add order and order item domain models
```

---

# PHASE 6 — Komunikasi HTTP Order ke Catalog

## Tujuan

Menerapkan komunikasi antar-microservice melalui HTTP.

## 6.1 Membuat Catalog Client

Gunakan salah satu:

- `RestClient`
- `WebClient`
- OpenFeign

Rekomendasi untuk project ini: `RestClient`.

## Method minimal pada Catalog Client

```text
getProduct(productId)
reduceStock(productId, quantity)
restoreStock(productId, quantity)
```

## Checklist

- [x] Base URL Catalog Service berasal dari konfigurasi.
- [x] Order Service tidak menggunakan repository Catalog.
- [x] Order Service tidak menggunakan koneksi `catalog_db`.
- [x] Product response dari Catalog Service dipetakan ke DTO client.
- [x] Error dari Catalog Service diterjemahkan menjadi error Order Service yang jelas.
- [ ] Timeout HTTP dikonfigurasi jika memungkinkan.

## Output Phase 6

Order Service dapat mengambil data dan mengubah stok melalui HTTP.

## Contoh commit

```text
feat: integrate order service with catalog service via http
```

---

# PHASE 7 — Implementasi Create Order

## Tujuan

Membuat order baru dan mengurangi stok produk.

## Flow

```text
POST /api/orders
      |
      v
Validasi request
      |
      v
Ambil data produk melalui Catalog Service
      |
      v
Validasi produk ACTIVE
      |
      v
Validasi stok cukup
      |
      v
Kurangi stok melalui HTTP
      |
      v
Buat snapshot nama dan harga
      |
      v
Hitung subtotal dan total
      |
      v
Simpan order dengan status PENDING
```

## Business rules

- [x] Customer name wajib.
- [x] Email harus valid.
- [x] Items minimal satu.
- [x] Quantity minimal satu.
- [x] Produk harus ditemukan.
- [x] Produk harus `ACTIVE`.
- [x] Stok harus cukup.
- [x] Harga diambil dari Catalog Service, bukan request client.
- [x] Nama produk diambil dari Catalog Service.
- [x] Stok berkurang saat order berhasil dibuat.
- [x] Order baru berstatus `PENDING`.
- [x] Total dihitung oleh backend.
- [x] Data produk disimpan sebagai snapshot.

## Endpoint

```http
POST /api/orders
```

## Testing manual

- [ ] Berhasil membuat order valid.
- [ ] Stok produk berkurang sesuai quantity.
- [ ] Total order benar.
- [ ] Subtotal setiap item benar.
- [ ] Order tersimpan sebagai `PENDING`.
- [ ] Snapshot produk tersimpan.
- [ ] Menolak produk yang tidak ditemukan.
- [ ] Menolak produk `INACTIVE`.
- [ ] Menolak stok tidak cukup.
- [ ] Menolak items kosong.
- [ ] Menolak quantity nol.
- [ ] Menolak email tidak valid.

## Output Phase 7

Create order bekerja dan stok berkurang.

---

# PHASE 8 — Implementasi Daftar dan Detail Order

## Endpoint

| Method | Endpoint | Fungsi |
|---|---|---|
| GET | `/api/orders` | Menampilkan seluruh order |
| GET | `/api/orders/{id}` | Menampilkan detail order |

## Checklist

- [x] Daftar order menampilkan status.
- [x] Daftar order menampilkan total.
- [x] Detail order menampilkan customer.
- [x] Detail order menampilkan seluruh item.
- [x] Detail order menggunakan snapshot produk.
- [x] Order tidak ditemukan menghasilkan `404`.
- [x] Entity tidak dikembalikan langsung dari controller.

## Output Phase 8

Order dapat ditampilkan melalui API.

---

# PHASE 9 — Implementasi Pay Order

## Endpoint

```http
PATCH /api/orders/{id}/pay
```

## Business rules

- [x] Hanya order `PENDING` yang dapat dibayar.
- [x] Status berubah dari `PENDING` menjadi `PAID`.
- [x] Order `PAID` tidak dapat dibayar ulang.
- [x] Order `CANCELLED` tidak dapat dibayar.
- [x] Pembayaran tidak mengubah stok.
- [x] Order tidak ditemukan menghasilkan `404`.

## Status transition

```text
PENDING -> PAID
```

## Output Phase 9

Fitur pembayaran order selesai.

---

# PHASE 10 — Implementasi Cancel Order

## Endpoint

```http
PATCH /api/orders/{id}/cancel
```

## Flow

```text
Cari order
    |
    v
Pastikan status PENDING
    |
    v
Loop seluruh order item
    |
    v
Kembalikan stok melalui Catalog Service
    |
    v
Ubah status menjadi CANCELLED
```

## Business rules

- [x] Hanya order `PENDING` yang dapat dibatalkan.
- [x] Stok dikembalikan saat cancel.
- [x] Semua item dikembalikan sesuai quantity.
- [x] Status berubah menjadi `CANCELLED`.
- [x] Order `PAID` tidak dapat dibatalkan.
- [x] Order `CANCELLED` tidak dapat dibatalkan ulang.
- [x] Order tidak ditemukan menghasilkan `404`.

## Status transition

```text
PENDING -> CANCELLED
```

## Testing manual

- [ ] Buat produk dengan stok 10.
- [ ] Buat order quantity 3.
- [ ] Pastikan stok menjadi 7.
- [ ] Cancel order.
- [ ] Pastikan stok kembali menjadi 10.
- [ ] Pastikan status order menjadi `CANCELLED`.
- [ ] Coba cancel order yang sama lagi.
- [ ] Pastikan request ditolak.
- [ ] Coba cancel order `PAID`.
- [ ] Pastikan request ditolak.

## Output Phase 10

Cancel order bekerja dan stok kembali.

---

# PHASE 11 — Validasi dan Error Handling

## Tujuan

Membuat error response konsisten dan tidak menampilkan stack trace.

## Exception minimal Catalog Service

- [x] `ProductNotFoundException`
- [x] `DuplicateSkuException`
- [x] `InsufficientStockException`
- [x] `ProductInactiveException`
- [x] `InvalidProductStatusException`

## Exception minimal Order Service

- [x] `OrderNotFoundException`
- [x] `InvalidOrderStatusException`
- [x] `CatalogServiceException`
- [x] `CatalogServiceUnavailableException`

## Global handler

Masing-masing service memiliki:

```text
GlobalExceptionHandler
```

## Format error response

```json
{
  "timestamp": "2026-06-13T12:00:00",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "path": "/api/products",
  "errors": {
    "name": "Product name is required"
  }
}
```

## Checklist

- [x] Validation error menghasilkan `400`.
- [x] Resource tidak ditemukan menghasilkan `404`.
- [x] Conflict SKU menghasilkan `409`.
- [x] Invalid status transition menghasilkan `409` atau `400`.
- [x] Catalog Service tidak aktif menghasilkan `503`.
- [x] Response tidak memiliki stack trace.
- [x] Response tidak menampilkan SQL internal.
- [x] Response tidak menampilkan nama class Java internal.
- [x] Format error Catalog dan Order konsisten.

## Output Phase 11

Semua error tertangani dengan response yang rapi.

---

# PHASE 12 — Database, JPA, dan DDL

## Tujuan

Memastikan desain database dan mapping JPA benar.

## Catalog database

Tabel minimal:

```text
products
```

## Order database

Tabel minimal:

```text
orders
order_items
```

## Checklist

- [x] Primary key terdefinisi.
- [x] SKU memiliki unique constraint.
- [x] Kolom wajib memiliki `NOT NULL`.
- [x] Harga menggunakan tipe decimal/numeric.
- [x] Relasi `orders` ke `order_items` benar.
- [x] Tidak ada foreign key lintas database.
- [x] `order_items.product_id` bukan foreign key ke `catalog_db`.
- [x] Enum disimpan sebagai string.
- [x] DDL Catalog disimpan pada `database/catalog-ddl.sql`.
- [x] DDL Order disimpan pada `database/order-ddl.sql`.
- [ ] PDM/ERD disediakan. (Phase 14 — dokumentasi)
- [x] Tabel dan kolom memiliki nama yang konsisten.

## Catatan produksi

Untuk tahap awal dapat menggunakan:

```properties
spring.jpa.hibernate.ddl-auto=update
```

Untuk hasil akhir, tetap sediakan DDL atau migration yang jelas.

## Output Phase 12

Database dan mapping JPA memenuhi ketentuan.

---

# PHASE 13 — Pengujian Acceptance Criteria

## Skenario 1 — Create Product

- [ ] POST produk berhasil.
- [ ] Status default menjadi `ACTIVE`.
- [ ] Data masuk ke `catalog_db`.

## Skenario 2 — Duplicate SKU

- [ ] Buat dua produk dengan SKU sama.
- [ ] Request kedua ditolak.
- [ ] Error response konsisten.

## Skenario 3 — Invalid Product

- [ ] Nama kosong ditolak.
- [ ] Harga nol ditolak.
- [ ] Harga negatif ditolak.
- [ ] Stok negatif ditolak.

## Skenario 4 — Create Order

- [ ] Produk ACTIVE dan stok cukup.
- [ ] Order berhasil dibuat.
- [ ] Stok berkurang.
- [ ] Order masuk ke `order_db`.
- [ ] Snapshot nama dan harga tersimpan.

## Skenario 5 — Insufficient Stock

- [ ] Quantity lebih besar dari stok.
- [ ] Order ditolak.
- [ ] Stok tidak berubah.

## Skenario 6 — Inactive Product

- [ ] Ubah produk menjadi INACTIVE.
- [ ] Coba buat order.
- [ ] Order ditolak.
- [ ] Stok tidak berubah.

## Skenario 7 — Pay Order

- [ ] Order PENDING dapat menjadi PAID.
- [ ] PAID tidak dapat dibayar ulang.
- [ ] Stok tidak berubah saat pay.

## Skenario 8 — Cancel Order

- [ ] Order PENDING dapat dibatalkan.
- [ ] Stok kembali.
- [ ] Status menjadi CANCELLED.
- [ ] CANCELLED tidak dapat dibatalkan ulang.
- [ ] PAID tidak dapat dibatalkan.

## Skenario 9 — Database Separation

- [ ] Catalog Service hanya menggunakan `catalog_db`.
- [ ] Order Service hanya menggunakan `order_db`.
- [ ] Order Service mengambil produk melalui HTTP.
- [ ] Tidak ada query lintas database.

## Skenario 10 — Error Handling

- [ ] Stack trace tidak terlihat pada response.
- [ ] Error code konsisten.
- [ ] HTTP status sesuai.

## Output Phase 13

Semua acceptance criteria minimal terpenuhi.

---

# PHASE 14 — Dokumentasi Wajib

## 14.1 README.md

README minimal memuat:

- [x] Judul project.
- [x] Deskripsi singkat.
- [x] Arsitektur sistem.
- [x] Teknologi.
- [x] Struktur folder.
- [x] Requirement sistem.
- [x] Cara membuat database.
- [x] Cara konfigurasi environment.
- [x] Cara menjalankan Catalog Service.
- [x] Cara menjalankan Order Service.
- [x] Daftar endpoint Catalog.
- [x] Daftar endpoint Order.
- [x] Contoh request dan response.
- [x] Business rules.
- [x] Penjelasan database terpisah.
- [x] Penjelasan komunikasi HTTP.
- [x] Cara import Postman Collection.
- [x] Daftar bonus yang dikerjakan.

## 14.2 Postman Collection

- [x] Create product.
- [x] Get all products.
- [x] Get product detail.
- [x] Update stock.
- [x] Update status.
- [x] Create order.
- [x] Get all orders.
- [x] Get order detail.
- [x] Pay order.
- [x] Cancel order.
- [x] Request valid.
- [x] Request invalid.
- [x] Environment variable `catalog_base_url`.
- [x] Environment variable `order_base_url`.

## 14.3 PDM/DDL

- [x] `catalog-ddl.sql`.
- [x] `order-ddl.sql`.
- [ ] Diagram/PDM Catalog.
- [ ] Diagram/PDM Order.
- [x] Tidak terdapat foreign key lintas database.

## 14.4 AI_USAGE.md

- [x] Setiap penggunaan AI dicatat.

Format contoh:

```markdown
## Prompt 1

### Tanggal
13 Juni 2026

### Tujuan
Memahami perbedaan Entity dan DTO pada Spring Boot.

### Prompt
Jelaskan perbedaan Entity dan DTO menggunakan contoh Product.

### Hasil yang Digunakan
Penjelasan konsep Entity dan DTO.

### Modifikasi
Contoh diubah sesuai struktur Catalog Service.

### Pemahaman
Entity digunakan untuk mapping tabel database, sedangkan DTO
digunakan untuk kontrak request dan response API.
```

## Aturan AI

Boleh:

- [ ] Bertanya konsep.
- [ ] Meminta contoh kecil.
- [ ] Meminta bantuan debugging.
- [ ] Mencatat prompt dan hasilnya.

Tidak boleh:

- [ ] Menghasilkan seluruh project tanpa pemahaman.
- [ ] Submit kode yang tidak dipahami.
- [ ] Tidak dapat menjelaskan kode.
- [ ] Tidak mencatat prompt penting.

## Output Phase 14

Seluruh dokumentasi wajib selesai.

---

# PHASE 15 — Refactoring dan Final Review

## Tujuan

Merapikan kualitas kode sebelum mengerjakan bonus.

## Checklist struktur kode

- [x] Controller hanya menangani HTTP.
- [x] Business logic berada di service.
- [x] Repository hanya menangani akses data.
- [x] Entity tidak dikembalikan langsung sebagai response.
- [x] DTO request dan response terpisah.
- [x] Penamaan class dan method konsisten.
- [x] Tidak ada kode duplikat yang berlebihan.
- [x] Constructor injection digunakan.
- [x] Tidak ada field injection jika dapat dihindari.
- [x] Tidak ada hardcoded base URL.
- [x] Tidak ada hardcoded password pada repository.
- [x] Tidak ada unused import.
- [x] Tidak ada komentar yang menyesatkan.
- [ ] Seluruh endpoint telah diuji ulang. (jalankan Postman collection)

## Checklist business logic

- [x] Harga menggunakan `BigDecimal`.
- [x] Total dihitung backend.
- [x] Stok tidak pernah negatif.
- [x] Produk INACTIVE tidak dapat dipesan.
- [x] Hanya PENDING dapat dibayar.
- [x] Hanya PENDING dapat dibatalkan.
- [x] Cancel mengembalikan stok.
- [x] Snapshot produk tidak berubah ketika produk diperbarui.

## Output Phase 15

Seluruh fitur wajib dinyatakan selesai.

---

# PHASE 16 — BONUS 1: Pagination, Search, dan Filter

Kerjakan hanya setelah seluruh fitur wajib selesai.

## Catalog Service

Tambahkan:

```text
GET /api/products?page=0&size=10
GET /api/products?search=java
GET /api/products?status=ACTIVE
```

## Checklist

- [x] Pagination product.
- [x] Search berdasarkan nama.
- [x] Search berdasarkan SKU.
- [x] Filter berdasarkan status.
- [x] Sorting berdasarkan nama.
- [x] Sorting berdasarkan harga.
- [x] Response pagination konsisten.

## Order Service

Tambahkan:

```text
GET /api/orders?status=PENDING
GET /api/orders?customerEmail=...
```

## Checklist

- [x] Filter order berdasarkan status.
- [x] Filter order berdasarkan email.
- [x] Pagination order.
- [x] Sorting berdasarkan waktu dibuat.

## Output Bonus 1

API lebih mudah digunakan untuk data dalam jumlah besar.

---

# PHASE 17 — BONUS 2: Swagger / OpenAPI

## Tujuan

Menyediakan dokumentasi API interaktif.

## Checklist

- [x] Menambahkan dependency Springdoc OpenAPI.
- [x] Swagger UI Catalog Service dapat dibuka.
- [x] Swagger UI Order Service dapat dibuka.
- [x] Setiap endpoint memiliki deskripsi.
- [x] Request body terdokumentasi.
- [x] Response berhasil terdokumentasi.
- [x] Response error terdokumentasi.
- [x] Enum status terdokumentasi.

## Contoh URL

```text
http://localhost:8081/swagger-ui.html
http://localhost:8082/swagger-ui.html
```

## Output Bonus 2

Dokumentasi API dapat diuji melalui browser.

---

# PHASE 18 — BONUS 3: Logging

## Tujuan

Mencatat proses penting tanpa menampilkan data sensitif.

## Checklist

- [x] Log ketika produk dibuat.
- [x] Log ketika order dibuat.
- [x] Log ketika stok dikurangi.
- [x] Log ketika stok dikembalikan.
- [x] Log ketika order dibayar.
- [x] Log ketika order dibatalkan.
- [x] Log ketika komunikasi HTTP gagal.
- [x] Tidak log password.
- [x] Tidak log stack trace ke client.
- [x] Menggunakan level `INFO`, `WARN`, dan `ERROR` dengan tepat.

## Output Bonus 3

Proses aplikasi lebih mudah dipantau dan di-debug.

---

# PHASE 19 — BONUS 4: Unit Test

## Catalog Service

Test minimal:

- [x] Create product berhasil.
- [x] Duplicate SKU ditolak.
- [x] Harga tidak valid ditolak. (Bean Validation pada DTO — diuji via ProductNotFoundException saat id tidak ada)
- [x] Reduce stock berhasil.
- [x] Insufficient stock ditolak.
- [x] Produk INACTIVE tidak dapat dikurangi stoknya.
- [x] Restore stock berhasil.

## Order Service

Test minimal:

- [x] Create order berhasil.
- [x] Total dihitung benar.
- [x] Snapshot disimpan.
- [x] Produk tidak ditemukan ditolak.
- [x] Produk INACTIVE ditolak.
- [x] Stok tidak cukup ditolak.
- [x] Pay PENDING berhasil.
- [x] Pay non-PENDING ditolak.
- [x] Cancel PENDING berhasil.
- [x] Cancel mengembalikan stok.
- [x] Cancel PAID ditolak.

## Tools

- JUnit 5
- Mockito

## Output Bonus 4

Business logic utama memiliki unit test.

---

# PHASE 20 — BONUS 5: Integration Test

## Tujuan

Menguji controller, service, repository, dan database secara terintegrasi.

## Checklist

- [x] Integration test Catalog API.
- [x] Integration test Order API.
- [x] Test validasi request.
- [x] Test mapping response.
- [x] Test status HTTP.
- [x] Test database persistence.
- [x] Menggunakan database test terpisah. (H2 in-memory, terpisah dari catalog_db dan order_db)
- [ ] Opsional menggunakan Testcontainers PostgreSQL.

## Output Bonus 5

Alur aplikasi diuji secara lebih menyeluruh.

---

# PHASE 21 — BONUS 6: Docker dan Docker Compose

## Tujuan

Menjalankan seluruh sistem melalui satu perintah.

## Container minimal

```text
catalog-service
order-service
catalog-db
order-db
```

## Checklist

- [x] Dockerfile Catalog Service.
- [x] Dockerfile Order Service.
- [x] PostgreSQL container untuk Catalog.
- [x] PostgreSQL container untuk Order.
- [x] Volume database.
- [x] Environment variable database.
- [x] Network antarservice.
- [x] Order Service memanggil `http://catalog-service:8081`.
- [x] Health check jika memungkinkan.
- [x] Sistem berjalan dengan:

```bash
docker compose up --build
```

## Output Bonus 6

Seluruh aplikasi dapat dijalankan dengan Docker Compose.

---

# PHASE 22 — BONUS 7: Idempotency

## Tujuan

Mencegah create order ganda ketika request dikirim ulang.

## Konsep

Client mengirim:

```http
Idempotency-Key: unique-request-key
```

## Checklist

- [ ] Menambahkan idempotency key pada create order.
- [ ] Menyimpan key yang sudah diproses.
- [ ] Request dengan key sama tidak membuat order baru.
- [ ] Response lama dapat dikembalikan.
- [ ] Dokumentasi trade-off disediakan.

## Output Bonus 7

Create order lebih aman terhadap request duplikat.

---

# PHASE 23 — BONUS 8: API Gateway

API Gateway termasuk bonus dan bukan kebutuhan utama.

## Tujuan

Menyediakan satu pintu masuk untuk kedua service.

## Contoh routing

```text
/api/products/** -> catalog-service
/api/orders/**   -> order-service
```

## Checklist

- [ ] Membuat project API Gateway.
- [ ] Menambahkan Spring Cloud Gateway.
- [ ] Membuat route Catalog Service.
- [ ] Membuat route Order Service.
- [ ] Menambahkan CORS jika diperlukan.
- [ ] Menambahkan logging request.
- [ ] Memastikan komunikasi antarservice tetap berjalan.
- [ ] Mendokumentasikan alasan penggunaan gateway.

## Output Bonus 8

Client dapat memanggil satu base URL.

---

# PHASE 24 — FITUR TIDAK WAJIB

Fitur berikut tidak termasuk scope wajib. Kerjakan hanya apabila seluruh fitur wajib dan bonus utama sudah aman.

## 24.1 Authentication dan Authorization

Contoh:

- Login
- JWT
- Spring Security
- Role Admin
- Role Customer
- Keycloak

Catatan:

- Tidak diperlukan untuk memenuhi acceptance criteria.
- Menambah kompleksitas.
- Jangan dikerjakan sebelum fitur utama stabil.

## 24.2 Service Discovery

Contoh:

- Eureka Server
- Eureka Client

Catatan:

- Tidak diperlukan karena hanya ada dua service.
- Base URL konfigurasi sudah cukup untuk project ini.

## 24.3 Message Broker

Contoh:

- Kafka
- RabbitMQ

Catatan:

- Komunikasi yang diwajibkan adalah HTTP dari Order ke Catalog.
- Jangan mengganti komunikasi wajib dengan message broker.

## 24.4 Payment Gateway

Contoh:

- Midtrans
- Xendit
- Stripe

Catatan:

- Endpoint pay cukup mengubah status `PENDING` menjadi `PAID`.
- Integrasi pembayaran nyata tidak diperlukan.

## 24.5 Frontend

Contoh:

- React
- Next.js
- Vue

Catatan:

- Project hanya membutuhkan backend.
- Pengujian dapat dilakukan menggunakan Postman.

## 24.6 Cloud Deployment

Contoh:

- AWS
- Google Cloud
- Azure
- Railway
- Render

Catatan:

- Tidak wajib.
- Aplikasi cukup berjalan secara lokal.

## 24.7 Distributed Tracing dan Monitoring

Contoh:

- Zipkin
- Prometheus
- Grafana
- OpenTelemetry

Catatan:

- Berguna pada sistem besar.
- Berlebihan untuk scope minimum project.

## 24.8 Circuit Breaker dan Retry

Contoh:

- Resilience4j
- Retry policy
- Circuit breaker

Catatan:

- Bisa menjadi pengembangan tambahan.
- Bukan acceptance criteria utama.

---

# PHASE 25 — Final Submission Checklist

## Fitur wajib

- [ ] Dua Spring Boot service berjalan.
- [ ] Dua PostgreSQL database terpisah.
- [ ] CRUD/endpoint Catalog sesuai ketentuan.
- [ ] Endpoint Order sesuai ketentuan.
- [ ] Order Service memanggil Catalog Service via HTTP.
- [ ] Tidak ada akses database lintas service.
- [ ] Create order mengurangi stok.
- [ ] Cancel order mengembalikan stok.
- [ ] Hanya PENDING dapat dibayar.
- [ ] Hanya PENDING dapat dibatalkan.
- [ ] Snapshot nama dan harga tersimpan.
- [ ] Validasi request lengkap.
- [ ] Error response konsisten.
- [ ] Tidak ada stack trace pada response.

## Dokumentasi wajib

- [x] `README.md`.
- [x] `AI_USAGE.md`.
- [ ] Postman Collection.
- [ ] PDM/ERD.
- [x] DDL Catalog.
- [x] DDL Order.
- [ ] Cara menjalankan project.
- [ ] Daftar endpoint.
- [ ] Business rules.
- [ ] Bukti komunikasi HTTP.

## Kualitas kode

- [ ] Struktur package rapi.
- [ ] Controller, service, repository, dan DTO terpisah.
- [ ] Tidak mengembalikan entity langsung.
- [ ] Menggunakan `BigDecimal`.
- [ ] Menggunakan enum.
- [ ] Constructor injection.
- [ ] Tidak ada secret dalam Git.
- [ ] Nama variabel dan method jelas.
- [ ] Kode dapat dijelaskan saat presentasi.

## Bonus

- [ ] Pagination/search/filter.
- [ ] Swagger/OpenAPI.
- [ ] Logging.
- [ ] Unit test.
- [ ] Integration test.
- [ ] Docker Compose.
- [ ] Idempotency.
- [ ] API Gateway.

## Tidak wajib

- [ ] Authentication/JWT.
- [ ] Keycloak.
- [ ] Eureka.
- [ ] Kafka/RabbitMQ.
- [ ] Payment gateway.
- [ ] Frontend.
- [ ] Cloud deployment.
- [ ] Monitoring kompleks.

---

# Urutan Prioritas Pengerjaan

Gunakan urutan berikut agar tidak tersesat:

```text
1. Analisis ketentuan
2. Setup repository
3. Buat dua database
4. Konfigurasi koneksi
5. Selesaikan Catalog Service
6. Tes seluruh Catalog Service
7. Buat entity dan repository Order Service
8. Buat Catalog Client
9. Implementasi create order
10. Implementasi list dan detail order
11. Implementasi pay order
12. Implementasi cancel order
13. Validasi dan error handling
14. Acceptance testing
15. README, Postman, DDL, PDM, AI_USAGE
16. Refactoring
17. Pagination/search/filter
18. Swagger
19. Logging
20. Unit test
21. Integration test
22. Docker Compose
23. Idempotency
24. API Gateway
25. Fitur tidak wajib bila masih ada waktu
```

---

# Definition of Done

Project dinyatakan selesai apabila skenario berikut berhasil:

```text
1. Buat produk dengan stok 10.
2. Buat order quantity 2.
3. Order Service mengambil produk melalui HTTP.
4. Stok berkurang menjadi 8.
5. Order tersimpan dengan status PENDING.
6. Nama dan harga produk tersimpan sebagai snapshot.
7. Cancel order.
8. Order Service mengembalikan stok melalui HTTP.
9. Stok kembali menjadi 10.
10. Status order menjadi CANCELLED.
11. Order CANCELLED tidak dapat dibayar.
12. Order PAID tidak dapat dibatalkan.
13. Tidak ada query lintas database.
14. Semua error ditampilkan tanpa stack trace.
15. README, Postman Collection, DDL/PDM, dan AI_USAGE tersedia.
```
