# AI Usage Log — Mini Commerce Kampus

Dokumen ini mencatat penggunaan AI selama pengerjaan proyek Mini Commerce Kampus.

**Tool AI yang digunakan:** Claude 

---

## Prompt 1

### Tanggal
13 Juni 2026

### Tujuan
Memahami arsitektur microservices dan merencanakan struktur proyek

### Prompt
```
Saya akan membuat backend microservices Spring Boot dengan dua service: Catalog Service dan Order Service. Bantu saya merencanakan struktur folder, endpoint yang dibutuhkan, aturan komunikasi antar service dan langkah-langkah yang harus dilakukan.
```

### Hasil yang Digunakan
- Arsitektur dua service dengan database terpisah
- Daftar endpoint wajib untuk Catalog dan Order Service
- Aturan bahwa Order Service tidak boleh query langsung ke catalog_db
- Konsep snapshot nama dan harga produk di order_items
- Langkah-langkah membuat microservices

### Modifikasi
- Menyesuaikan nama package dengan konvensi proyek (`com.campus_commerce`)
- Menambahkan endpoint internal `/reduce-stock` dan `/restore-stock` di Catalog Service

### Pemahaman
Saya memahami bahwa pemisahan database adalah inti dari arsitektur microservices. Order Service harus memanggil Catalog Service melalui HTTP untuk mendapatkan data produk, bukan mengakses database catalog secara langsung. Ini memastikan setiap service dapat di-deploy dan di-scale secara independen. Dan saya menjadi tahi bagaimana cara membuat microservices dari awal sampai bisa di jalankan.

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
Saya memahami bahwa `RestClient` dengan method `onStatus()` memungkinkan kita menghandle respons HTTP tertentu sebelum body diparse. Penting untuk membedakan error dari service (4xx/5xx) dengan service yang tidak bisa dihubungi sama sekali (connection refused/timeout), karena keduanya membutuhkan response yang berbeda ke client.

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

---

## Prompt 6

### Tanggal
23 Juni 2026

### Tujuan
Menambahkan pagination dan filter pada endpoint GET /api/products dan GET /api/orders

### Prompt
```
Bagaimana cara menambahkan pagination dan filter berdasarkan nama dan status pada endpoint Spring Boot yang sudah ada? Saya menggunakan Spring Data JPA dan ingin response-nya menggunakan format yang konsisten dengan field content, page, size, totalElements, dan totalPages.
```

### Hasil yang Digunakan
- Penggunaan `Pageable` dari Spring Data JPA di parameter controller
- `@ParameterObject` dari Springdoc agar parameter pagination muncul di Swagger
- `Page<T>` dari repository dan mapping ke custom `PagedResponse<T>`
- `@PageableDefault(size = 10)` sebagai nilai default
- Query JPQL dengan `LOWER(p.name) LIKE LOWER(:name)` untuk case-insensitive search

### Modifikasi
- Menambahkan method `findByFilters` di repository menggunakan `@Query` kustom
- Membuat class `PagedResponse<T>` sebagai wrapper generic untuk semua list response

### Pemahaman
Saya memahami bahwa `Pageable` di Spring Data JPA secara otomatis membaca parameter `page`, `size`, dan `sort` dari query string. `Page<T>` yang dikembalikan repository berisi metadata pagination yang bisa langsung digunakan. Custom `PagedResponse<T>` diperlukan karena `Page<T>` dari Spring tidak bisa langsung diserialisasi dengan format yang diinginkan.

---

## Prompt 7

### Tanggal
23 Juni 2026

### Tujuan
Mengintegrasikan Swagger UI menggunakan Springdoc OpenAPI

### Prompt
```
Bagaimana cara menambahkan dokumentasi Swagger UI di Spring Boot 3 menggunakan Springdoc OpenAPI? Saya ingin endpoint terdokumentasi dengan deskripsi, contoh request, dan kode respons yang mungkin.
```

### Hasil yang Digunakan
- Dependency `springdoc-openapi-starter-webmvc-ui` versi 2.8.9
- Anotasi `@Operation`, `@ApiResponse`, `@Tag` untuk dokumentasi controller
- `@Schema(description = "...")` pada field DTO untuk deskripsi field
- Konfigurasi `OpenAPI` bean dengan info title, version, dan description

### Modifikasi
- Memisahkan `@Operation` dengan `summary` dan `description` agar lebih informatif
- Menambahkan `@ApiResponse` untuk kode 201, 400, 404, 409, dan 503

### Pemahaman
Saya memahami bahwa Springdoc OpenAPI membaca anotasi Spring MVC secara otomatis dan menghasilkan dokumentasi tanpa banyak konfigurasi tambahan. `@ParameterObject` diperlukan khusus untuk parameter `Pageable` agar muncul sebagai query parameter individual, bukan satu objek.

---

## Prompt 8

### Tanggal
23 Juni 2026

### Tujuan
Menambahkan structured logging menggunakan SLF4J + Logback

### Prompt
```
Bagaimana cara menambahkan logging yang baik di Spring Boot? Saya ingin log INFO untuk operasi bisnis normal, WARN untuk error yang dapat diantisipasi, dan ERROR untuk exception yang tidak terduga.
```

### Hasil yang Digunakan
- `@Slf4j` dari Lombok sebagai shortcut deklarasi logger
- `log.info(...)` pada service setelah operasi berhasil (product created, order created, dll.)
- `log.warn(...)` di CatalogClient saat Catalog Service mengembalikan error 4xx
- `log.error(...)` di GlobalExceptionHandler untuk exception tak terduga dengan stack trace ke log (bukan ke response)

### Modifikasi
- Menambahkan parameter structured (id, sku, quantity) pada pesan log agar mudah di-parse
- Memisahkan log di GlobalExceptionHandler antara WARN (client error) dan ERROR (server error)

### Pemahaman
Saya memahami pentingnya structured logging: parameter di-embed di pesan log (bukan di-string-concatenate) menggunakan placeholder `{}`. Ini lebih efisien karena string hanya dibuild jika log level aktif. Stack trace hanya ditulis ke log server, tidak pernah ke response API.

---

## Prompt 9

### Tanggal
23 Juni 2026

### Tujuan
Membuat unit test menggunakan JUnit 5 dan Mockito

### Prompt
```
Bagaimana cara membuat unit test untuk Spring Boot service menggunakan JUnit 5 dan Mockito? Saya ingin test yang tidak memerlukan Spring context agar cepat dijalankan.
```

### Hasil yang Digunakan
- `@ExtendWith(MockitoExtension.class)` sebagai pengganti `@SpringBootTest` untuk unit test
- `@Mock` untuk membuat mock dependency (repository, client)
- `@InjectMocks` untuk inject mock ke class yang ditest
- `assertThrows(ExceptionClass.class, () -> ...)` untuk test exception
- `when(...).thenReturn(...)` dan `verify(...)` dari Mockito

### Modifikasi
- Memisahkan test per skenario (success + berbagai failure case) agar mudah dibaca
- Menggunakan nama method test yang deskriptif: `createProduct_duplicateSku_throwsException`

### Pemahaman
Saya memahami perbedaan unit test (tanpa Spring context, semua dependency di-mock) dan integration test (dengan Spring context). Unit test lebih cepat dan fokus pada logika bisnis. `@InjectMocks` menggunakan constructor injection jika tersedia, sehingga kompatibel dengan pola constructor injection yang sudah digunakan.

---

## Prompt 10

### Tanggal
23 Juni 2026

### Tujuan
Membuat integration test menggunakan MockMvc dan H2 in-memory database

### Prompt
```
Bagaimana cara membuat integration test di Spring Boot yang menggunakan H2 sebagai pengganti PostgreSQL? Saya ingin test yang menyimulasikan request HTTP nyata tanpa perlu server eksternal.
```

### Hasil yang Digunakan
- `@SpringBootTest` + `@AutoConfigureMockMvc` untuk test dengan full Spring context
- `@TestPropertySource(properties = {...})` untuk override datasource ke H2 in-memory
- `@Transactional` pada test class agar setiap test auto-rollback setelah selesai
- `@MockBean CatalogClient` di Order integration test agar tidak perlu Catalog Service aktif
- `MockMvc.perform(post(...).contentType(JSON).content(body)).andExpect(status().isCreated())`

### Modifikasi
- Menggunakan `DB_CLOSE_DELAY=-1` pada URL H2 agar koneksi tidak ditutup di tengah test
- Membuat helper method `createProduct()` dan `createOrder()` untuk mengurangi duplikasi setup

### Pemahaman
Saya memahami bahwa dengan `@Transactional` di test class, setiap test method berjalan dalam transaction yang di-rollback setelah selesai — sehingga test tidak saling mempengaruhi. `@MockBean` menggantikan bean nyata di Spring context, memungkinkan test Order Service tanpa Catalog Service aktif.

---

## Prompt 11

### Tanggal
23 Juni 2026

### Tujuan
Membuat Dockerfile dan Docker Compose untuk seluruh sistem

### Prompt
```
Bagaimana cara membuat Dockerfile untuk aplikasi Spring Boot menggunakan multi-stage build? Dan bagaimana cara mengorkestrasi dua service Spring Boot dengan dua PostgreSQL menggunakan Docker Compose termasuk health check?
```

### Hasil yang Digunakan
- Multi-stage Dockerfile: stage `build` menggunakan `maven:3.9-eclipse-temurin-17`, stage akhir menggunakan `eclipse-temurin:17-jre-alpine` (lebih kecil)
- `COPY pom.xml . && RUN mvn dependency:go-offline` sebelum `COPY src` untuk cache dependency layer
- `depends_on` dengan `condition: service_healthy` agar service tidak start sebelum DB siap
- Health check PostgreSQL dengan `pg_isready`
- Health check Spring Boot dengan `wget -qO- http://localhost:808x/api/...`
- `restart: on-failure` agar container restart jika crash

### Modifikasi
- Menambahkan `start_period: 40s` pada health check service untuk memberi waktu Spring Boot startup
- Menggunakan named volumes agar data PostgreSQL persisten antar restart

### Pemahaman
Saya memahami pentingnya urutan startup dalam Docker Compose. `depends_on` dengan `service_healthy` memastikan PostgreSQL benar-benar siap menerima koneksi sebelum Spring Boot mencoba connect. Multi-stage build memisahkan build tools dari runtime image, menghasilkan image yang lebih kecil dan lebih aman. Akan tetapi masih perlu dipelajari lebih lanjut untuk lebih memahami.

---

## Prompt 12

### Tanggal
23 Juni 2026

### Tujuan
Mengimplementasikan idempotency pada endpoint create order

### Prompt
```
Bagaimana cara mengimplementasikan idempotency pada endpoint POST /api/orders di Spring Boot? Saya ingin menggunakan header Idempotency-Key dan menyimpan mapping key ke orderId di database.
```

### Hasil yang Digunakan
- Entity `IdempotencyRecord` dengan field `idempotencyKey` (unique) dan `orderId`
- `IdempotencyService` interface dengan method `findOrderId(key)` dan `save(key, orderId)`
- Cek di controller: jika key sudah ada → ambil order dari DB dan return 200, jika belum → buat order baru lalu save key
- `@RequestHeader(required = false)` agar endpoint tetap berfungsi tanpa header

### Modifikasi
- Menyimpan hanya `orderId` (bukan full response JSON) untuk menghindari masalah deserialisasi dengan `@Builder`
- Menggunakan `@Transactional` pada `save()` dan `@Transactional(readOnly=true)` pada `findOrderId()`

### Pemahaman
Saya memahami bahwa idempotency penting untuk mencegah duplikasi akibat retry dari client (network timeout, dsb). Menyimpan response JSON secara utuh sulit karena deserialisasi JSON ke class dengan `@Builder` Lombok membutuhkan konfigurasi tambahan. Alternatif yang lebih sederhana: simpan hanya ID, lalu query ulang saat dibutuhkan.

---

## Prompt 13

### Tanggal
23 Juni 2026

### Tujuan
Membuat API Gateway menggunakan Spring Cloud Gateway

### Prompt
```
Bagaimana cara membuat API Gateway di Spring Boot menggunakan Spring Cloud Gateway? Saya ingin routing dari satu port ke dua service yang berbeda, dengan CORS dan logging request.
```

### Hasil yang Digunakan
- `spring-cloud-starter-gateway` dengan BOM `spring-cloud-dependencies 2024.0.0`
- Routing via `application.yml`: path predicate `/api/products/**` → Catalog Service, `/api/orders/**` → Order Service
- `CorsWebFilter` menggunakan `org.springframework.web.cors.reactive.*` (bukan servlet CORS)
- `GlobalFilter` + `Ordered` untuk logging method + path + status setiap request

### Modifikasi
- Menggunakan `setAllowedOriginPatterns(List.of("*"))` bukan `setAllowedOrigins` karena lebih fleksibel
- Menambahkan `ExposedHeaders` untuk `Idempotency-Key` agar dapat dibaca client
- URL upstream dikonfigurasi via environment variable agar dapat diganti saat deploy

### Pemahaman
Saya memahami bahwa Spring Cloud Gateway berbasis WebFlux (reactive, non-blocking), sehingga menggunakan `CorsWebFilter` dari package reactive, bukan `CorsFilter` dari package servlet. `GlobalFilter` memungkinkan intercept semua request/response tanpa perlu annotasi di setiap route.

---

## Bagian yang Sudah Dipahami

- Perbedaan Entity dan DTO, serta mengapa entity tidak boleh dikembalikan dari controller
- Arsitektur microservices dengan database terpisah per service
- Cara kerja `@RestControllerAdvice` dan `@ExceptionHandler`
- Komunikasi HTTP antar service menggunakan Spring RestClient
- Alur create order: validasi → kurangi stok → simpan snapshot → hitung total
- Alur cancel order: cek status PENDING → kembalikan stok tiap item → ubah status CANCELLED
- Kenapa `productId` di order_items bukan foreign key ke catalog_db
- Pagination dengan `Pageable` dan custom `PagedResponse<T>`
- Dokumentasi API dengan Springdoc OpenAPI dan Swagger UI
- Structured logging dengan SLF4J: INFO/WARN/ERROR sesuai severity
- Unit test tanpa Spring context menggunakan `@ExtendWith(MockitoExtension.class)`
- Integration test dengan H2 in-memory dan `@MockBean` untuk dependency eksternal
- Multi-stage Docker build dan orkestrasi dengan Docker Compose + health checks

## Bagian yang Masih Perlu Dipelajari

- Untuk Structured logging sudah berhasil di implementasi, akan tetapi masih perlu mempelajari lagi agar benar-benar paham
- Berhasil mengimplementasi H2 untuk integration testing, akan tetapi masih belum paham cara kerja nya seperti apa
- Penanganan race condition pada pengurangan stok secara concurrent (Saga Pattern atau Optimistic Locking)
- Distributed tracing untuk melacak request yang melewati multiple service (Zipkin/OpenTelemetry)
- Idempotency via header HTTP: simpan key→id di database, check sebelum proses
- API Gateway dengan Spring Cloud Gateway: routing, CORS reaktif, global logging filter
