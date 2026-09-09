# Belgeler Merkezi

## Amaç

Belgeler Merkezi arşiv değil, dönem kapanışına giden aktif çalışma alanıdır. Kullanıcı
dosya adı aramak yerine hangi belgenin neden ilgisini beklediğini görür.

## Bilgi mimarisi

- Üst seviye arama ve dönem bağlamı.
- Rol bazlı kayıtlı görünümler.
- Tek satırda belge türü, tedarikçi, tarih, tutar, durum ve risk.
- Detayda kaynak belge, mali alanlar, yorumlar, görevler ve audit geçmişi.
- SMMM açıklama talebi belge ve alan bağlamını kaybetmez.

## Akıllı önceliklendirme

Sıralama yalnız tarihe göre yapılmaz. `needs_attention` görünümü şu sinyalleri
birleştirir:

1. Düşük güvenli kritik alan
2. KDV/matematik tutarsızlığı
3. Yaklaşan dönem kapanışı
4. SMMM açıklama talebi
5. Olağandışı tedarikçi veya tutar
6. Uzun süredir bekleyen görev

Öncelik skoru kullanıcıya gizli kara kutu değildir; “neden üstte?” açıklaması bulunur.

## Mobil davranış

- Filtreler ayrı karmaşık sayfa yerine bottom sheet içinde açılır.
- Kayıtlı görünümler yatay chip sırası; ekran okuyucuda liste olarak sunulur.
- Liste satırı swipe ile yıkıcı işlem yapmaz.
- Detay ekranı kaynağa, alanlara ve geçmişe kademeli erişim verir.
- Tutar ve VKN bildirim önizlemelerinde varsayılan olarak maskelenir.

