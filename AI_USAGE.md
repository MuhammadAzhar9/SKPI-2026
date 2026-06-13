# AI_USAGE.md

Isi file ini jika menggunakan AI secara signifikan selama pengerjaan.

## Tool AI yang Digunakan

Contoh:

- ChatGPT
- Claude
- Gemini
- GitHub Copilot
- Cursor
- Codeium
- Lainnya:

## Bagian yang Dibantu AI

Tuliskan bagian mana saja yang dibantu AI.

Contoh:

- Membantu memahami error Spring Boot.
- Membantu membuat contoh DTO validation.
- Membantu membuat GlobalExceptionHandler.
- Membantu membuat RestClient/WebClient untuk komunikasi antarservice.
- Membantu refactor kode agar lebih rapi.

## Prompt Penting yang Digunakan

Tempelkan beberapa prompt penting.

```text
Contoh prompt:
Saya membuat order-service Spring Boot. Bagaimana cara memanggil catalog-service menggunakan RestClient dan menangani error 404?
```

## Modifikasi yang Dilakukan Sendiri

Jelaskan perubahan yang dilakukan setelah menerima jawaban AI.

Contoh:

- Menyesuaikan nama package.
- Menyesuaikan endpoint sesuai requirement.
- Mengubah business rule stok.
- Menguji ulang menggunakan Postman.

## Bagian yang Sudah Dipahami

Tuliskan secara singkat apa yang sudah dipahami.

Contoh:

- Saya memahami perbedaan Entity dan DTO.
- Saya memahami order-service tidak boleh query langsung ke database catalog.
- Saya memahami create order harus mengurangi stok melalui API catalog-service.

## Bagian yang Masih Membingungkan

Tuliskan jika masih ada bagian yang belum dipahami.

Contoh:

- Saya masih belum sepenuhnya memahami transaction antarservice.