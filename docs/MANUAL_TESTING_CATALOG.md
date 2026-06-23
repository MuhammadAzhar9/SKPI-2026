# Manual Testing — Catalog Service

Base URL: `http://localhost:8081`

---

## 1. POST /api/products — Buat Produk Baru

### 1a. Request valid

```
POST http://localhost:8081/api/products
Content-Type: application/json
```

```json
{
  "sku": "PROD-001",
  "name": "Nasi Goreng Spesial",
  "price": 15000,
  "stock": 50
}
```

**Expected: 201 Created**

```json
{
  "id": 1,
  "sku": "PROD-001",
  "name": "Nasi Goreng Spesial",
  "price": 15000.00,
  "stock": 50,
  "status": "ACTIVE",
  "createdAt": "...",
  "updatedAt": "..."
}
```

---

### 1b. SKU duplikat

```json
{
  "sku": "PROD-001",
  "name": "Produk Lain",
  "price": 10000,
  "stock": 10
}
```

**Expected: 409 Conflict**

```json
{
  "timestamp": "...",
  "status": 409,
  "error": "Conflict",
  "message": "Product with SKU 'PROD-001' already exists",
  "path": "/api/products"
}
```

---

### 1c. SKU kosong

```json
{
  "sku": "",
  "name": "Produk A",
  "price": 5000,
  "stock": 10
}
```

**Expected: 400 Bad Request**

```json
{
  "timestamp": "...",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/products",
  "errors": {
    "sku": "SKU is required"
  }
}
```

---

### 1d. Harga nol / negatif

```json
{
  "sku": "PROD-002",
  "name": "Produk B",
  "price": 0,
  "stock": 10
}
```

**Expected: 400 Bad Request**

```json
{
  "errors": {
    "price": "Price must be greater than 0"
  }
}
```

---

### 1e. Stok negatif

```json
{
  "sku": "PROD-003",
  "name": "Produk C",
  "price": 5000,
  "stock": -1
}
```

**Expected: 400 Bad Request**

```json
{
  "errors": {
    "stock": "Stock must not be negative"
  }
}
```

---

### 1f. Buat beberapa produk untuk testing berikutnya

```json
{
  "sku": "PROD-002",
  "name": "Es Teh Manis",
  "price": 5000,
  "stock": 100
}
```

```json
{
  "sku": "PROD-003",
  "name": "Ayam Bakar",
  "price": 20000,
  "stock": 30
}
```

---

## 2. GET /api/products — Daftar Semua Produk

```
GET http://localhost:8081/api/products
```

**Expected: 200 OK**

```json
[
  {
    "id": 1,
    "sku": "PROD-001",
    "name": "Nasi Goreng Spesial",
    "price": 15000.00,
    "stock": 50,
    "status": "ACTIVE",
    "createdAt": "...",
    "updatedAt": "..."
  },
  {
    "id": 2,
    "sku": "PROD-002",
    "name": "Es Teh Manis",
    "price": 5000.00,
    "stock": 100,
    "status": "ACTIVE",
    "createdAt": "...",
    "updatedAt": "..."
  }
]
```

---

## 3. GET /api/products/{id} — Detail Produk

### 3a. ID valid

```
GET http://localhost:8081/api/products/1
```

**Expected: 200 OK** — data produk dengan id 1.

---

### 3b. ID tidak ditemukan

```
GET http://localhost:8081/api/products/999
```

**Expected: 404 Not Found**

```json
{
  "timestamp": "...",
  "status": 404,
  "error": "Not Found",
  "message": "Product not found with id: 999",
  "path": "/api/products/999"
}
```

---

## 4. PATCH /api/products/{id}/stock — Update Stok Manual

### 4a. Kurangi stok (DECREASE)

```
PATCH http://localhost:8081/api/products/1/stock
Content-Type: application/json
```

```json
{
  "quantity": 5,
  "operation": "DECREASE"
}
```

**Expected: 200 OK** — stok berkurang 5 (dari 50 → 45).

---

### 4b. Tambah stok (INCREASE)

```json
{
  "quantity": 10,
  "operation": "INCREASE"
}
```

**Expected: 200 OK** — stok bertambah 10.

---

### 4c. Kurangi melebihi stok tersedia

```json
{
  "quantity": 9999,
  "operation": "DECREASE"
}
```

**Expected: 400 Bad Request**

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Insufficient stock. Available: 45, requested: 9999"
}
```

---

### 4d. Operation tidak valid

```json
{
  "quantity": 5,
  "operation": "INVALID_OP"
}
```

**Expected: 400 Bad Request**

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid request body or unrecognized value"
}
```

---

### 4e. Quantity nol

```json
{
  "quantity": 0,
  "operation": "DECREASE"
}
```

**Expected: 400 Bad Request**

```json
{
  "errors": {
    "quantity": "Quantity must be greater than 0"
  }
}
```

---

## 5. PATCH /api/products/{id}/status — Ubah Status

### 5a. Ubah ke INACTIVE

```
PATCH http://localhost:8081/api/products/1/status
Content-Type: application/json
```

```json
{
  "status": "INACTIVE"
}
```

**Expected: 200 OK** — status berubah jadi `INACTIVE`.

---

### 5b. Ubah kembali ke ACTIVE

```json
{
  "status": "ACTIVE"
}
```

**Expected: 200 OK** — status kembali `ACTIVE`.

---

### 5c. Status tidak valid

```json
{
  "status": "DELETED"
}
```

**Expected: 400 Bad Request**

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid request body or unrecognized value"
}
```

---

## 6. PATCH /api/products/{id}/reduce-stock — Kurangi Stok (Internal)

> Endpoint ini dipanggil Order Service. Bisa juga ditest manual.

```
PATCH http://localhost:8081/api/products/1/reduce-stock
Content-Type: application/json
```

```json
{
  "quantity": 3
}
```

**Expected: 200 OK** (response body kosong) — stok berkurang 3.

---

### 6a. Kurangi melebihi stok

```json
{
  "quantity": 9999
}
```

**Expected: 400 Bad Request**

```json
{
  "message": "Insufficient stock. Available: ..., requested: 9999"
}
```

---

## 7. PATCH /api/products/{id}/restore-stock — Kembalikan Stok (Internal)

```
PATCH http://localhost:8081/api/products/1/restore-stock
Content-Type: application/json
```

```json
{
  "quantity": 3
}
```

**Expected: 200 OK** (response body kosong) — stok kembali bertambah 3.

---

## Urutan Testing yang Disarankan

```
1. POST   /api/products          → buat 3 produk (PROD-001, PROD-002, PROD-003)
2. POST   /api/products          → coba SKU duplikat → harus 409
3. POST   /api/products          → coba harga 0, stok -1 → harus 400
4. GET    /api/products          → pastikan 3 produk muncul
5. GET    /api/products/1        → detail produk pertama
6. GET    /api/products/999      → harus 404
7. PATCH  /api/products/1/stock  → DECREASE 5 → stok berkurang
8. PATCH  /api/products/1/stock  → DECREASE 9999 → harus 400
9. PATCH  /api/products/1/stock  → INCREASE 10 → stok bertambah
10. PATCH /api/products/1/status → INACTIVE
11. GET   /api/products/1        → pastikan status INACTIVE
12. PATCH /api/products/1/status → ACTIVE kembali
13. PATCH /api/products/1/reduce-stock  → quantity 3 → stok berkurang
14. PATCH /api/products/1/restore-stock → quantity 3 → stok kembali
```
