# AI Usage Log — Mini Commerce Kampus

Dokumen ini mencatat penggunaan AI selama pengerjaan proyek Mini Commerce Kampus.

**Tool AI yang digunakan:** Claude (Anthropic) via Claude Code

---

## Prompt 1

### Tanggal
13 Juni 2026

### Tujuan
Memahami arsitektur microservices dan merencanakan struktur proyek

### Prompt
```
Saya membuat tugas backend microservices Spring Boot dengan dua service: Catalog Service dan Order Service. Bantu saya merencanakan struktur folder, endpoint yang dibutuhkan, dan aturan komunikasi antar service.
```

### Hasil yang Digunakan
- Arsitektur dua service dengan database terpisah
- Daftar endpoint wajib untuk Catalog dan Order Service
- Aturan bahwa Order Service tidak boleh query langsung ke catalog_db
- Konsep snapshot nama dan harga produk di order_items

### Modifikasi
- Menyesuaikan nama package dengan konvensi proyek (`com.campus_commerce`)
- Menambahkan endpoint internal `/reduce-stock` dan `/restore-stock` di Catalog Service

### Pemahaman
Saya memahami bahwa pemisahan database adalah inti dari arsitektur microservices. Order Service harus memanggil Catalog Service melalui HTTP untuk mendapatkan data produk, bukan mengakses database catalog secara langsung. Ini memastikan setiap service dapat di-deploy dan di-scale secara independen.

---

## Prompt 2

### Tanggal
13 Juni 2026

### Tujuan
Memahami pola layered architecture di Spring Boot (Controller → Service → Repository)

### Prompt
```
Jelaskan perbedaan antara Entity, DTO, Service interface, dan Service implementation di Spring Boot menggunakan contoh Product.
```

### Hasil yang Digunakan
- Pemahaman bahwa Entity hanya untuk mapping tabel database
- DTO digunakan sebagai kontrak request dan response API
- Service interface memisahkan kontrak dari implementasi
- Controller hanya menangani HTTP — logika bisnis ada di Service

### Modifikasi
- Menambahkan static factory method `from(entity)` di class response DTO agar mapping lebih rapi
- Memisahkan `StockAdjustmentRequest` untuk endpoint internal dan `UpdateStockRequest` untuk endpoint publik

### Pemahaman
Saya memahami mengapa entity tidak boleh dikembalikan langsung dari controller: entity mengekspos struktur internal database dan bisa menyebabkan circular reference pada relasi JPA. DTO memberikan kontrol penuh atas apa yang dikirim ke client.

---

## Prompt 3

### Tanggal
13 Juni 2026

### Tujuan
Memahami cara membuat GlobalExceptionHandler yang konsisten

### Prompt
```
Bagaimana cara membuat GlobalExceptionHandler di Spring Boot menggunakan @RestControllerAdvice? Saya ingin error response punya format yang konsisten: timestamp, status, error, message, path, dan errors untuk validation.
```

### Hasil yang Digunakan
- Penggunaan `@RestControllerAdvice` dan `@ExceptionHandler`
- Format `ErrorResponse` dengan field `errors` yang nullable (pakai `@JsonInclude(NON_NULL)`)
- Penanganan `MethodArgumentNotValidException` untuk mengekstrak field errors
- Penanganan `HttpMessageNotReadableException` untuk enum value yang tidak valid

### Modifikasi
- Menambahkan `HttpServletRequest` sebagai parameter untuk mengisi field `path`
- Memisahkan handler untuk setiap custom exception agar message lebih spesifik
- Menggunakan private helper method `build()` untuk menghindari duplikasi kode

### Pemahaman
Saya memahami bahwa `@RestControllerAdvice` mencegat exception sebelum response dikirim ke client, sehingga stack trace tidak pernah keluar ke response. `MethodArgumentNotValidException` terjadi saat Bean Validation gagal, dan kita bisa mengekstrak error per field dari `getBindingResult().getFieldErrors()`.

---

## Prompt 4

### Tanggal
13 Juni 2026

### Tujuan
Memahami cara komunikasi HTTP antar service menggunakan Spring RestClient

### Prompt
```
Saya membuat Order Service yang perlu memanggil Catalog Service menggunakan Spring RestClient. Bagaimana cara menangani error 404 (produk tidak ditemukan) dan connection refused (service tidak aktif) dengan response yang berbeda?
```

### Hasil yang Digunakan
- Penggunaan `RestClient.Builder` + `@Value` untuk base URL dari konfigurasi
- Method `onStatus()` untuk menangani HTTP 4xx/5xx dari service lain
- Try-catch berlapis: `CatalogServiceException` untuk error HTTP, `CatalogServiceUnavailableException` untuk connection error
- Response `503 Service Unavailable` saat Catalog Service tidak dapat dihubungi

### Modifikasi
- Memisahkan `CatalogServiceException` (4xx) dan `CatalogServiceUnavailableException` (503)
- Menambahkan pesan yang lebih deskriptif pada setiap kasus error
- Membuat `RestClientConfig` terpisah sebagai Spring `@Configuration` bean

### Pemahaman
Saya memahami bahwa `RestClient` adalah pengganti `RestTemplate` di Spring 6.1+. Method `onStatus()` memungkinkan kita menghandle respons HTTP tertentu sebelum body diparse. Penting untuk membedakan error dari service (4xx/5xx) dengan service yang tidak bisa dihubungi sama sekali (connection refused/timeout), karena keduanya membutuhkan response yang berbeda ke client.

---

## Prompt 5

### Tanggal
13 Juni 2026

### Tujuan
Memahami alur create order dan manajemen stok yang aman

### Prompt
```
Pada create order, saya perlu validasi semua produk dulu sebelum mengurangi stok. Apakah lebih baik validasi semua produk dulu baru kurangi stok, atau validasi dan kurangi per item?
```

### Hasil yang Digunakan
- Strategi validasi semua produk terlebih dahulu (existence + ACTIVE + stock sufficient)
- Baru setelah semua valid, lakukan pengurangan stok satu per satu
- Ini meminimalkan partial failure (stok sebagian berkurang saat ada item yang gagal)

### Modifikasi
- Menyimpan hasil `catalogClient.getProduct()` ke list `products` agar tidak memanggil API dua kali
- Menghitung subtotal menggunakan `product.getPrice().multiply(BigDecimal.valueOf(quantity))` — menggunakan `BigDecimal` arithmetic bukan `double`

### Pemahaman
Saya memahami bahwa pada sistem terdistribusi, urutan operasi sangat penting. Dengan memvalidasi semua item terlebih dahulu sebelum mengubah state (mengurangi stok), kita menghindari situasi di mana stok sebagian sudah berkurang tapi order gagal dibuat karena item lain tidak valid. Ini bukan solusi sempurna (masih ada race condition), tapi cukup untuk scope proyek ini.

---

## Bagian yang Sudah Dipahami

- Perbedaan Entity dan DTO, serta mengapa entity tidak boleh dikembalikan dari controller
- Arsitektur microservices dengan database terpisah per service
- Cara kerja `@RestControllerAdvice` dan `@ExceptionHandler`
- Komunikasi HTTP antar service menggunakan Spring RestClient
- Alur create order: validasi → kurangi stok → simpan snapshot → hitung total
- Alur cancel order: cek status PENDING → kembalikan stok tiap item → ubah status CANCELLED
- Kenapa `productId` di order_items bukan foreign key ke catalog_db

## Bagian yang Masih Perlu Dipelajari

- Penanganan race condition pada pengurangan stok secara concurrent (perlu Saga Pattern atau Optimistic Locking)
- Unit testing dengan Mockito untuk layer service
- Docker Compose untuk menjalankan seluruh sistem dalam container
