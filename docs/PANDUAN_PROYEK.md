# Panduan Menjalankan, Pengujian, dan Ringkasan Proyek

## Daftar Isi

1. [Cara Menjalankan](#cara-menjalankan)
2. [Cara Pengujian](#cara-pengujian)
3. [Ringkasan Proyek](#ringkasan-proyek)
4. [Kemungkinan Pertanyaan Penguji](#kemungkinan-pertanyaan-penguji)

---

# Cara Menjalankan

## Opsi A: Docker Compose (Paling Mudah)

Semua database, service, dan network dibuat otomatis.

### Prasyarat

- Docker Desktop sudah terinstall dan berjalan

### Langkah-Langkah

**1. Buka terminal di folder root project:**
```
D:\PROJECT\UJIKOM\BACKEND
```

**2. Jalankan semua service:**
```bash
docker compose up --build
```

Flag `--build` artinya Docker akan build ulang image dari source code. Proses pertama kali butuh beberapa menit untuk download dependency Maven.

**3. Tunggu sampai semua service siap.**

Di terminal akan muncul log seperti ini saat semua sudah jalan:
```
catalog-service  | Started CatalogApplication in 4.2 seconds
order-service    | Started OrderApplication in 5.1 seconds
gateway-service  | Started GatewayApplication in 3.8 seconds
```

**4. Akses service melalui satu base URL:**
```
http://localhost:8080
```

### Port yang Digunakan

| Service | Port | Keterangan |
|---|---|---|
| Gateway | 8080 | Entry point utama (pakai ini) |
| Catalog Service | 8081 | Bisa diakses langsung juga |
| Order Service | 8082 | Bisa diakses langsung juga |
| catalog-db | 5433 | PostgreSQL internal |
| order-db | 5434 | PostgreSQL internal |

### Menghentikan Semua Service

```bash
# Ctrl+C untuk stop, lalu:
docker compose down

# Jika ingin hapus data database juga:
docker compose down -v
```

---

## Opsi B: Menjalankan Lokal (Tanpa Docker)

### Prasyarat

- Java 17 sudah terinstall (cek: `java -version`)
- PostgreSQL sudah terinstall dan berjalan
- Maven wrapper sudah ada (sudah ada di project, tidak perlu install Maven)

### Langkah 1: Setup Database PostgreSQL

Buka `psql` atau pgAdmin, jalankan sebagai superuser:

```sql
-- Buat user untuk masing-masing service
CREATE ROLE catalog_user WITH LOGIN PASSWORD 'Catalog123!';
CREATE ROLE order_user   WITH LOGIN PASSWORD 'Order123!';

-- Buat database
CREATE DATABASE catalog_db OWNER catalog_user;
CREATE DATABASE order_db   OWNER order_user;
```

Tabel akan dibuat otomatis oleh Hibernate saat service pertama kali dijalankan.

### Langkah 2: Jalankan Catalog Service

Buka terminal pertama:
```bash
cd campus_commerce/catalog
./mvnw spring-boot:run
```

Tunggu sampai muncul:
```
Started CatalogApplication in X.X seconds
```

Catalog Service jalan di: `http://localhost:8081`

### Langkah 3: Jalankan Order Service

Buka terminal kedua (catalog harus sudah jalan):
```bash
cd campus_commerce/order
./mvnw spring-boot:run
```

Order Service jalan di: `http://localhost:8082`

### Langkah 4: Jalankan Gateway (Opsional)

Buka terminal ketiga:
```bash
cd campus_commerce/gateway
./mvnw spring-boot:run
```

Gateway jalan di: `http://localhost:8080`

> **Catatan Windows:** Gunakan `.\mvnw.cmd` jika `./mvnw` tidak dikenali.

---

## Verifikasi Service Berjalan

Buka browser atau Postman, akses URL berikut. Jika response berhasil, service sudah jalan.

```
GET http://localhost:8081/api/products
GET http://localhost:8082/api/orders
```

Response normal (list kosong):
```json
{
  "content": [],
  "page": 0,
  "size": 10,
  "totalElements": 0,
  "totalPages": 0
}
```

---

# Cara Pengujian

## 1. Unit Test dan Integration Test (Otomatis)

Unit test dan integration test dijalankan dengan satu perintah yang sama. Tidak perlu PostgreSQL aktif karena integration test menggunakan H2 (database in-memory).

### Jalankan Test Catalog Service

```bash
cd campus_commerce/catalog
./mvnw test
```

### Jalankan Test Order Service

```bash
cd campus_commerce/order
./mvnw test
```

### Hasil yang Diharapkan

```
Tests run: X, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Daftar Test yang Ada

**Catalog Service — Unit Test** (`ProductServiceImplTest.java`) — 7 test:

| Nama Test | Yang Diuji |
|---|---|
| `createProduct_success` | Produk berhasil dibuat, SKU dan nama benar |
| `createProduct_duplicateSku_throwsException` | SKU duplikat → `DuplicateSkuException` |
| `getProductById_notFound_throwsException` | ID tidak ada → `ProductNotFoundException` |
| `reduceStock_success` | Stok berkurang dengan benar (10 - 3 = 7) |
| `reduceStock_insufficientStock_throwsException` | Kurang dari yang diminta → `InsufficientStockException` |
| `reduceStock_inactiveProduct_throwsException` | Produk INACTIVE → `ProductInactiveException` |
| `restoreStock_success` | Stok bertambah dengan benar (10 + 5 = 15) |

**Catalog Service — Integration Test** (`ProductControllerIntegrationTest.java`) — 9 test:

| Nama Test | Yang Diuji |
|---|---|
| `createProduct_valid_returns201` | HTTP 201, status ACTIVE |
| `createProduct_duplicateSku_returns409` | HTTP 409 Conflict |
| `createProduct_blankName_returns400` | HTTP 400, ada `errors.name` |
| `createProduct_zeroPrice_returns400` | HTTP 400, ada `errors.price` |
| `getProducts_returns200WithPagination` | HTTP 200, ada field content/page/totalElements |
| `getProductById_returns200` | HTTP 200, data benar |
| `getProductById_notFound_returns404` | HTTP 404 |
| `reduceStock_success_stockDecreases` | Stok berkurang dari 10 menjadi 7 |
| `reduceStock_insufficient_returns400` | HTTP 400 saat stok tidak cukup |

**Order Service — Unit Test** (`OrderServiceImplTest.java`) — 9 test:

| Nama Test | Yang Diuji |
|---|---|
| `createOrder_success_totalAndSnapshotCorrect` | Total benar, snapshot nama/harga tersimpan |
| `createOrder_productNotFound_throwsException` | Produk tidak ada di catalog → exception |
| `createOrder_inactiveProduct_throwsException` | Produk INACTIVE → `ProductInactiveException` |
| `createOrder_insufficientStock_throwsException` | Stok tidak cukup → exception |
| `payOrder_pending_success` | Status berubah menjadi PAID |
| `payOrder_nonPending_throwsException` | Order sudah PAID → `InvalidOrderStatusException` |
| `cancelOrder_pending_success_restoresStock` | Status CANCELLED, stok dikembalikan |
| `cancelOrder_paid_throwsException` | Order PAID tidak bisa cancel |
| `cancelOrder_alreadyCancelled_throwsException` | Order sudah CANCELLED tidak bisa cancel lagi |

**Order Service — Integration Test** (`OrderControllerIntegrationTest.java`) — 13 test:

| Nama Test | Yang Diuji |
|---|---|
| `createOrder_valid_returns201WithSnapshot` | HTTP 201, total benar, snapshot tersimpan |
| `createOrder_emptyItems_returns400` | HTTP 400 saat items kosong |
| `createOrder_invalidEmail_returns400` | HTTP 400 saat email salah format |
| `createOrder_inactiveProduct_returns400` | HTTP 400 produk INACTIVE |
| `createOrder_insufficientStock_returns400` | HTTP 400 stok tidak cukup |
| `createOrder_productNotFound_returns400` | HTTP 400 produk tidak ditemukan |
| `getOrders_returns200WithPagination` | HTTP 200 dengan pagination |
| `getOrderById_returns200` | HTTP 200, data benar |
| `getOrderById_notFound_returns404` | HTTP 404 |
| `payOrder_pending_returns200WithPaid` | HTTP 200, status PAID |
| `payOrder_alreadyPaid_returns409` | HTTP 409 Conflict |
| `cancelOrder_pending_returns200WithCancelled` | HTTP 200, status CANCELLED |
| `cancelOrder_paid_returns409` | HTTP 409 Conflict |

---

## 2. Pengujian Manual via Postman

### Setup Postman

1. Buka Postman
2. Klik **Import**
3. Pilih file: `campus_commerce/postman/campus-commerce.postman_collection.json`
4. Collection otomatis terkonfigurasi

### Skenario Pengujian Manual (Jalankan Berurutan)

#### Skenario 1: Alur Lengkap (Happy Path)

**Step 1 — Buat produk:**
```
POST http://localhost:8081/api/products
Body:
{
  "sku": "PROD-001",
  "name": "Nasi Goreng Spesial",
  "price": 15000,
  "stock": 10
}
```
Catat `id` dari response (misal: `1`).

**Step 2 — Verifikasi stok awal:**
```
GET http://localhost:8081/api/products/1
```
Pastikan `stock: 10`.

**Step 3 — Buat order:**
```
POST http://localhost:8082/api/orders
Body:
{
  "customerName": "Budi Santoso",
  "customerEmail": "budi@example.com",
  "items": [
    { "productId": 1, "quantity": 3 }
  ]
}
```
Catat `id` order dari response (misal: `1`). Pastikan status `PENDING` dan `totalAmount: 45000.00`.

**Step 4 — Verifikasi stok berkurang:**
```
GET http://localhost:8081/api/products/1
```
Stok harus berkurang menjadi `7` (10 - 3 = 7).

**Step 5 — Bayar order:**
```
PATCH http://localhost:8082/api/orders/1/pay
```
Status berubah menjadi `PAID`.

---

#### Skenario 2: Cancel dan Restore Stok

Buat order baru (gunakan step 1-3 dari skenario 1), lalu:

**Cancel order:**
```
PATCH http://localhost:8082/api/orders/{id}/cancel
```
Status berubah menjadi `CANCELLED`.

**Verifikasi stok kembali:**
```
GET http://localhost:8081/api/products/1
```
Stok harus kembali ke nilai sebelumnya (stok dipulihkan).

---

#### Skenario 3: Validasi Error

**Coba bayar order yang sudah PAID:**
```
PATCH http://localhost:8082/api/orders/1/pay
```
Response: `409 Conflict`, message berisi penjelasan status tidak valid.

**Coba buat produk SKU duplikat:**
```
POST http://localhost:8081/api/products
Body: { "sku": "PROD-001", ... }
```
Response: `409 Conflict`.

**Coba order dengan stok tidak cukup (stok 5, order 100):**
```
POST http://localhost:8082/api/orders
Body: { ..., "items": [{ "productId": 1, "quantity": 100 }] }
```
Response: `400 Bad Request`.

---

#### Skenario 4: Idempotency

**Kirim order pertama dengan Idempotency-Key:**
```
POST http://localhost:8082/api/orders
Header: Idempotency-Key: test-key-abc123
Body: { ... }
```
Response: `201 Created`, order baru dibuat.

**Kirim ulang request yang sama:**
```
POST http://localhost:8082/api/orders
Header: Idempotency-Key: test-key-abc123
Body: { ... }  (sama persis)
```
Response: `200 OK`, order yang sama dikembalikan. **Stok tidak berkurang dua kali.**

---

## 3. Pengujian via Swagger UI

Swagger UI memungkinkan test endpoint langsung dari browser.

| Service | URL Swagger |
|---|---|
| Catalog Service | http://localhost:8081/swagger-ui.html |
| Order Service | http://localhost:8082/swagger-ui.html |

**Cara pakai:**
1. Buka URL Swagger di browser
2. Pilih endpoint yang ingin dicoba
3. Klik **Try it out**
4. Isi body request
5. Klik **Execute**
6. Lihat response di bawah

---

# Ringkasan Proyek

## Apa Proyek Ini?

**Mini Commerce Kampus** adalah sistem backend e-commerce sederhana untuk lingkungan kampus. Dibangun menggunakan arsitektur **microservices** — yaitu arsitektur di mana satu aplikasi dibagi menjadi beberapa service kecil yang berjalan secara terpisah dan berkomunikasi lewat HTTP.

Proyek ini terdiri dari **3 service**:

| Service | Port | Fungsi |
|---|---|---|
| **Catalog Service** | 8081 | Kelola produk: buat, lihat, atur stok dan status |
| **Order Service** | 8082 | Kelola order: buat, bayar, batalkan |
| **Gateway Service** | 8080 | Pintu masuk tunggal — forward request ke service yang tepat |

---

## Alur Kerja Utama

### Membuat Order (Create Order)

```
Client → POST /api/orders
           |
           ▼
     Order Service
           |
     1. Validasi request (nama, email, items tidak kosong)
           |
     2. GET /api/products/{id} ke Catalog Service
        - Cek produk ada (404 jika tidak)
        - Cek status ACTIVE (400 jika INACTIVE)
        - Cek stok cukup (400 jika kurang)
           |
     3. PATCH /api/products/{id}/reduce-stock ke Catalog Service
        - Kurangi stok setiap item
           |
     4. Simpan order ke order_db
        - Simpan nama produk & harga sebagai SNAPSHOT
        - Hitung total & subtotal
        - Status awal: PENDING
           |
     5. Return 201 Created
```

### Membatalkan Order (Cancel Order)

```
Client → PATCH /api/orders/{id}/cancel
           |
     Order Service
           |
     1. Cek order ada (404 jika tidak)
     2. Cek status PENDING (409 jika PAID/CANCELLED)
           |
     3. PATCH /api/products/{id}/restore-stock ke Catalog Service
        - Kembalikan stok setiap item di order
           |
     4. Update status order menjadi CANCELLED
     5. Return 200 OK
```

---

## Penjelasan Teknis Penting

### Mengapa Database Dipisah?

Setiap service punya database sendiri:
- `catalog_db` → hanya diakses Catalog Service
- `order_db` → hanya diakses Order Service

**Alasannya:** Jika database dibagi, satu service bisa merusak data service lain. Pemisahan database memastikan setiap service benar-benar independen — bisa di-deploy, di-scale, bahkan di-shutdown secara terpisah tanpa mempengaruhi service lain.

### Apa itu Snapshot Pattern?

Saat order dibuat, nama dan harga produk **disalin** ke tabel `order_items`:

```
order_items:
  product_id   = 1          ← hanya ID, bukan FK ke catalog_db
  product_name = "Nasi Goreng Spesial"  ← SNAPSHOT nama saat order dibuat
  unit_price   = 15000.00               ← SNAPSHOT harga saat order dibuat
```

**Mengapa?** Karena jika nama atau harga produk berubah di kemudian hari, data order lama tidak boleh ikut berubah. Order sudah "terkunci" pada harga saat pembelian.

`product_id` di `order_items` adalah kolom biasa, **bukan foreign key** ke `catalog_db`. Ini disengaja — Order Service tidak boleh tahu struktur internal Catalog Service.

### Mengapa Validasi Dilakukan Sebelum Kurangi Stok?

Di `OrderServiceImpl`, logikanya:
1. **Validasi SEMUA produk dulu** (cek ada, cek ACTIVE, cek stok cukup)
2. **Baru kurangi stok semua**

Jika validasi per-item sekaligus kurangi stok, bisa terjadi situasi: item pertama stoknya sudah dikurangi, tapi item kedua gagal validasi. Stok jadi "bocor" padahal order tidak jadi dibuat.

### Apa itu DTO?

**DTO (Data Transfer Object)** adalah class yang digunakan sebagai "wadah" data untuk request dan response API. Entity (class yang map ke tabel database) **tidak boleh** dikembalikan langsung dari controller.

**Contoh:**
- `CreateProductRequest` → data yang dikirim client saat membuat produk
- `ProductResponse` → data yang dikembalikan ke client (bukan entity `Product`)

**Mengapa?** Entity mengekspos struktur internal database. Jika ada field sensitif atau relasi JPA yang circular, bisa bermasalah. DTO memberi kontrol penuh atas apa yang dikirim dan diterima.

### Apa itu Constructor Injection?

```java
// DIPAKAI (Constructor Injection — benar)
public ProductServiceImpl(ProductRepository productRepository) {
    this.productRepository = productRepository;
}

// TIDAK DIPAKAI (@Autowired field — kurang baik)
@Autowired
private ProductRepository productRepository;
```

Constructor injection lebih baik karena:
- Dependency jelas terlihat di constructor
- Memudahkan unit test (bisa inject mock tanpa Spring)
- Object tidak bisa dibuat tanpa dependency-nya terpenuhi

### Mengapa BigDecimal, Bukan double?

Harga dan total menggunakan `BigDecimal`, bukan `double` atau `float`:

```java
BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
```

**Alasannya:** `double` punya masalah presisi floating point. Contoh: `0.1 + 0.2 = 0.30000000000000004` di Java. Untuk nilai uang, ini tidak bisa diterima. `BigDecimal` memberikan presisi eksak.

### Apa itu @Transactional?

`@Transactional` memastikan semua operasi database dalam satu method berjalan sebagai satu unit atomik:
- Jika **berhasil semua** → data tersimpan (commit)
- Jika **ada yang gagal** → semua dibatalkan (rollback)

Contoh di `createOrder`: jika pengurangan stok berhasil tapi penyimpanan order gagal, stok tidak ikut di-rollback karena pengurangan stok ada di service yang berbeda (Catalog Service). Inilah keterbatasan microservices — tidak ada distributed transaction. Untuk scope proyek ini, dianggap cukup.

### Apa itu GlobalExceptionHandler?

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(...) { ... }
}
```

Mencegat semua exception sebelum response dikirim ke client. Hasilnya:
- **Stack trace tidak pernah keluar** ke response API (hanya ditulis ke log server)
- Format error selalu konsisten: `{ timestamp, status, error, message, path }`

---

## Struktur Package

```
com.campus_commerce.catalog
├── controller/    ← Terima request HTTP, return response
├── dto/
│   ├── request/   ← Data dari client (CreateProductRequest, dll.)
│   └── response/  ← Data ke client (ProductResponse, PagedResponse, dll.)
├── exception/     ← Custom exception + GlobalExceptionHandler
├── model/
│   ├── entity/    ← Mapping ke tabel database (Product)
│   └── enums/     ← Enum (ProductStatus, StockOperation)
├── repository/    ← Akses database (JPA Repository)
└── service/
    ├── ProductService.java         ← Interface
    └── impl/ProductServiceImpl.java ← Implementasi
```

Pola yang sama diikuti di Order Service.

---

## Teknologi yang Dipakai dan Alasannya

| Teknologi | Dipakai Untuk | Alasan |
|---|---|---|
| **Spring Boot 3.5.15** | Framework utama | Convention over configuration, production-ready |
| **Spring Data JPA** | ORM / akses database | Tidak perlu tulis SQL manual untuk operasi dasar |
| **Spring RestClient** | HTTP call antar service | Pengganti RestTemplate di Spring 6.1+, lebih modern |
| **Spring Cloud Gateway** | API Gateway | Routing reaktif, CORS, logging request |
| **Bean Validation** | Validasi request | Anotasi `@NotBlank`, `@Email`, `@Min` — tidak perlu if-else manual |
| **Lombok** | Kurangi boilerplate | `@Getter`, `@Setter`, `@Builder`, `@Slf4j` |
| **PostgreSQL** | Database produksi | ACID compliant, cocok untuk data transaksional |
| **H2** | Database test | In-memory, tidak perlu setup — hanya dipakai saat test |
| **JUnit 5 + Mockito** | Unit testing | Standard Java testing, Mockito untuk mock dependency |
| **Springdoc OpenAPI** | Swagger UI | Dokumentasi API otomatis dari anotasi |
| **Docker + Compose** | Containerisasi | Satu perintah menjalankan semua service |

---

## Fitur Bonus yang Diimplementasikan

| Fitur | Detail |
|---|---|
| **Pagination + Filter** | GET /api/products?name=nasi&status=ACTIVE&page=0&size=10 |
| **Swagger UI** | Dokumentasi interaktif di /swagger-ui.html |
| **Structured Logging** | INFO/WARN/ERROR dengan parameter terstruktur |
| **Unit Test** | 16 test case (7 catalog + 9 order) |
| **Integration Test** | 22 test case (9 catalog + 13 order) dengan H2 |
| **Docker Compose** | Satu perintah `docker compose up --build` |
| **Idempotency** | Header Idempotency-Key mencegah duplikasi order |
| **API Gateway** | Port 8080 sebagai single entry point |

---

# Kemungkinan Pertanyaan Penguji

### Tentang Arsitektur

**Q: Kenapa pakai microservices, bukan monolith?**

A: Untuk tujuan pembelajaran arsitektur terdistribusi. Microservices memungkinkan setiap service di-deploy dan di-scale secara independen. Catalog Service dan Order Service bisa dikembangkan oleh tim berbeda tanpa mengganggu satu sama lain. Di proyek ini, pemisahan juga memaksa kami untuk memikirkan komunikasi antar service secara eksplisit.

---

**Q: Bagaimana Order Service tahu harga produk?**

A: Order Service memanggil `GET /api/products/{id}` ke Catalog Service melalui HTTP menggunakan Spring RestClient. Harga dan nama produk yang didapat dari Catalog Service kemudian disimpan sebagai snapshot di tabel `order_items`. Jadi setelah order dibuat, Order Service tidak perlu lagi tanya ke Catalog Service untuk tahu harga order tersebut.

---

**Q: Kenapa `product_id` di order_items bukan foreign key ke products?**

A: Karena `products` ada di `catalog_db` (database berbeda). Foreign key hanya bisa dibuat dalam satu database yang sama. Selain itu, secara prinsip microservices, Order Service tidak boleh bergantung langsung pada struktur internal Catalog Service. Kalau Catalog Service diganti database-nya, Order Service tidak perlu berubah.

---

**Q: Bagaimana jika Catalog Service mati saat Order Service mencoba memanggil?**

A: Di `CatalogClient`, ada try-catch berlapis. Jika koneksi gagal (connection refused, timeout), akan di-throw `CatalogServiceUnavailableException` yang ditangkap `GlobalExceptionHandler` dan dikembalikan sebagai `503 Service Unavailable` ke client. Jika Catalog Service memberikan response 4xx, akan di-throw `CatalogServiceException` dengan pesan dari Catalog Service.

---

### Tentang Kode

**Q: Jelaskan alur create order dari controller sampai database.**

A:
1. `OrderController.createOrder()` menerima request, validasi Bean Validation
2. Kalau ada `Idempotency-Key` header, cek apakah sudah pernah diproses
3. Panggil `OrderService.createOrder(request)`
4. Di service: validasi semua produk dulu (GET ke Catalog), baru kurangi stok (PATCH ke Catalog)
5. Buat entity `Order` + list `OrderItem` dengan data snapshot
6. Hitung total menggunakan BigDecimal
7. Simpan ke `order_db` via `OrderRepository`
8. Log info order created
9. Return `OrderResponse.from(order)` — entity dikonversi ke DTO

---

**Q: Apa bedanya unit test dan integration test di proyek ini?**

A:
- **Unit test** (`ProductServiceImplTest`, `OrderServiceImplTest`): tidak ada Spring context, semua dependency di-mock dengan Mockito. Test hanya fokus pada logika bisnis di layer service. Sangat cepat.
- **Integration test** (`ProductControllerIntegrationTest`, `OrderControllerIntegrationTest`): Spring context penuh dengan `@SpringBootTest`, database nyata tapi menggunakan H2 in-memory (bukan PostgreSQL), request HTTP disimulasikan dengan `MockMvc`. Test lebih realistis tapi lebih lambat.

---

**Q: Kenapa tidak mengembalikan entity langsung dari controller?**

A: Ada dua alasan utama. Pertama, entity mengekspos field internal yang tidak seharusnya dilihat client (misal: field yang berubah di masa depan, field internal framework). Kedua, relasi JPA bisa menyebabkan circular reference saat serialisasi JSON — misalnya `Order` punya list `OrderItem`, dan `OrderItem` punya referensi balik ke `Order`. Dengan DTO, kita kontrol penuh apa yang keluar ke response.

---

**Q: Apa itu idempotency dan kenapa penting?**

A: Idempotency berarti request yang sama dieksekusi berulang kali menghasilkan efek yang sama seperti dieksekusi sekali. Ini penting untuk create order karena: bayangkan client kirim request, tapi jaringan timeout sebelum response diterima. Client tidak tahu apakah order sudah dibuat atau belum. Tanpa idempotency, client yang retry akan membuat dua order dan stok berkurang dua kali. Dengan `Idempotency-Key`, request kedua akan mengembalikan order pertama yang sudah ada.

---

**Q: Kenapa Gateway pakai port 8080, bukan 8081 atau 8082?**

A: Konvensi umum: port 8080 adalah port default untuk HTTP server di development. Dengan gateway di 8080, client hanya perlu satu base URL untuk mengakses semua endpoint. Di production, gateway biasanya di port 80 (HTTP) atau 443 (HTTPS) dengan reverse proxy seperti Nginx di depannya.

---

**Q: Apa yang terjadi jika client kirim request dengan body yang tidak valid?**

A: Spring Boot menjalankan **Bean Validation** (`@Valid` di controller). Jika ada field yang tidak valid (misal: email kosong, quantity 0), Spring throw `MethodArgumentNotValidException`. `GlobalExceptionHandler` menangkap exception ini, mengekstrak semua field error, dan mengembalikan response `400 Bad Request` dengan format:
```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "customerEmail": "must be a well-formed email address",
    "items": "Order must have at least one item"
  }
}
```

---

**Q: Bagaimana cara memastikan tidak ada stack trace di response?**

A: `@RestControllerAdvice` di `GlobalExceptionHandler` mencegat **semua** exception yang tidak ditangani di controller. Handler `handleGeneral` menangkap `Exception.class` sebagai catch-all terakhir. Di dalam handler ini, stack trace hanya ditulis ke log server (`log.error(..., ex)`), sedangkan response yang dikembalikan ke client hanya berisi pesan error yang sudah diformat — tanpa stack trace.

---

**Q: Bagaimana pengurangan stok bekerja jika ada beberapa item dalam satu order?**

A: Di `OrderServiceImpl.createOrder()`, ada dua loop terpisah:
- **Loop 1:** Validasi semua item — cek produk ada, ACTIVE, stok cukup. Kalau ada satu yang gagal, langsung throw exception, tidak ada stok yang diubah.
- **Loop 2:** Baru setelah semua lolos validasi, kurangi stok satu per satu via HTTP call ke Catalog Service.

Ini meminimalkan partial failure. Tapi ini bukan solusi sempurna — masih ada race condition jika dua order dibuat secara bersamaan untuk produk yang sama dengan stok terbatas. Solusi sempurnanya butuh distributed lock atau Saga Pattern, tapi itu di luar scope proyek ini.
