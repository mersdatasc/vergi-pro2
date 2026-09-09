# Güvenlik ve Gizlilik Tabanı

- Tokenlar Keychain/Android Keystore destekli güvenli depoda saklanır.
- Access token kısa ömürlü, refresh token rotasyonlu ve cihaz oturumuna bağlıdır.
- Biyometri tek başına sunucu kimliği değildir; yerel secret erişimini açar.
- Tenant izolasyonu istemciye güvenmeden sunucuda uygulanır.
- Hassas alanlar log, analytics, crash payload ve push metnine yazılmaz.
- Yüklemeler boyut, MIME, magic bytes, zararlı içerik ve sayfa sayısı açısından doğrulanır.
- Dosyalar aktarımda ve depoda şifrelenir; erişim süreli URL ile sağlanır.
- Yüksek riskli eylemlerde step-up authentication ve dört göz prensibi desteklenir.
- Audit olayları append-only, zaman damgalı ve actor/device bağlamlıdır.
- Ekran görüntüsü engelleme platform politikasına uygun, risk bazlı uygulanır.
- Veri dışa aktarma, hesap kapatma, saklama ve silme akışları ürünün parçasıdır.
- AI sağlayıcısına gönderilen veri minimize edilir ve tenant politikasına tabidir.

İlk tehdit modeli: hesap ele geçirme, IDOR/tenant geçişi, zararlı dosya, tekrar
yükleme, token çalma, cihaz kaybı, hassas log, prompt injection, yanlış OCR,
yetkisiz dışa aktarma ve içeriden kötüye kullanım.

