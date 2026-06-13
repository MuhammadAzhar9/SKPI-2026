# PHASE 0 — Analisis Kebutuhan

Dokumen ini merupakan output Phase 0 berdasarkan pembacaan spesifikasi tugas Mini Commerce Kampus.

---

## 1. Dua Service yang Digunakan

| Service | Port | Database |
|---|---|---|
| Catalog Service | `8081` | `catalog_db` |
| Order Service | `8082` | `order_db` |

**Aturan keras:**
- Catalog Service hanya boleh mengakses `catalog_db`.
- Order Service hanya boleh mengakses `order_db`.
- Order Service tidak boleh query langsung ke `catalog_db`.
- Seluruh pertukaran data dilakukan via HTTP REST API.

---

## 2. Arsitektur Sistem

```text
Client / Postman
      |
      | HTTP
      v
+--------------------+                 +--------------------+
|  Catalog Service   | <-------------- |   Order Service    |
|  localhost:8081    |   HTTP REST     |   localhost:8082   |
+--------+-----------+                 +--------+-----------+
         |                                      |
         v                                      v
+--------------------+                 +--------------------+
|    catalog_db      |                 |     order_db       |
|    PostgreSQL      |                 |    PostgreSQL      |
+--------------------+                 +--------------------+
```

---

## 3. Daftar Endpoint

### 3.1 Catalog Service

| Method | Endpoint | Fungsi | Status Code |
|---|---|---|---|
| `POST` | `/api/products` | Membuat produk baru | `201 Created` |
| `GET` | `/api/products` | Menampilkan seluruh produk | `200 OK` |
| `GET` | `/api/products/{id}` | Menampilkan detail produk | `200 OK` |
| `PATCH` | `/api/products/{id}/stock` | Mengubah stok produk (manual) | `200 OK` |
| `PATCH` | `/api/products/{id}/status` | Mengubah status produk | `200 OK` |

**Endpoint internal (dipanggil Order Service):**

| Method | Endpoint | Fungsi |
|---|---|---|
| `PATCH` | `/api/products/{id}/reduce-stock` | Mengurangi stok saat order dibuat |
| `PATCH` | `/api/products/{id}/restore-stock` | Mengembalikan stok saat order dibatalkan |

### 3.2 Order Service

| Method | Endpoint | Fungsi | Status Code |
|---|---|---|---|
| `POST` | `/api/orders` | Membuat order baru | `201 Created` |
| `GET` | `/api/orders` | Menampilkan seluruh order | `200 OK` |
| `GET` | `/api/orders/{id}` | Menampilkan detail order | `200 OK` |
| `PATCH` | `/api/orders/{id}/pay` | Membayar order | `200 OK` |
| `PATCH` | `/api/orders/{id}/cancel` | Membatalkan order | `200 OK` |

---

## 4. Rancangan Tabel

### 4.1 Catalog DB — Tabel `products`

| Kolom | Tipe | Constraint |
|---|---|---|
| `id` | `BIGSERIAL` | PRIMARY KEY |
| `sku` | `VARCHAR(100)` | NOT NULL, UNIQUE |
| `name` | `VARCHAR(255)` | NOT NULL |
| `price` | `NUMERIC(15,2)` | NOT NULL, > 0 |
| `stock` | `INTEGER` | NOT NULL, >= 0 |
| `status` | `VARCHAR(20)` | NOT NULL, DEFAULT 'ACTIVE' |
| `created_at` | `TIMESTAMP` | NOT NULL |
| `updated_at` | `TIMESTAMP` | NOT NULL |

### 4.2 Order DB — Tabel `orders`

| Kolom | Tipe | Constraint |
|---|---|---|
| `id` | `BIGSERIAL` | PRIMARY KEY |
| `order_number` | `VARCHAR(100)` | NOT NULL, UNIQUE |
| `customer_name` | `VARCHAR(255)` | NOT NULL |
| `customer_email` | `VARCHAR(255)` | NOT NULL |
| `status` | `VARCHAR(20)` | NOT NULL, DEFAULT 'PENDING' |
| `total_amount` | `NUMERIC(15,2)` | NOT NULL |
| `created_at` | `TIMESTAMP` | NOT NULL |
| `updated_at` | `TIMESTAMP` | NOT NULL |

### 4.3 Order DB — Tabel `order_items`

| Kolom | Tipe | Constraint |
|---|---|---|
| `id` | `BIGSERIAL` | PRIMARY KEY |
| `order_id` | `BIGINT` | NOT NULL, FK → orders.id |
| `product_id` | `BIGINT` | NOT NULL (bukan FK ke catalog_db) |
| `product_name` | `VARCHAR(255)` | NOT NULL (snapshot) |
| `product_price` | `NUMERIC(15,2)` | NOT NULL (snapshot) |
| `quantity` | `INTEGER` | NOT NULL, > 0 |
| `subtotal` | `NUMERIC(15,2)` | NOT NULL |

> `product_id` tidak dijadikan foreign key karena lintas database tidak diperbolehkan. Integritas dijaga di application layer via HTTP ke Catalog Service.

---

## 5. Business Rules

### 5.1 Catalog Service

| # | Rule |
|---|---|
| 1 | SKU wajib diisi |
| 2 | SKU harus unik — duplikat ditolak `409 Conflict` |
| 3 | Nama produk wajib diisi |
| 4 | Harga harus lebih dari `0` |
| 5 | Stok tidak boleh kurang dari `0` |
| 6 | Status hanya boleh `ACTIVE` atau `INACTIVE` |
| 7 | Status default produk baru adalah `ACTIVE` |
| 8 | Produk `INACTIVE` tidak boleh dipesan |
| 9 | Pengurangan stok tidak boleh menyebabkan stok negatif |

### 5.2 Order Service

| # | Rule |
|---|---|
| 1 | `customerName` wajib diisi |
| 2 | `customerEmail` wajib berformat email valid |
| 3 | Order wajib memiliki minimal satu item |
| 4 | `quantity` setiap item minimal `1` |
| 5 | `productId` wajib diisi |
| 6 | Produk harus ditemukan di Catalog Service |
| 7 | Produk harus berstatus `ACTIVE` |
| 8 | Stok produk harus mencukupi |
| 9 | Total harga dihitung oleh backend — client tidak boleh menentukan sendiri |
| 10 | Nama dan harga produk disimpan sebagai snapshot saat order dibuat |
| 11 | Status awal order adalah `PENDING` |
| 12 | Hanya order `PENDING` yang dapat dibayar |
| 13 | Hanya order `PENDING` yang dapat dibatalkan |
| 14 | Order `PAID` tidak dapat dibayar ulang atau dibatalkan |
| 15 | Order `CANCELLED` tidak dapat dibayar atau dibatalkan ulang |
| 16 | Stok berkurang saat order dibuat |
| 17 | Stok dikembalikan saat order dibatalkan |

---

## 6. Status Transition Order

```text
              PATCH /pay
         +-------------------+
         |                   v
     PENDING --------------> PAID
         |
         | PATCH /cancel
         v
     CANCELLED
```

| Status Saat Ini | Aksi | Hasil |
|---|---|---|
| `PENDING` | Pay | `PAID` — berhasil |
| `PENDING` | Cancel | `CANCELLED` — berhasil, stok dikembalikan |
| `PAID` | Pay | `400 Bad Request` |
| `PAID` | Cancel | `400 Bad Request` |
| `CANCELLED` | Pay | `400 Bad Request` |
| `CANCELLED` | Cancel | `400 Bad Request` |

---

## 7. Format Request dan Response

### 7.1 Create Product — Request

```json
{
  "sku": "PROD-001",
  "name": "Nasi Goreng Spesial",
  "price": 15000,
  "stock": 50
}
```

### 7.2 Product — Response

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

### 7.3 Update Stock — Request

```json
{
  "quantity": 5,
  "operation": "DECREASE"
}
```

```json
{
  "quantity": 5,
  "operation": "INCREASE"
}
```

### 7.4 Update Status — Request

```json
{
  "status": "INACTIVE"
}
```

### 7.5 Create Order — Request

```json
{
  "customerName": "Budi Santoso",
  "customerEmail": "budi@example.com",
  "items": [
    {
      "productId": 1,
      "quantity": 2
    },
    {
      "productId": 3,
      "quantity": 1
    }
  ]
}
```

### 7.6 Order — Response

```json
{
  "id": 1,
  "orderNumber": "ORD-20260613-0001",
  "customerName": "Budi Santoso",
  "customerEmail": "budi@example.com",
  "status": "PENDING",
  "totalAmount": 45000.00,
  "createdAt": "2026-06-13T10:30:00",
  "updatedAt": "2026-06-13T10:30:00",
  "items": [
    {
      "id": 1,
      "productId": 1,
      "productName": "Nasi Goreng Spesial",
      "productPrice": 15000.00,
      "quantity": 2,
      "subtotal": 30000.00
    },
    {
      "id": 2,
      "productId": 3,
      "productName": "Es Teh Manis",
      "productPrice": 15000.00,
      "quantity": 1,
      "subtotal": 15000.00
    }
  ]
}
```

---

## 8. Format Error Response

```json
{
  "timestamp": "2026-06-13T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Stock is not sufficient",
  "path": "/api/orders"
}
```

Format untuk validation error (multiple field):

```json
{
  "timestamp": "2026-06-13T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/products",
  "errors": {
    "name": "must not be blank",
    "price": "must be greater than 0"
  }
}
```

### HTTP Status Code yang Digunakan

| Kondisi | Status |
|---|---|
| Data berhasil dibuat | `201 Created` |
| Request berhasil | `200 OK` |
| Validasi request gagal | `400 Bad Request` |
| Status order tidak valid | `400 Bad Request` |
| Resource tidak ditemukan | `404 Not Found` |
| SKU duplikat | `409 Conflict` |
| Catalog Service tidak aktif | `502 Bad Gateway` |
| Kesalahan internal | `500 Internal Server Error` |

---

## 9. Alur Create Order

```text
POST /api/orders
      |
      v
[1] Controller menerima dan validasi format request
      |
      v
[2] Pastikan customerName, email valid, items tidak kosong, quantity >= 1
      |
      v
[3] Untuk setiap item → GET /api/products/{id} ke Catalog Service
      |
      v
[4] Validasi: produk ditemukan + status ACTIVE + stok cukup
      |
      v
[5] Simpan snapshot: productName, productPrice dari response Catalog
      |
      v
[6] Hitung: subtotal = productPrice × quantity
           totalAmount = jumlah semua subtotal
      |
      v
[7] PATCH /api/products/{id}/reduce-stock ke Catalog Service (per item)
      |
      v  (jika ada yang gagal → restore stok item sebelumnya)
[8] Simpan Order (status: PENDING) + OrderItems ke order_db
      |
      v
[9] Return OrderResponse
```

---

## 10. Alur Pay Order

```text
PATCH /api/orders/{id}/pay
      |
      v
[1] Cari order berdasarkan ID → 404 jika tidak ditemukan
      |
      v
[2] Cek status == PENDING → 400 jika bukan PENDING
      |
      v
[3] Ubah status → PAID
      |
      v
[4] Simpan ke order_db
      |
      v
[5] Return OrderResponse (status: PAID)
```

> Pembayaran tidak mengubah stok.

---

## 11. Alur Cancel Order

```text
PATCH /api/orders/{id}/cancel
      |
      v
[1] Cari order berdasarkan ID → 404 jika tidak ditemukan
      |
      v
[2] Cek status == PENDING → 400 jika bukan PENDING
      |
      v
[3] Loop setiap order item →
    PATCH /api/products/{id}/restore-stock ke Catalog Service
      |
      v
[4] Ubah status → CANCELLED
      |
      v
[5] Simpan ke order_db
      |
      v
[6] Return OrderResponse (status: CANCELLED)
```

---

## 12. Data Snapshot pada `order_items`

Saat order dibuat, Order Service menyimpan data berikut dari Catalog Service sebagai snapshot:

| Field | Sumber | Tujuan |
|---|---|---|
| `product_id` | Request client | Referensi produk (bukan FK) |
| `product_name` | Response Catalog Service | Snapshot nama saat transaksi |
| `product_price` | Response Catalog Service | Snapshot harga saat transaksi |
| `quantity` | Request client | Jumlah yang dipesan |
| `subtotal` | Dihitung backend | `product_price × quantity` |

**Kenapa snapshot diperlukan:** Jika nama atau harga produk berubah di Catalog Service setelah order dibuat, histori order lama tetap menampilkan data yang benar saat transaksi terjadi.

---

## 13. Komponen Komunikasi Antar-Service

Order Service wajib memiliki `CatalogClient` sebagai komponen khusus untuk komunikasi HTTP:

```java
// com.campus_commerce.order.client.CatalogClient

ProductResponse getProduct(Long productId);
void reduceStock(Long productId, Integer quantity);
void restoreStock(Long productId, Integer quantity);
```

- Implementasi menggunakan `RestClient` (Spring Boot 3.2+).
- Base URL diambil dari `application.properties`: `catalog.service.base-url`.
- Error dari Catalog Service diterjemahkan menjadi exception yang mudah dipahami.
- Stack trace tidak boleh tampil di response.

---

## 14. Struktur Package

### Catalog Service

```text
com.campus_commerce.catalog/
├── CatalogApplication.java
├── controller/
├── dto/
│   ├── request/
│   └── response/
├── model/
│   ├── entity/        ← Product.java
│   └── enums/         ← ProductStatus.java
├── repository/
├── service/
│   └── impl/
├── exception/
└── config/
```

### Order Service

```text
com.campus_commerce.order/
├── OrderApplication.java
├── client/            ← CatalogClient.java
├── config/
├── controller/
├── dto/
│   ├── request/
│   └── response/
├── model/
│   ├── entity/        ← Order.java, OrderItem.java
│   └── enums/         ← OrderStatus.java
├── repository/
├── service/
│   └── impl/
├── exception/
└── mapper/
```

---

## 15. Checklist Phase 0

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
