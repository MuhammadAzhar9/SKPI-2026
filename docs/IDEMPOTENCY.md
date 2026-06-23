# Idempotency — Create Order

## Cara Penggunaan

Tambahkan header `Idempotency-Key` pada request `POST /api/orders`:

```http
POST /api/orders
Idempotency-Key: order-budi-20260623-001
Content-Type: application/json

{
  "customerName": "Budi",
  "customerEmail": "budi@example.com",
  "items": [{ "productId": 1, "quantity": 2 }]
}
```

Jika request dikirim ulang dengan **key yang sama**, server mengembalikan order yang sudah dibuat sebelumnya tanpa membuat order baru dan tanpa mengurangi stok lagi.

Jika header tidak disertakan, order dibuat seperti biasa (tanpa jaminan idempotency).

## Cara Kerja

```
Request masuk dengan Idempotency-Key
         |
         v
  Key sudah ada di DB?
    YES ──► Kembalikan order lama (fetch by orderId)
    NO  ──► Buat order baru → simpan key + orderId → kembalikan order baru
```

Key dan orderId disimpan di tabel `idempotency_records` pada `order_db`.

## Trade-off

| Aspek | Penjelasan |
|---|---|
| **Keuntungan** | Mencegah order duplikat saat client melakukan retry akibat timeout atau network error |
| **Response cached** | Response dikembalikan dengan fetch ulang dari DB, bukan dari cache statis — sehingga status order yang mungkin sudah berubah (PAID/CANCELLED) akan terlihat |
| **Tidak ada TTL** | Key disimpan selamanya — key yang sama tidak pernah bisa dipakai untuk order baru meski sudah berbulan-bulan |
| **Race condition** | Dua request simultan dengan key yang sama dapat lolos validasi sebelum salah satu menyimpan key, menyebabkan dua order terbuat. Untuk mencegah ini diperlukan distributed lock atau database-level locking yang lebih kuat |
| **Key unik per transaksi** | Client bertanggung jawab menghasilkan key yang benar-benar unik (misal UUID atau kombinasi user+timestamp+nonce) |
| **Scope** | Idempotency hanya diterapkan pada `POST /api/orders`. Endpoint lain (pay, cancel) secara alami idempoten karena operasinya adalah state machine transition |
