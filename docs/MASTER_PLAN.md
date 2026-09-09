# Ana Uygulama Planı

## Faz 0 — Temel kararlar

- Ürün anayasası, terminoloji ve kapsam.
- iOS/Android özellik paritesi sözleşmesi.
- Design token kaynağı ve tema sistemi.
- API, hata, audit ve AI açıklanabilirlik sözleşmeleri.
- Güvenlik ve KVKK tehdit modeli.

Çıkış ölçütü: İki ekip aynı ekranı bağımsız yorumlamadan uygulayabilmeli.

## Faz 1 — Premium uygulama kabuğu

- Güvenli onboarding, giriş, biyometri ve organizasyon seçimi.
- Ana navigasyon ve rol bazlı bilgi mimarisi.
- Açık, koyu ve yüksek kontrast tema.
- Türkçe ve İngilizce; RTL hazırlığı.
- Skeleton, boş, hata, offline ve izin durumları.
- Ortak hareket ve haptik sözlüğü.

Çıkış ölçütü: Her iki platformda aynı yetenekler, native davranış ve ölçülmüş akıcılık.

## Faz 2 — Belge yakalama ve doğrulama

- Kamera kalite koçu ve galeriden/PDF'den yükleme.
- Çok sayfalı tarama, otomatik kırpma ve sıkıştırma.
- Upload kuyruğu, devam ettirme ve idempotency.
- Alan bazlı OCR güveni ve belge üzerindeki kanıt bölgeleri.
- İnsan doğrulama ve değişiklik geçmişi.

Çıkış ölçütü: Ağ kesintisi ve uygulama kapanmasında belge kaybı olmaması.

## Faz 3 — Muhasebe çalışma alanı

- Belgeler, giderler, satışlar, araçlar ve personel masrafları.
- Gelişmiş arama, filtre, kayıtlı görünümler ve toplu işlemler.
- Belge yorumları, görevler ve SMMM açıklama talepleri.
- Excel/ZIP dışa aktarma ve paylaşım politikaları.

Çıkış ölçütü: Kaynak belgeye kadar izlenebilir her hesaplama.

## Faz 4 — Mali içgörü ve kapanış

- KDV ve nakit görünümü.
- Dönem kapanış kokpiti.
- Eksik düzenli belge, mükerrer ve anomali tespiti.
- Senaryo tabanlı vergi projeksiyonu; sonuçlar hukuki uyarı ve varsayımlarla gösterilir.

Çıkış ölçütü: Her içgörünün kanıtı, varsayımı ve kural sürümü bulunmalı.

## Faz 5 — Kurumsal ölçek

- Çoklu şirket SMMM portföyü.
- Gelişmiş RBAC/ABAC, dört göz prensibi ve yüksek tutarlı işlem onayı.
- SSO, cihaz politikaları, oturum yönetimi ve uzaktan çıkış.
- Entegrasyon merkezi ve açık API.
- Audit dışa aktarımı, veri saklama ve silme politikaları.

## Faz 6 — Uluslararasılaşma

- Ülke bazlı vergi paketleri ve belge şemaları.
- Para birimi, yerel formatlar ve ülke mevzuat sürümleri.
- RTL arayüz doğrulaması.
- Bölgesel veri yerleşimi ve yasal metin yönetimi.

## Sürekli kalite kapıları

- Unit, contract, snapshot, UI ve erişilebilirlik testleri.
- API geriye uyumluluk kontrolü.
- Mobil performans bütçeleri ve gerçek cihaz benchmarkları.
- Tehdit modelleme, SAST, bağımlılık ve secret tarama.
- Beta telemetrisi, çökme raporları ve kontrollü feature flag yayını.
- Platform parite denetimi geçmeden sürüm yayımlanmaz.

