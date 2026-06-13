# Spesifikasi Tugas Mini Commerce Kampus

## 1. Ringkasan Studi Kasus

Mini Commerce Kampus merupakan aplikasi backend sederhana untuk mengelola produk kantin kampus dan proses pemesanan produk. Sistem tidak memiliki frontend dan seluruh fungsi diuji menggunakan REST API melalui Postman.

Sistem dibangun menggunakan arsitektur microservices yang terdiri atas dua aplikasi Spring Boot terpisah:

1. **Catalog Service**, untuk mengelola data produk dan stok.
2. **Order Service**, untuk mengelola pemesanan produk.

Setiap service memiliki database PostgreSQL sendiri dan hanya boleh berkomunikasi melalui HTTP REST API. Order Service tidak diperbolehkan mengakses database milik Catalog Service secara langsung.

Fokus utama tugas ini bukan banyaknya fitur, tetapi kemampuan menerapkan:

- Layered architecture.
- Object-Oriented Programming.
- REST API.
- Request validation.
- Spring Data JPA.
- Relasi database.
- Business logic.
- Komunikasi antar-microservice.
- Transaction handling.
- Error handling yang konsisten.
- Dokumentasi penggunaan API.

---

## 2. Tujuan Pengerjaan

Setelah menyelesaikan tugas ini, mahasiswa diharapkan mampu:

1. Membuat dua aplikasi Spring Boot yang berjalan secara mandiri.
2. Menerapkan pemisahan tanggung jawab antara controller, service, repository, entity, dan DTO.
3. Menggunakan PostgreSQL sebagai database.
4. Membuat REST API sesuai kebutuhan bisnis.
5. Melakukan validasi request menggunakan Bean Validation.
6. Mengelola data menggunakan Spring Data JPA.
7. Mengimplementasikan komunikasi antar-service melalui HTTP.
8. Menjaga konsistensi stok ketika order dibuat atau dibatalkan.
9. Memberikan response dan error response yang konsisten.
10. Mendokumentasikan API dan penggunaan AI selama pengerjaan.

---

## 3. Arsitektur Sistem

```text
Client / Postman
       |
       | HTTP REST API
       |
       +---------------------------+
       |                           |
       v                           v
Catalog Service                Order Service
Port: 8081                     Port: 8082
Database: catalog_db           Database: order_db
       ^                           |
       |                           |
       +------ HTTP REST API <-----+
```

### 3.1 Catalog Service

Catalog Service bertanggung jawab untuk:

- Menyimpan data produk.
- Memvalidasi SKU produk.
- Mengelola harga produk.
- Mengelola jumlah stok.
- Mengelola status produk.
- Memberikan informasi produk kepada Order Service.
- Mengurangi atau menambahkan stok berdasarkan permintaan Order Service.

### 3.2 Order Service

Order Service bertanggung jawab untuk:

- Menyimpan data order.
- Menyimpan item-item dalam order.
- Memvalidasi data pelanggan.
- Memvalidasi produk melalui Catalog Service.
- Menghitung total harga order.
- Menyimpan snapshot nama dan harga produk.
- Meminta Catalog Service mengurangi stok saat order dibuat.
- Meminta Catalog Service mengembalikan stok saat order dibatalkan.
- Mengelola perubahan status order.

### 3.3 Aturan Pemisahan Database

- Catalog Service hanya menggunakan `catalog_db`.
- Order Service hanya menggunakan `order_db`.
- Order Service tidak boleh melakukan query langsung ke `catalog_db`.
- Catalog Service tidak boleh melakukan query langsung ke `order_db`.
- Pertukaran data hanya dilakukan melalui HTTP REST API.

---

## 4. Teknologi yang Digunakan

Teknologi minimum yang digunakan adalah:

- Java 17.
- Spring Boot.
- Spring Web.
- Spring Data JPA.
- Bean Validation.
- PostgreSQL.
- Maven atau Gradle.
- Postman.
- Lombok, jika diperlukan.

Teknologi tambahan yang dapat digunakan:

- Spring WebClient.
- RestClient.
- OpenFeign.
- Swagger/OpenAPI.
- Docker dan Docker Compose.

---

## 5. Spesifikasi Catalog Service

### 5.1 Informasi Service

| Komponen | Nilai |
|---|---|
| Nama service | `catalog-service` |
| Port | `8081` |
| Database | `catalog_db` |
| Tanggung jawab | Mengelola produk, stok, dan status produk |

### 5.2 Endpoint Wajib

| Method | Endpoint | Fungsi |
|---|---|---|
| `POST` | `/api/products` | Membuat produk baru |
| `GET` | `/api/products` | Menampilkan seluruh produk |
| `GET` | `/api/products/{id}` | Menampilkan detail produk |
| `PATCH` | `/api/products/{id}/stock` | Mengubah stok produk |
| `PATCH` | `/api/products/{id}/status` | Mengubah status produk |

### 5.3 Data Produk Minimum

Produk minimal memiliki atribut berikut:

| Field | Tipe Data | Keterangan |
|---|---|---|
| `id` | Long/UUID | Primary key |
| `sku` | String | Kode unik produk |
| `name` | String | Nama produk |
| `price` | Decimal | Harga produk |
| `stock` | Integer | Jumlah stok |
| `status` | Enum | `ACTIVE` atau `INACTIVE` |
| `createdAt` | DateTime | Waktu data dibuat |
| `updatedAt` | DateTime | Waktu data diperbarui |

### 5.4 Business Rules Produk

1. SKU wajib diisi.
2. SKU harus unik.
3. Produk dengan SKU yang sama tidak boleh dibuat dua kali.
4. Nama produk wajib diisi.
5. Harga produk harus lebih besar dari `0`.
6. Stok produk tidak boleh kurang dari `0`.
7. Status produk hanya boleh berupa:
   - `ACTIVE`
   - `INACTIVE`
8. Status default produk baru adalah `ACTIVE`.
9. Produk dengan status `INACTIVE` tidak boleh dipesan.
10. Pengurangan stok tidak boleh menyebabkan stok menjadi negatif.
11. Penambahan atau pengurangan stok harus dilakukan melalui service layer.

### 5.5 Operasi Stok

Endpoint stok harus mendukung kebutuhan berikut:

- Mengurangi stok ketika order dibuat.
- Menambahkan stok ketika order dibatalkan.
- Menolak pengurangan stok jika jumlah yang diminta melebihi stok yang tersedia.
- Menolak perubahan stok jika hasil akhirnya kurang dari `0`.

Contoh pendekatan request:

```json
{
  "quantity": 2,
  "operation": "DECREASE"
}
```

atau:

```json
{
  "quantity": 2,
  "operation": "INCREASE"
}
```

Bentuk request dapat disesuaikan, selama dapat mendukung proses pengurangan dan pengembalian stok secara aman.

---

## 6. Spesifikasi Order Service

### 6.1 Informasi Service

| Komponen | Nilai |
|---|---|
| Nama service | `order-service` |
| Port | `8082` |
| Database | `order_db` |
| Tanggung jawab | Mengelola order dan item order |

### 6.2 Endpoint Wajib

| Method | Endpoint | Fungsi |
|---|---|---|
| `POST` | `/api/orders` | Membuat order baru |
| `GET` | `/api/orders` | Menampilkan seluruh order |
| `GET` | `/api/orders/{id}` | Menampilkan detail order |
| `PATCH` | `/api/orders/{id}/pay` | Membayar order |
| `PATCH` | `/api/orders/{id}/cancel` | Membatalkan order |

### 6.3 Data Order Minimum

Order minimal memiliki atribut berikut:

| Field | Tipe Data | Keterangan |
|---|---|---|
| `id` | Long/UUID | Primary key |
| `orderNumber` | String | Nomor unik order |
| `customerName` | String | Nama pelanggan |
| `customerEmail` | String | Email pelanggan |
| `status` | Enum | Status order |
| `totalAmount` | Decimal | Total harga order |
| `createdAt` | DateTime | Waktu order dibuat |
| `updatedAt` | DateTime | Waktu order diperbarui |
| `items` | List | Daftar item order |

### 6.4 Data Order Item Minimum

Setiap item order minimal memiliki atribut berikut:

| Field | Tipe Data | Keterangan |
|---|---|---|
| `id` | Long/UUID | Primary key |
| `productId` | Long/UUID | ID produk dari Catalog Service |
| `productName` | String | Snapshot nama produk |
| `productPrice` | Decimal | Snapshot harga produk |
| `quantity` | Integer | Jumlah produk |
| `subtotal` | Decimal | Harga × quantity |

### 6.5 Business Rules Order

1. Nama pelanggan wajib diisi.
2. Email pelanggan wajib memiliki format email yang valid.
3. Order wajib memiliki minimal satu item.
4. Quantity setiap item minimal `1`.
5. Product ID wajib diisi.
6. Produk harus tersedia di Catalog Service.
7. Produk harus berstatus `ACTIVE`.
8. Stok produk harus mencukupi.
9. Total harga dihitung oleh backend.
10. Client tidak boleh menentukan total harga sendiri.
11. Nama dan harga produk harus disimpan sebagai snapshot.
12. Status awal order adalah `PENDING`.
13. Order dengan status `PENDING` dapat diubah menjadi:
    - `PAID`
    - `CANCELLED`
14. Order yang sudah `PAID` tidak dapat dibayar kembali.
15. Order yang sudah `PAID` tidak dapat dibatalkan.
16. Order yang sudah `CANCELLED` tidak dapat dibayar.
17. Order yang sudah `CANCELLED` tidak dapat dibatalkan kembali.
18. Saat order dibuat, stok produk dikurangi.
19. Saat order dibatalkan, stok produk dikembalikan.
20. Pembatalan hanya dapat dilakukan untuk order berstatus `PENDING`.

---

## 7. Status Transition Order

```text
                 PATCH /pay
            +-------------------+
            |                   v
        PENDING --------------> PAID
            |
            |
            | PATCH /cancel
            v
        CANCELLED
```

### 7.1 Aturan Status

| Status Saat Ini | Aksi | Status Berikutnya | Hasil |
|---|---|---|---|
| `PENDING` | Pay | `PAID` | Berhasil |
| `PENDING` | Cancel | `CANCELLED` | Berhasil dan stok dikembalikan |
| `PAID` | Pay | - | Gagal, `400 Bad Request` |
| `PAID` | Cancel | - | Gagal, `400 Bad Request` |
| `CANCELLED` | Pay | - | Gagal, `400 Bad Request` |
| `CANCELLED` | Cancel | - | Gagal, `400 Bad Request` |

---

## 8. Flow Pembuatan Order

Flow pembuatan order dilakukan sebagai berikut:

1. Client mengirim request ke:

   ```http
   POST /api/orders
   ```

2. `OrderController` menerima dan memvalidasi format request.

3. `OrderService` memastikan:
   - Customer name tersedia.
   - Email valid.
   - Items tidak kosong.
   - Quantity setiap item minimal `1`.

4. Untuk setiap item, Order Service memanggil Catalog Service:

   ```http
   GET /api/products/{id}
   ```

5. Order Service memvalidasi:
   - Produk ditemukan.
   - Produk berstatus `ACTIVE`.
   - Stok produk mencukupi.

6. Order Service mengambil nama dan harga produk sebagai snapshot.

7. Order Service menghitung subtotal setiap item:

   ```text
   subtotal = productPrice × quantity
   ```

8. Order Service menghitung total order:

   ```text
   totalAmount = jumlah seluruh subtotal
   ```

9. Order Service meminta Catalog Service mengurangi stok melalui HTTP.

10. Setelah seluruh stok berhasil dikurangi, Order Service menyimpan:
    - Order.
    - Order items.
    - Snapshot nama produk.
    - Snapshot harga produk.
    - Total harga.
    - Status `PENDING`.

11. Order Service mengembalikan response order yang berhasil dibuat.

---

## 9. Flow Pembatalan Order

Flow pembatalan order dilakukan sebagai berikut:

1. Client mengirim request:

   ```http
   PATCH /api/orders/{id}/cancel
   ```

2. Order Service mencari order berdasarkan ID.

3. Jika order tidak ditemukan, sistem mengembalikan `404 Not Found`.

4. Order Service memastikan status order masih `PENDING`.

5. Untuk setiap item order, Order Service memanggil Catalog Service untuk menambahkan kembali stok.

6. Setelah stok berhasil dikembalikan, status order diubah menjadi `CANCELLED`.

7. Perubahan order disimpan ke `order_db`.

8. Response order yang telah dibatalkan dikembalikan kepada client.

---

## 10. Komunikasi Antar-Service

Order Service harus memiliki komponen khusus untuk berkomunikasi dengan Catalog Service, misalnya:

```text
CatalogClient
```

Komponen tersebut dapat dibuat menggunakan:

- `RestClient`
- `WebClient`
- OpenFeign

Minimal fungsi yang dibutuhkan:

```java
ProductResponse getProduct(Long productId);
void reduceStock(Long productId, Integer quantity);
void restoreStock(Long productId, Integer quantity);
```

### Aturan Komunikasi

1. URL Catalog Service diletakkan pada configuration file.
2. URL service tidak boleh ditulis berulang kali di dalam business logic.
3. Order Service tidak boleh menggunakan repository milik Catalog Service.
4. Response dari Catalog Service dipetakan ke DTO, bukan entity.
5. Error dari Catalog Service harus diterjemahkan menjadi error yang mudah dipahami client.
6. Gangguan koneksi antar-service harus ditangani tanpa menampilkan stack trace kepada pengguna.

---

## 11. Layering Project

Setiap service minimal menggunakan struktur berikut:

```text
src/main/java/com/example/service
├── controller
├── service
├── repository
├── entity
├── dto
│   ├── request
│   └── response
├── exception
├── client
├── config
└── mapper
```

### Tanggung Jawab Setiap Layer

#### Controller

- Menerima HTTP request.
- Memvalidasi request.
- Memanggil service layer.
- Mengembalikan HTTP response.
- Tidak berisi business logic utama.

#### Service

- Menjalankan business logic.
- Mengatur transaksi database.
- Memanggil repository.
- Memanggil service eksternal melalui client.
- Mengelola perubahan status dan stok.

#### Repository

- Melakukan operasi database.
- Menggunakan Spring Data JPA.
- Tidak berisi business logic.

#### Entity

- Merepresentasikan tabel database.
- Memiliki anotasi JPA.
- Tidak digunakan langsung sebagai request dari client.

#### DTO

- Digunakan untuk request dan response.
- Memisahkan API contract dari struktur database.
- Dapat memiliki validasi seperti `@NotBlank`, `@Email`, `@Positive`, dan `@NotEmpty`.

#### Exception Handler

- Menangani exception secara global.
- Menghasilkan format error yang konsisten.
- Mencegah stack trace tampil pada response client.

---

## 12. Validasi Request

Validasi minimal yang harus diterapkan:

### Catalog Service

- `sku`: tidak boleh kosong.
- `name`: tidak boleh kosong.
- `price`: harus lebih besar dari `0`.
- `stock`: minimal `0`.
- `status`: hanya `ACTIVE` atau `INACTIVE`.

### Order Service

- `customerName`: tidak boleh kosong.
- `customerEmail`: format email harus valid.
- `items`: minimal satu item.
- `productId`: tidak boleh null.
- `quantity`: minimal `1`.

Contoh anotasi yang dapat digunakan:

```java
@NotBlank
@Email
@NotNull
@NotEmpty
@Positive
@PositiveOrZero
@Size
```

---

## 13. Error Handling

Sistem harus menghasilkan error response yang konsisten.

Contoh format:

```json
{
  "timestamp": "2026-06-13T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Stock is not sufficient",
  "path": "/api/orders"
}
```

### Status Code Minimum

| Kondisi | HTTP Status |
|---|---|
| Data berhasil dibuat | `201 Created` |
| Request berhasil | `200 OK` |
| Request tidak valid | `400 Bad Request` |
| Produk atau order tidak ditemukan | `404 Not Found` |
| SKU sudah digunakan | `409 Conflict` |
| Gangguan Catalog Service | `502 Bad Gateway` atau status yang sesuai |
| Kesalahan internal | `500 Internal Server Error` |

### Error yang Harus Ditangani

- SKU duplikat.
- Produk tidak ditemukan.
- Order tidak ditemukan.
- Produk tidak aktif.
- Stok tidak mencukupi.
- Quantity tidak valid.
- Email tidak valid.
- Items kosong.
- Perubahan status order tidak valid.
- Catalog Service tidak dapat dihubungi.
- Request body tidak sesuai format.
- Database error.

Response tidak boleh menampilkan stack trace.

---

## 14. Transaction Handling

### 14.1 Transaksi Database Lokal

Operasi penyimpanan order dan order items harus menggunakan transaksi database pada Order Service.

Contoh:

```java
@Transactional
public OrderResponse createOrder(CreateOrderRequest request) {
    // business process
}
```

### 14.2 Keterbatasan Transaksi Antar-Microservice

`@Transactional` hanya dapat menjamin transaksi pada satu database lokal. Anotasi tersebut tidak dapat melakukan rollback otomatis terhadap perubahan stok di Catalog Service.

Contoh masalah:

1. Stok di Catalog Service berhasil dikurangi.
2. Penyimpanan order di Order Service gagal.
3. Stok sudah berkurang, tetapi order tidak tersimpan.

Untuk tugas minimum, kondisi ini dapat ditangani menggunakan kompensasi sederhana:

1. Catat item yang stoknya sudah dikurangi.
2. Jika proses berikutnya gagal, panggil Catalog Service untuk mengembalikan stok.
3. Jelaskan keterbatasan pendekatan tersebut di README.

Pendekatan yang lebih lanjut seperti Saga Pattern, message broker, dan distributed transaction tidak wajib.

---

## 15. Snapshot Produk

Order Service wajib menyimpan snapshot data produk pada saat order dibuat.

Data snapshot minimal:

- Product ID.
- Product name.
- Product price.

Tujuan snapshot adalah menjaga histori order. Jika nama atau harga produk berubah di Catalog Service, order lama tetap menampilkan nama dan harga saat transaksi dilakukan.

Contoh:

```text
Harga produk saat order dibuat: Rp10.000
Harga produk setelah diperbarui: Rp12.000

Order lama tetap menyimpan harga Rp10.000.
```

---

## 16. Scope Wajib

Seluruh bagian berikut wajib diselesaikan:

- Dua aplikasi Spring Boot terpisah.
- Dua database PostgreSQL terpisah.
- Catalog Service berjalan pada port `8081`.
- Order Service berjalan pada port `8082`.
- REST API produk.
- REST API order.
- Validasi request.
- Spring Data JPA.
- Komunikasi HTTP dari Order Service ke Catalog Service.
- Pengurangan stok saat order dibuat.
- Pengembalian stok saat order dibatalkan.
- Validasi status produk.
- Validasi status order.
- Snapshot data produk.
- Error response konsisten.
- Postman collection.
- README.
- PDM atau ERD.
- DDL database.
- `AI_USAGE.md`.

---

## 17. Fitur yang Tidak Wajib

Fitur berikut tidak wajib untuk memenuhi spesifikasi minimum:

- Login.
- JWT authentication.
- Authorization.
- API Gateway.
- Eureka atau service discovery.
- RabbitMQ.
- Kafka.
- Payment gateway.
- Frontend.
- Deployment cloud.
- Docker.
- Unit test lengkap.
- Distributed transaction.

Fitur tidak wajib sebaiknya tidak dikerjakan sebelum seluruh scope wajib selesai.

---

## 18. Fitur Bonus

Fitur bonus dapat dikerjakan setelah fungsi wajib berjalan dengan baik.

### Bonus Catalog Service

- Pagination daftar produk.
- Search berdasarkan nama atau SKU.
- Filter berdasarkan status.
- Sorting berdasarkan harga atau nama.

### Bonus Order Service

- Filter order berdasarkan status.
- Search berdasarkan customer name.
- Pagination daftar order.
- Filter berdasarkan tanggal.

### Bonus Dokumentasi dan Infrastruktur

- Swagger/OpenAPI.
- Dockerfile.
- Docker Compose.
- Unit test.
- Integration test.
- API Gateway.
- Structured logging.
- Correlation ID.
- Idempotency key.
- Retry dan timeout komunikasi HTTP.

---

## 19. Acceptance Criteria

Tugas dinyatakan memenuhi acceptance criteria minimum apabila:

1. Catalog Service dapat berjalan pada port `8081`.
2. Order Service dapat berjalan pada port `8082`.
3. `catalog_db` dan `order_db` merupakan database terpisah.
4. Produk dapat dibuat dan ditampilkan.
5. SKU duplikat ditolak.
6. Harga dan stok yang tidak valid ditolak.
7. Order dapat dibuat.
8. Order Service memanggil Catalog Service melalui HTTP.
9. Order Service tidak mengakses `catalog_db` secara langsung.
10. Produk `INACTIVE` tidak dapat dipesan.
11. Order dengan quantity melebihi stok ditolak.
12. Stok berkurang setelah order dibuat.
13. Order berstatus awal `PENDING`.
14. Order `PENDING` dapat dibayar.
15. Order `PENDING` dapat dibatalkan.
16. Stok kembali setelah order dibatalkan.
17. Order yang sudah `PAID` tidak dapat dibayar atau dibatalkan kembali.
18. Order yang sudah `CANCELLED` tidak dapat dibayar atau dibatalkan kembali.
19. Error response tidak menampilkan stack trace.
20. README, Postman collection, PDM/ERD, DDL, dan `AI_USAGE.md` tersedia.

---

## 20. Flow Pengujian

### Pengujian 1 — Menjalankan Service

1. Jalankan PostgreSQL.
2. Jalankan Catalog Service pada port `8081`.
3. Jalankan Order Service pada port `8082`.
4. Pastikan kedua service dapat diakses.

### Pengujian 2 — Membuat Produk

1. Buat produk melalui `POST /api/products`.
2. Ambil detail produk melalui `GET /api/products/{id}`.
3. Pastikan data tersimpan di `catalog_db`.
4. Coba membuat produk dengan SKU yang sama.
5. Sistem harus menolak SKU duplikat.

### Pengujian 3 — Membuat Order

1. Buat order melalui `POST /api/orders`.
2. Pastikan order tersimpan di `order_db`.
3. Periksa detail produk.
4. Pastikan stok produk berkurang sesuai quantity.

### Pengujian 4 — Membayar Order

1. Bayar order melalui `PATCH /api/orders/{id}/pay`.
2. Pastikan status menjadi `PAID`.
3. Lakukan pembayaran sekali lagi.
4. Sistem harus mengembalikan `400 Bad Request`.

### Pengujian 5 — Membatalkan Order

1. Buat order baru.
2. Catat stok setelah order dibuat.
3. Batalkan order melalui `PATCH /api/orders/{id}/cancel`.
4. Pastikan status menjadi `CANCELLED`.
5. Pastikan stok produk kembali.
6. Coba batalkan kembali.
7. Sistem harus menolak request.

### Pengujian 6 — Produk Tidak Aktif

1. Ubah status produk menjadi `INACTIVE`.
2. Coba membuat order menggunakan produk tersebut.
3. Sistem harus menolak order.

### Pengujian 7 — Batas Stok

1. Cek stok produk.
2. Buat order dengan quantity lebih besar dari stok.
3. Sistem harus menolak order.
4. Stok tidak boleh berubah.

---

## 21. Dokumentasi yang Harus Dikumpulkan

### 21.1 README.md

README minimal memuat:

- Deskripsi project.
- Arsitektur sistem.
- Teknologi yang digunakan.
- Struktur project.
- Cara membuat database.
- Cara mengatur konfigurasi database.
- Cara menjalankan Catalog Service.
- Cara menjalankan Order Service.
- Daftar endpoint.
- Contoh request dan response.
- Penjelasan flow create order.
- Penjelasan flow cancel order.
- Penjelasan transaction handling.
- Keterbatasan sistem.
- Fitur bonus yang dikerjakan.

### 21.2 Postman Collection

Postman collection minimal memuat request untuk:

- Create product.
- List products.
- Product detail.
- Update stock.
- Update status.
- Create order.
- List orders.
- Order detail.
- Pay order.
- Cancel order.
- Pengujian SKU duplikat.
- Pengujian stok tidak cukup.
- Pengujian produk inactive.
- Pengujian status order tidak valid.

### 21.3 PDM atau ERD

Diagram database minimal memperlihatkan:

#### Catalog Database

```text
products
```

#### Order Database

```text
orders
    |
    +---- order_items
```

### 21.4 DDL

Sediakan file SQL untuk membuat tabel, constraint, dan index yang diperlukan.

Contoh nama file:

```text
database/
├── catalog_ddl.sql
└── order_ddl.sql
```

### 21.5 AI_USAGE.md

Dokumen ini minimal memuat:

- Prompt penting yang digunakan.
- Tujuan menggunakan AI.
- Ringkasan jawaban AI.
- Bagian kode yang dibantu AI.
- Perubahan yang dilakukan setelah menerima jawaban AI.
- Penjelasan bahwa mahasiswa memahami kode yang digunakan.

Contoh format:

```markdown
## Prompt 1

### Tujuan
Memahami cara membuat global exception handler.

### Prompt
Bagaimana cara membuat GlobalExceptionHandler pada Spring Boot?

### Ringkasan Hasil
AI memberikan contoh penggunaan @RestControllerAdvice dan @ExceptionHandler.

### Implementasi
Contoh disesuaikan dengan format ErrorResponse project.

### Pemahaman
GlobalExceptionHandler digunakan untuk memusatkan penanganan exception agar response error konsisten.
```

---

## 22. Rubrik Penilaian

| Kategori | Poin |
|---|---:|
| Struktur project dan kerapian kode | 15 |
| REST API dan validasi | 15 |
| Database dan JPA | 10 |
| Business logic | 15 |
| Komunikasi microservices | 15 |
| Testing dan dokumentasi | 15 |
| Penggunaan AI yang bertanggung jawab | 15 |
| **Total** | **100** |

Agar memperoleh hasil baik, project harus:

- Berjalan sesuai acceptance criteria.
- Memiliki struktur kode yang mudah dibaca.
- Memisahkan setiap layer dengan jelas.
- Memiliki validasi yang lengkap.
- Memiliki error handling yang konsisten.
- Menjelaskan keterbatasan transaksi antar-service.
- Memiliki dokumentasi pengujian yang lengkap.
- Dapat dijelaskan kembali oleh pembuatnya.

---

## 23. Urutan Prioritas Pengerjaan

Urutan pengerjaan yang disarankan adalah:

1. Membuat repository atau folder utama project.
2. Membuat `catalog-service`.
3. Membuat `order-service`.
4. Membuat `catalog_db`.
5. Membuat `order_db`.
6. Mengatur koneksi database masing-masing service.
7. Membuat entity dan repository Catalog Service.
8. Membuat DTO, service, dan controller Catalog Service.
9. Menguji seluruh endpoint Catalog Service.
10. Membuat entity dan repository Order Service.
11. Membuat DTO Order Service.
12. Membuat `CatalogClient`.
13. Membuat flow create order.
14. Membuat flow pay order.
15. Membuat flow cancel order.
16. Membuat global error handling.
17. Menjalankan tujuh flow pengujian.
18. Membuat Postman collection.
19. Membuat PDM/ERD dan DDL.
20. Membuat README.
21. Membuat `AI_USAGE.md`.
22. Mengerjakan fitur bonus setelah seluruh scope wajib selesai.

---

## 24. Kesimpulan Spesifikasi

Mini Commerce Kampus harus dibangun sebagai dua microservice Spring Boot yang independen, yaitu Catalog Service dan Order Service. Masing-masing service memiliki database PostgreSQL sendiri dan tidak diperbolehkan mengakses database service lain secara langsung.

Catalog Service bertanggung jawab atas pengelolaan produk, status, dan stok. Order Service bertanggung jawab atas pengelolaan order, validasi produk, penghitungan total, penyimpanan snapshot produk, serta perubahan status order.

Komunikasi antara kedua service dilakukan melalui HTTP REST API. Ketika order dibuat, Order Service harus memvalidasi produk dan stok ke Catalog Service, mengurangi stok, menghitung total, kemudian menyimpan order dengan status `PENDING`. Ketika order dibatalkan, stok harus dikembalikan. Order hanya dapat dibayar atau dibatalkan ketika masih berstatus `PENDING`.

Keberhasilan tugas tidak hanya dinilai dari API yang berjalan, tetapi juga dari kualitas struktur project, penerapan layering, validasi, business logic, komunikasi microservices, konsistensi error response, dokumentasi, dan kemampuan mahasiswa menjelaskan kode yang dibuat. Seluruh fitur wajib harus diselesaikan terlebih dahulu sebelum mengerjakan fitur bonus.
