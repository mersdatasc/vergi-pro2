# Veri ve Senkronizasyon Mimarisi

## Katmanlar

```text
View → ViewModel → Use Case → Repository
                              ├── Remote data source
                              ├── Local encrypted database
                              ├── Secure credential store
                              └── Sync queue
```

UI; URLSession, OkHttp, SQLite, Keychain veya Keystore hakkında bilgi sahibi olmaz.
Preview verisi ayrı repository implementasyonudur ve production build'e bağlanmaz.

## Oturum

- Access token yalnız bellekte tutulur.
- Rotasyonlu refresh token güvenli donanım destekli depoda saklanır.
- Uygulama açıldığında refresh token ile yeni access token alınır.
- Refresh reuse tespitinde tüm cihaz oturumu iptal edilir.
- Biyometri refresh credential erişimini açabilir; sunucu kimliği yerine geçmez.
- Kullanıcı aktif cihazlarını görüp uzaktan kapatabilir.

## Tenant bağlamı

- Her repository çağrısı açık `OrganizationContext` alır.
- Organizasyon kimliği yalnız header'a yazılmaz; token yetkisiyle sunucuda doğrulanır.
- Organizasyon değişiminde memory cache ve hassas ekran durumu temizlenir.
- Offline kuyruk öğeleri tenant kimliğiyle ayrılır ve tenantlar arasında taşınmaz.

## Offline mutation kuyruğu

- Her mutation UUID idempotency key taşır.
- Aynı aggregate üzerindeki işlemler sıralı yürütülür.
- Geçici ağ/sunucu hatası exponential backoff + jitter ile denenir.
- Yetki/validation hatası tekrar edilmez; kullanıcıya eylem gerekir durumu gösterilir.
- Maksimum denemeden sonra dead-letter öğesi sessizce kaybolmaz.
- Belge upload'u resumable session kimliği ve checksum taşır.
- Kuyruk uygulama kapanışları arasında şifreli olarak korunur; iOS Data Protection Keychain,
  Android ise hardware-backed AES-GCM anahtarı kullanır.
- Büyük belge binary'leri kuyruğa gömülmez. Kuyruk en fazla 1 MB metadata/payload taşır ve
  şifreli dosya referansını saklar.

## Hata deneyimi

- UI ham HTTP kodu veya backend mesajı göstermez.
- Hata kodu yerelleştirilmiş, eyleme dönük metne eşlenir.
- Request ID destek ekranına kopyalanabilir.
- Ağ yokluğu, kimlik hatası ve sunucu bakımı farklı durumlar olarak ele alınır.

## Özellik repository'leri

- Belgeler, finans ve çalışma alanı tenant-scope repository sözleşmeleri kullanır.
- API payload'ları View katmanına ulaşmadan domain modellerine dönüştürülür.
- Para değerleri signed minor unit olarak taşınır; kayan noktalı sayı kullanılmaz.
- Sayfalama cursor tabanlıdır ve bir istekte en fazla 100 kayıt alınır.
- Durum akışı `idle → loading → content/empty/failure` biçimindedir.
- Yenileme hatasında son başarılı içerik korunur ve gerektiğinde stale olarak işaretlenir.
- İptal edilen işler kullanıcıya hata olarak sunulmaz; belge araması 300 ms debounce kullanır.

## Uygulama bootstrap

Uygulama açılışında tenant verisi içermeyen bootstrap endpoint'i çağrılır. API uyumluluğu,
servis durumu, minimum uygulama sürümü ve desteklenen yetenekler doğrulanır. Yetenek bayrakları
yalnız görünürlük/uyumluluk içindir; hiçbir zaman sunucu yetkilendirmesinin yerine geçmez.
Bakım modunda mutation işlemleri durdurulur, degraded durumda güvenli okuma akışları korunur.
