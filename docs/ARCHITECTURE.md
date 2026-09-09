# Mobil Mimari

## Yaklaşım

Her platform Clean Architecture sınırlarını ve tek yönlü veri akışını uygular.
Görsel katman native kalır. Paylaşılacak domain ve senkronizasyon çekirdeği sonraki
aşamada Kotlin Multiplatform modülüne taşınmaya hazır sınırlar içinde tutulur.

```text
Presentation → Application/Use Cases → Domain ← Data
                                           ↑
                         API · Local DB · Secure Store · Device
```

## Modüller

- AppShell: başlangıç, environment, feature flag, deep link.
- Identity: oturum, biyometri, cihaz ve organizasyon bağlamı.
- Capture: kamera, kalite analizi, belge taslağı ve upload kuyruğu.
- Documents: liste, detay, doğrulama, yorum ve geçmiş.
- Finance: KDV, nakit, gider ve projeksiyon görünümleri.
- Closing: dönem kontrol listesi, eksikler ve SMMM teslimi.
- Collaboration: görevler, açıklamalar ve bildirim tercihleri.
- Organization: ekip, roller, araçlar, abonelik ve ayarlar.
- PlatformOps: staff ve super-admin işlevleri; ayrı yetki sınırları.

## Veri ilkeleri

- Para `Double` ile tutulmaz; para birimi + minor unit/decimal kullanılır.
- Tüm mutation isteklerinde idempotency key bulunur.
- Silme yerel optimistik başarı olarak gösterilmez; sunucu sonucu beklenir.
- Offline mutation kuyruğu sıralı, tekrar çalıştırılabilir ve gözlenebilirdir.
- Tenant bağlamı her istekte zorunlu ve token yetkisiyle sunucuda doğrulanır.
- AI çıktısı kaynak veri değil öneridir; doğrulama durumu ayrı tutulur.

## Performans bütçesi

- Animasyon hedefi: 60 FPS, ProMotion cihazlarda 120 Hz'e uyum.
- Ana thread üzerinde ağ, disk, OCR veya büyük JSON dönüşümü yapılmaz.
- Liste satırlarında pahalı blur/gölge ve sınırsız animasyon kullanılmaz.
- Büyük belgeler kademeli yüklenir; thumbnail ve tam çözünürlük ayrılır.
- Soğuk açılış, scroll ve kamera akışı gerçek cihazlarda ölçülür.

