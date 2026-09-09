# VergiPro Mobile — Agent Handoff & Developer Architecture Guide

> **Hedef:** Bu doküman, VergiPro'nun yerel mobil uygulamalarını (iOS SwiftUI ve Android Jetpack Compose) devralacak mühendis veya yapay zeka ajanları için eksiksiz mimari referans, tasarım kuralları, bilinen tuzaklar ve yol haritasıdır.

---

## 1. Proje Özeti & Vizyon
VergiPro, Türkiye'deki KOBİ'ler, şahıs şirketleri ve Mali Müşavirler (SMMM) için geliştirilmiş, ön muhasebeyi resmi muhasebe entegrasyonuyla bağlayan yeni nesil bir finansal operasyon katmanıdır.

Mobil uygulamalar (**iOS SwiftUI** ve **Android Jetpack Compose**), masaüstü web uygulamasının tüm temel yeteneklerini sahaya taşır:
1. **Canlı KDV & Vergi Radarı:** 391 (Hesaplanan KDV) vs 191 (İndirilecek KDV) dengesi, GVK 40/1 binek araç %70 gider / %30 KKEG kısıtlaması, anlık simülasyon ve projeksiyon.
2. **Saha Belge Toplama & OCR:** Fiş, fatura, akaryakıt belgelerini kameradan/galeriden çoklu tarama, çevrimdışı kuyruk ve anlık ayrıştırma.
3. **Mali Müşavir İhracat Masası:** Luca ve Zirve formatında resmi muhasebe yevmiye fişi önizleme ve anlık XML/Excel dışa aktarım.
4. **Kapanış Odası & Hatırlatıcılar:** Ay sonu kontrol listesi, eksik belge takibi, WhatsApp / Telegram / SMS hatırlatma tetikleyicisi.
5. **Çalışma Alanı (Workspace):** Çoklu şirket/tenancy yönetimi, SMMM portföy konsolu, gelir ortaklığı (affiliate) modeli, araç filosu yönetimi.

---

## 2. Dizin Yapısı

```
mobile/mobile-new/
├── AGENT_HANDOFF.md                    <-- Bu dosya (tüm ajanlar için rehber)
├── README.md                           <-- Hızlı kurulum ve özet
├── ios/                                <-- iOS Projesi (SwiftUI, iOS 16+)
│   ├── project.yml                     <-- XcodeGen proje konfigürasyonu
│   └── VergiPro/
│       ├── App/
│       │   └── VergiProApp.swift       <-- Ana uygulama girişi & lifecycle
│       ├── Core/
│       │   ├── DesignSystem/
│       │   │   └── VPTokens.swift      <-- Renkler, tipografi, TRY formatlayıcıları
│       │   ├── Networking/             <-- API istemcisi, endpoint'ler
│       │   ├── Security/               <-- Keychain token saklama, şifreli vault
│       │   ├── Sync/                   <-- Çevrimdışı mutasyon kuyruğu
│       │   └── Data/                   <-- Feature repository'leri, durum modelleri
│       ├── Features/
│       │   ├── Identity/               <-- Giriş, Kayıt, OTP, Şirket Seçimi
│       │   ├── Home/                   <-- Dashboard, KPI kartları, bildirimler
│       │   ├── Finance/                <-- Canlı KDV Radarı, Luca/Zirve Masası, Hatırlatıcı
│       │   ├── Documents/              <-- Belge merkezi, filtreleme, detay dialogları
│       │   ├── Capture/                <-- Kamera, çoklu fiş tarama, Vision OCR
│       │   └── Workspace/              <-- Çalışma alanı, SMMM konsolu, filo, affiliate
│       └── Resources/
│           ├── Assets.xcassets/
│           │   ├── AppIcon.appiconset  <-- 1024x1024 OLED siyah zeminli resmi logo
│           │   └── VPMark.imageset     <-- Şeffaf vektörel beyaz & siyah amblem
│           └── Info.plist
│
└── android/                            <-- Android Projesi (Jetpack Compose, SDK 26+)
    ├── build.gradle.kts
    ├── settings.gradle.kts
    └── app/
        └── src/main/
            ├── java/com/vergipro/mobile/
            │   ├── core/
            │   │   ├── designsystem/
            │   │   │   └── Theme.kt    <-- Obsidian renk paleti & TRY extension'ları
            │   │   ├── navigation/     <-- VergiProApp scaffold ve tab bar
            │   │   ├── network/        <-- OkHttp / Retrofit API temeli
            │   │   ├── security/       <-- Android Keystore şifreleme
            │   │   ├── sync/           <-- Çevrimdışı yükleme kuyruğu
            │   │   └── data/           <-- Veri modelleri ve repository'ler
            │   └── feature/
            │       ├── identity/       <-- IdentityFlow, ekranlar, viewmodel
            │       ├── home/           <-- HomeScreen, KPI kartları
            │       ├── finance/        <-- FinanceCenterScreen, TaxRadarCard, LucaZirve
            │       ├── documents/      <-- DocumentsCenterScreen, filtreler
            │       ├── capture/        <-- CaptureScreen, galeri/kamera picker
            │       └── workspace/      <-- WorkspaceScreen, filo, portföy, ekip
            └── res/
                ├── values/icon_colors.xml
                ├── drawable-*/ic_launcher_foreground.png
                └── mipmap-*/ic_launcher.png / ic_launcher_round.png
```

---

## 3. Kesin Tasarım ve Kodlama Kuralları (KIRILMAZ İLKELER)

Gelecekte projeye dokunacak herhangi bir agent aşağıdaki kurallara **istisnasız** uymak zorundadır:

### 1. Renk Paleti (Saf OLED Siyahı — Slate/Lacivert YASAKTIR)
* **Canvas / Arkaplan:** Saf OLED siyahıdır (`#000000` / `UIColor.black`).
* **Yüzey / Kartlar:** Derin koyu gridir (`#121214`).
* **Kenarlıklar:** İnce yarı saydam zinc/beyazdır (`Color.white.opacity(0.08)` veya `#27272A`).
* **Metinler:** Birincil `#FAFAFA`, ikincil zinc `#A1A1AA` veya `#71717A`.
* ⚠️ **ASLA** lacivert/slate (`#0F172A`, `#1E293B`) arkaplan kullanmayın. Kullanıcı bu tonları kesin olarak reddetmiştir.

### 2. Para Formatı (Darwin Printf Bug'ı Engellendi)
* ⚠️ **ASLA `String(format: "₺%,.2f", amount)` veya `String(format: "₺%,.0f", amount)` KULLANMAYIN!**
  Apple Darwin Foundation `printf` POSIX `,` flag'ini tanımaz ve ekrana literally **`₺,.0f`** veya **`₺,.2f`** yazdırır!
* **iOS Çözümü:** Daima `VPTokens.swift` içindeki `.formattedTRY` veya `.formattedTRYCompact` property'lerini kullanın.
  ```swift
  Text(amount.formattedTRY)         // ₺42.911,31
  Text(amount.formattedTRYCompact)  // ₺288.000
  ```
* **Android Çözümü:** Daima `Theme.kt` içindeki `.formattedTRY()` veya `.formattedTRYCompact()` extension fonksiyonlarını kullanın.
  ```kotlin
  Text(amount.formattedTRY())       // ₺42.911,31
  Text(amount.formattedTRYCompact())// ₺288.000
  ```

### 3. Kimlik Doğrulama & Çoklu Şirket (Kullanıcıyı Hapsetmeme)
* Uygulama açılışında kayıtlı oturum kontrol edilir (`restoreSessionIfAvailable`).
* Birden fazla şirketi olan veya şirket seçimine düşen kullanıcı **kesinlikle hapsedilmemelidir**.
* `OrganizationSelectionView` / `OrganizationSelectionScreen` üzerinde daima:
  - **"Farklı Hesapla Giriş Yap"** (mevcut oturumu siler, giriş ekranına atar)
  - **"Yeni Şirket veya Hesap Oluştur"** (kayıt akışına atar)
  - Sağ üstte **"Çıkış Yap"** butonu bulunmalıdır.

### 4. İkon Standartları
* iOS SF Symbols kullanırken yalnızca **iOS 16+ geçerli semboller** kullanılmalıdır.
  - Örneğin `"radar"` geçerli bir SF Symbol değildir, yerine `"chart.line.uptrend.xyaxis"` kullanılır.
* Koyu temada beyaz ikonlar beyaz zemin üzerine konulmamalıdır; `VPColor.brand.opacity(0.12)` gibi zıtlık sağlayan yüzeyler kullanılmalıdır.
* AppIcon dosyaları 1024x1024 RGB `#000000` zemin üzerine ortalanmış %55 oranlı resmi beyaz markayı barındırmalıdır. Asla önceden yuvarlatılmış veya 3D plastik efektli ikonlar koymayın.

---

## 4. API ve Backend Entegrasyonu

Backend `http://localhost:3001` (sunucuda) veya `https://vergi.pro/api` adresinde çalışmaktadır.

| Endpoint | Metot | Açıklama |
|---|---|---|
| `/api/auth/send-verification-code` | POST | E-posta OTP kodu gönderimi |
| `/api/auth/verify-code` | POST | 6 haneli OTP kodu doğrulaması |
| `/api/auth/register` | POST | Yeni kullanıcı & şirket kaydı |
| `/api/auth/login` | POST | E-posta & parola ile oturum açma |
| `/api/auth/refresh` | POST | Refresh token rotasyonu |
| `/api/auth/me` | GET | Kullanıcı profili & şirket listesi |
| `/api/dashboard` | GET | Anlık ciro, gider, KDV, belge sayıları |
| `/api/tax-radar` | GET | Canlı KDV Radarı (391 vs 191, GVK 40/1, simülasyon) |
| `/api/reminders/status` | GET | Ay sonu hatırlatma bildirim durumu |
| `/api/reminders/trigger` | POST | WhatsApp/Telegram/SMS hatırlatıcı tetikleme |
| `/api/export/luca` & `/api/export/zirve` | GET/POST | Resmi muhasebe yevmiye fişi ihracatı |
| `/api/documents/upload` | POST | Çoklu fiş/fatura fotoğrafı yükleme & OCR |

---

## 5. Uygulamaları Derleme & Test Etme

### iOS (macOS & Xcode)
1. Mac terminalinden projeyi indirin:
   ```bash
   rsync -avzP root@188.132.138.163:/var/www/vergi.pro/public_html/vergipro-main-2/mobile/mobile-new/ios/ ./ios/
   ```
2. Projeyi Xcode ile açın:
   - Eğer `VergiPro.xcodeproj` mevcutsa doğrudan çift tıklayıp açın.
   - Proje dosyasını yeniden oluşturmak isterseniz XcodeGen kurulu bir ortamda `xcodegen generate` çalıştırabilirsiniz.
3. Target: `VergiPro`, Minimum Deployment Target: `iOS 16.0`.
4. `Cmd + R` ile iPhone 15/16 Pro Simulator'da derleyip çalıştırın.

### Android (Android Studio)
1. Mac veya PC terminalinden projeyi indirin:
   ```bash
   rsync -avzP root@188.132.138.163:/var/www/vergi.pro/public_html/vergipro-main-2/mobile/mobile-new/android/ ./android/
   ```
2. Android Studio'yu açıp `android` klasörünü "Open Existing Project" ile seçin.
3. Gradle senkronizasyonunun tamamlanmasını bekleyin (Java 17 / Kotlin 1.9+).
4. Bir Android Emülatör (API 34/35) seçip `Run 'app'` düğmesine basın.

---

## 6. Sırada Bekleyen İyileştirmeler (Gelecek Ajan İçin Notlar)
1. **Push Notifications (APNs & FCM):** Ay sonu KDV radarı kritik eşiği aştığında (örneğin ödenecek KDV > 50.000 ₺) arka plan bildirimleri için payload desteği eklenmesi.
2. **Biometric Auth (Face ID & Touch ID):** Keychain üzerinde saklanan refresh token'ın Face ID onayı ile açılması seçeneği.
3. **Canlı Kamera Kenar Algılama (CoreImage & MLKit):** Fiş çekiminde belgenin dört köşesini otomatik tespit edip perspektif düzeltme (deskew) uygulaması.
