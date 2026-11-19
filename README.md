# ChiTiêu

## Mốc hoàn thành: M1 → M8 (MVP)

| Mốc | Nội dung |
|-----|----------|
| M1 | Nền tảng |
| M2 | Giao dịch + Widget + Chuyển khoản |
| M3 | Ngân sách |
| M4 | Báo cáo |
| M5 | Phân loại thông minh |
| M6 | Nhập từ thông báo |
| M7 | Gợi ý (MAD, định kỳ, dự báo) |
| **M8** | **Sao lưu / CSV / khóa PIN** |

## M8 – Sao lưu & khóa app

### Sao lưu
- **Xuất JSON.gz** (schemaVersion = 1) gồm accounts, categories, transactions, budgets, recurring
- **Xuất CSV** giao dịch (mở Excel)
- **Khôi phục**: chọn file → **preview** (số giao dịch, khoảng ngày, schema) → xác nhận ghi đè
- File nằm trong `Android/data/.../files/backups/` và có thể chia sẻ

### Khóa app
- PIN 4–8 số, hash SHA-256 trong DataStore
- Hiện màn hình khóa khi mở app
- **Không mã hóa database** (đúng threat model MVP trong plan)

### Cài đặt
Tab **Cài đặt** → Xuất JSON / CSV / Khôi phục / Khóa PIN

## Mở project

Android Studio → Open `ChiTieu` → Sync → Run (minSdk 26)

## Ngoài MVP (sau)

- Google Drive App Data auto-backup
- Biometric
- SQLCipher / mã hóa DB
- LLM gợi ý ngôn ngữ tự nhiên
