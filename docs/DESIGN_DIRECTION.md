# Tasarım Yönü — Quiet Intelligence

## Tasarım fikri

Arayüz “finans yazılımı” gibi yoğun değil, iyi tasarlanmış bir yönetim aracı gibi
sakin görünür. Karmaşıklık progressive disclosure ile gerektiğinde açılır.

## Görsel dil

- Geniş boşluk, güçlü tipografik hiyerarşi ve sınırlı vurgu rengi.
- Kart denizi yerine bağlama göre gruplanmış yüzeyler.
- Sayılar tabular numerals ile hizalanır.
- Grafikler dekor değil karar aracıdır; her grafiğin metinsel karşılığı vardır.
- Cam/blur yalnızca navigasyon ve geçici katmanlarda, performans bütçesi içinde.
- Kritik durumlar sadece renkle ifade edilmez; ikon, metin ve şekil eşlik eder.

## Tema ailesi

- Porcelain: sıcak, açık kurumsal tema.
- Obsidian: gerçek koyu tema; saf siyah yerine katmanlı nötrler.
- Contrast: WCAG odaklı yüksek kontrast tema.

Tema renk dışında elevation, separator, grafik paleti, belge zemini ve motion
yoğunluğunu da belirler. Sistem teması varsayılandır.

## Hareket sistemi

- Micro: 120–180 ms; buton, seçim ve küçük durum cevabı.
- Standard: 220–320 ms; panel, navigation ve içerik değişimi.
- Emphasis: 360–480 ms; yalnızca önemli başarı veya bağlam dönüşümü.
- Reduce Motion açıkken spatial hareket dissolve/crossfade ile değiştirilir.
- Haptik yalnızca onay, risk, seçim eşiği ve tarama yakalama anında kullanılır.

## Ana navigasyon

Varsayılan işletme sahibi görünümü:

1. Bugün
2. Belgeler
3. Tara (merkezi birincil eylem)
4. Finans
5. Çalışma Alanı

Rol değiştikçe sekme isimleri değil içerik önceliği değişir; kullanıcı her rolde
yeniden navigasyon öğrenmez.

