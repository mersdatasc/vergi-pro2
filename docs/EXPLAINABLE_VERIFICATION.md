# Açıklanabilir Belge Doğrulama

## İlke

AI sonucu bir öneridir. Kullanıcı her alanın kaynağını, güven seviyesini ve değişiklik
geçmişini görebilir. “%92 doğru” gibi tek bir belge skoru finansal doğruluk için
yeterli değildir.

## Alan modeli

Her çıkarılan alan şunları taşır:

- Alan kimliği ve yerelleştirilmiş etiketi
- Tip: metin, tarih, para, oran, kimlik veya enum
- Normalize değer ve ekranda gösterim değeri
- Güven seviyesi
- Durum: çıkarıldı, düşük güven, düzeltildi, SMMM doğruladı
- Kaynak sayfa ve normalize bounding box
- Kaynak üzerindeki ham metin
- AI değeri, güncel değer ve değişiklik geçmişi

## İnceleme sırası

1. Düşük güvenli kritik alanlar
2. Tutar/KDV matematiği uyuşmayan alanlar
3. Tenant geçmişinden sapan tedarikçi/vergi oranı
4. Diğer düşük güvenli alanlar
5. Yüksek güvenli ve kuralla doğrulanan alanlar

## Kullanıcı deneyimi

- Alan seçildiğinde ilgili belge bölgesi vurgulanır.
- Tutarlar tabular numerals ile hizalanır.
- Güven yalnız renkle değil metin ve ikonla anlatılır.
- Düzeltme sırasında orijinal AI değeri kaybolmaz.
- Onay düğmesi yalnız zorunlu alanlar ele alındığında etkinleşir.
- Başarısız işlem yeniden denenebilir; belge tekrar yüklenmek zorunda kalmaz.

