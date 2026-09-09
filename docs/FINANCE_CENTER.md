# Finans Merkezi

## Amaç

Finans Merkezi bir grafik vitrini değil, karar ve mutabakat yüzeyidir. Her metrik
kaynak belge sayısını, veri tamamlık seviyesini ve hesaplama zamanını taşır.

## Ana bölümler

- Dönem özeti: gelir, gider, KDV dengesi ve kapanış hazırlığı.
- KDV akışı: hesaplanan, indirilecek ve net bakiye.
- Gelir/gider trendi: dönem karşılaştırması ve açıklanabilir sapmalar.
- Araç giderleri: toplam, kabul edilen gider ve KKEG ayrımı.
- Vergi projeksiyonu: senaryolar, varsayımlar ve kural sürümü.
- Kaynaklara inme: metrikten filtrelenmiş belge listesine geçiş.

## Güvenlik ve doğruluk

- Para `Double/Float` ile tutulmaz; minor unit veya Decimal kullanılır.
- Eksik belgeler projeksiyon kartında görünür etki olarak işaretlenir.
- Veri eskiyse sessizce gösterilmez; son güncelleme ve stale uyarısı bulunur.
- “Ödenecek vergi” yerine “mevcut verilere göre tahmini yük” dili kullanılır.
- SMMM doğrulaması ayrı bir güven seviyesi olarak gösterilir.

## Akıllı içgörüler

- Değişimin yalnız yüzdesi değil olası nedeni sunulur.
- Düzenli ancak gelmeyen belge varsa gider/KDV tahmini aralık olarak gösterilir.
- Olağandışı değişim, kaynak belge kümeleriyle açıklanır.
- Kullanıcı içgörüden tek dokunuşla ilgili belgelere ulaşır.

