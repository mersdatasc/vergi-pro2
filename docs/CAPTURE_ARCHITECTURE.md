# Akıllı Belge Yakalama

## Tasarım amacı

Kullanıcı fotoğrafçılık öğrenmeden muhasebeleştirilebilir kalitede belge üretir.
Uygulama sadece deklanşör sunmaz; çekim öncesi kaliteyi açıklar ve hatayı kaynağında
önler.

## İşlem hattı

```text
Camera frame
  → device-side quality analysis
  → user coaching
  → original capture
  → non-destructive crop/enhancement recipe
  → encrypted local draft
  → duplicate fingerprint
  → resumable upload
  → malware/file validation
  → OCR + classification
  → field-level evidence
  → user/SMMM verification
```

## Cihaz üzerinde yapılacaklar

- Dört belge köşesi ve kadraj doluluk oranı.
- Bulanıklık ve hareket tahmini.
- Parlama ve düşük ışık uyarısı.
- Görsel yönü ve temel kırpma önerisi.
- Sayfa parmak izi; hassas metin analytics'e gönderilmez.

## Sunucuda yapılacaklar

- MIME/magic byte, zararlı içerik ve dosya limiti doğrulaması.
- OCR, sınıflandırma ve alan çıkarımı.
- Alan bazlı güven ve bounding box kanıtı.
- Tenant kuralları, mükerrer kontrol ve anomali analizi.
- İnsan doğrulama kuyruğu ve audit kaydı.

## Veri kaybını önleme

- Her sayfa çekildiği anda şifreli taslağa yazılır.
- Upload uygulama kapanınca devam edebilir.
- Aynı idempotency key ile tekrar gönderim yeni belge yaratmaz.
- Kullanıcı taslağı silmeden orijinal dosya kaldırılmaz.
- Başarısız OCR, mali değerlerle doldurulmaz; `needs_verification` olur.
- Sayfa sırası ve kasa referansları tenant bazlı şifreli manifestte tutulur.
- Uygulama yeniden açıldığında manifest yüklenir ve her sayfanın SHA-256 değeri doğrulanır.
- Yeni kasa dosyası yazılıp manifest commit edilemezse dosya geri alınır; yetim kayıt bırakılmaz.
- Silme işleminde önce manifest atomik güncellenir, ardından şifreli binary kaldırılır.

## Güvenli yerel kasa ve yükleme

- Orijinal binary cihazda AES-256-GCM ile şifrelenir ve tenant dizininde tutulur.
- Şifreleme anahtarı iOS Keychain Data Protection veya Android Keystore tarafından korunur.
- Yazma atomiktir; SHA-256 bütünlüğü hem okuma sırasında hem sunucuda doğrulanır.
- Yükleme oturumu 5 MB varsayılan parçalar, parça checksum'u ve server-authoritative offset kullanır.
- Sunucudan geriye giden veya dosya sınırını aşan offset kabul edilmez.
- Yerel kaynak ancak sunucu tamamlamayı onayladıktan ve saklama politikası izin verdikten sonra silinir.

## Arka plan davranışı

- iOS `BGProcessingTaskRequest`, Android `WorkManager` kullanır.
- İşler upload UUID ile unique planlanır; aynı belge için yinelenen worker oluşmaz.
- Ağ, düşük pil ve düşük depolama koşulları işletim sistemi tarafından gözetilir.
- Kullanıcının yalnız Wi-Fi tercihi Android'de unmetered network constraint'e çevrilir.
- Normal senkronizasyon görünmezdir; yalnız offline bekleme, aktif yükleme veya müdahale gereken durum gösterilir.

## Taslaktan submission'a geçiş

- Çok sayfalı taslak tek submission UUID ve tek idempotency kapsamı altında paketlenir.
- Kuyruk yalnız şifreli kasa referanslarını taşır; belge binary'si kuyruk payload'ına kopyalanmaz.
- Önce kalıcı çevrimdışı kuyruk commit edilir, ardından taslak düzenlemeye kilitlenir.
- Kuyruk commit başarısızsa taslak açık ve düzenlenebilir kalır.
- Commit sonrasında sayfa ekleme, silme ve yinelenen gönderim engellenir.
- Sunucu onayı gelene kadar şifreli taslak ve manifest cihazda korunur.
- Submission kilidi manifestin parçasıdır ve uygulama yeniden başlatıldığında geri yüklenir.
- Submission UUID aynı zamanda idempotency key'dir; yarım kalan prepare işlemi aynı kimlikle tamamlanır.
- Uygulama manifest commit ile kuyruk commit arasında kapanırsa açılış uzlaştırması kuyruğu tekrar güvenle yazar.
- Eski v1 manifestleri submission kilidi olmadan okunup v2 biçimine taşınabilir.

## Kalıcı submission durum makinesi

- Kuyruk durumu pending, running, uploading, processing, needs verification, needs attention ve completed aşamalarını taşır.
- Yükleme ilerlemesi 0...1 aralığındadır ve geriye gidemez.
- Geçersiz durum sıçramaları istemci tarafından reddedilir ve kalıcı depoya yazılmaz.
- Sunucunun document ID'si processing aşamasında kuyruk kaydına bağlanabilir.
- Kuyruk formatı v2'dir; mevcut v1 kayıtları progress ve server ID olmadan okunmaya devam eder.

## UX kuralları

- Aynı anda yalnızca en önemli kalite önerisi gösterilir.
- “Kötü çekim” yerine eylem söylenir: “Biraz yaklaşın”.
- Otomatik çekim yalnızca belge sabit ve tüm köşeler görünürken yapılır.
- Haptik geri bildirim hazır olma ve çekim anında farklıdır.
- Reduce Motion ve ekran okuyucu için görsel koçun metinsel karşılığı bulunur.
