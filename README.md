# VergiPro Mobile (iOS & Android) — Yeni Nesil Mobil Mimari

VergiPro Mobile, vergi.pro web uygulamasının Swiss/Linear minimalist tasarım dilini ve muhasebe/vergi radar özelliklerini native iOS (Swift 6 + SwiftUI) ve native Android (Jetpack Compose + Material3) platformlarına taşıyan kurumsal mobil uygulamadır.

> **Geliştirme durumu:** Canlı backend bağlantısı, kimlik doğrulama, şirket seçimi, belge yükleme, finans, çalışma alanı ve güvenli yerel taslak akışları bağlıdır. Mağaza yayını öncesinde tüm ekranların iki dilde tamamlanması, kesintiden sonra arka planda otomatik yükleme ve gerçek hesaplarla uçtan uca regresyon testleri sürmektedir. Bu depo henüz “mağazaya hazır” olarak değerlendirilmemelidir.

---

## 🎨 1. Tasarım Dili ve Marka Uyumu (Web ile %100 Birebir)

- **Canvas / Zemin:** `#F8FAFC` (Slate-50 porcelain)
- **Kartlar / Surface:** `#FFFFFF` (Saf beyaz, 14-18dp yuvarlatılmış köşeler)
- **Bordürler:** `#E2E8F0` (Hafif ince ayrım çizgisi)
- **Birincil Marka / Metin:** `#0F172A` (Derin kurumsal arduvaz / Slate-900)
- **Vurgu / Aksan:** `#2563EB` (Linear kurumsal mavi)
- **Başarı / Pozitif:** `#059669` (Zümrüt yeşili — Devreden KDV, onaylı belgeler, komisyon kazancı)
- **Uyarı:** `#D97706` (Amber sarısı — İnceleme bekleyen belgeler, yaklaşan beyanname)
- **Kritik / Risk:** `#DC2626` (Yakut kırmızısı — Ödenecek KDV 1 farkı, GVK 40/1 aşımı)
- **Tipografi:** Rakamlar ve tutarlar için `Monospace / monospacedDigit()`
- **Resmi Logolar:**
  - `vergipro-light-2.png` -> Açık tema / app icon
  - `vergipro-dark-2.png` -> Koyu tema / dark icon

---

## ⚡ 2. Eklenen ve Web ile Birebir Eşitlenen Temel Özellikler

### 1. Canlı KDV & Vergi Radarı (Patron Masası)
- **KDV 1 Dengesi:** 391 Hesaplanan KDV vs 191 İndirilecek KDV canlı farkı.
- **GVK 40/1 Binek Araç Akaryakıt Koruması:** Akaryakıt ve araç harcamalarında %70 indirilebilir gider vs %30 Kanunen Kabul Edilmeyen Gider (KKEG) ayrımı.
- **Dönem Sonu Hız & Run-Rate Projeksiyonu:** Günlük harcama hızı (`daily_velocity`) ile ay sonu tahmini ödenecek KDV tahmini.
- **Vergi Riski Skoru:** 0-100 ölçeğinde risk ve denetim uyarısı.
- **İnteraktif Vergi Simülasyonu:** Kullanıcının ek harcama simüle ederek KDV avantajını anlık görebileceği slider kontrolü.

### 2. Saha Masraf Toplama & Ay Sonu Radarı
- **25'i Geri Sayımı:** Ayın 25'i KDV 1 beyanname son teslim gününe kalan gün sayacı.
- **1-Tıkla Ekibe Hatırlat:** Tek butonla saha personeline e-posta ve Telegram bildirim şablonu gönderme.
- **Telegram Entegrasyonu:** `@VergiProBot` katılım kodu ve 1-tıkla panoya kopyalama.
- **Hızlı Belge Yakalama:** Kamera ve AI ile fiş/fatura tarama kısayolu.

### 3. Mali Müşavir İhracat Masası (Luca & Zirve)
- **Luca Muhasebe:** `.xlsx` formatında Luca uyumlu 100/191/770/320 hesap kodlu fiş listesi aktarımı.
- **Zirve Muhasebe:** `.xml` ve `.xls` formatında tek tıkla aktarım.
- **5 Sayfalı Master İcmal:** Özet, Satışlar, Giderler, Araç GVK 40/1 ve TDHP Fişleri sayfaları.
- **ZIP Belge Arşivi:** Ayın tüm onaylı PDF ve fiş görsellerini klasörlenmiş ZIP olarak indirme/paylaşma.
- **Tek Düzen Hesap Planı (TDHP) Canlı Önizleme:** Mahsup fişi borç/alacak denge modalı.

### 4. Mali Müşavir Portföyü & Kademeli Gelir Masası (SMMM Partner)
- **Mükellef Portföy Masası:** Mali müşavirin tüm mükellef şirketlerini listeleyip anlık belge durumu, onay bekleyen sayısı ve net KDV durumunu görerek tek tıkla şirket değiştirme.
- **Kademeli Gelir Modeli (%10 – %20):**
  - `1–10 Şirket`: %10 Komisyon
  - `11–25 Şirket`: %12 Komisyon
  - `26–50 Şirket`: %15 Komisyon
  - `51–100 Şirket`: %18 Komisyon
  - `100+ Şirket`: %20 Komisyon
- **Mükellef Davet Bağlantısı:** Özel referral linki ve 1-tıkla kopyalama.
- **Aylık Düzenli Ödeme Takvimi:** Her ayın 15'inde kayıtlı banka IBAN hesabına otomatik aktarım notu.

---

## 🛠️ 3. Mac Ortamında Projeyi Çalıştırma

### iOS (Xcode)
```bash
cd ios
# Proje dosyasını açın:
open VergiPro.xcodeproj
```
- **Hedef:** iOS 17.0+
- **Dil:** Swift 6 / SwiftUI
- Simülatör seçin (`iPhone 16 Pro` vb.) ve `Cmd + R` ile çalıştırın.

### Android (Android Studio)
```bash
cd android
# Android Studio ile projeyi açın:
open -a "Android Studio" .
```
- **Hedef:** Android 8.0+ (API 26+) — Hedef SDK: 35
- **Dil:** Kotlin + Jetpack Compose (Material3)
- Simülatör veya fiziksel cihaz seçip `Shift + F10` (Run) yapın.
