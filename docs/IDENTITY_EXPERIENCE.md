# Kimlik ve Onboarding Deneyimi

## Amaç

Kullanıcıya güven hissi verirken giriş süresini uzatmamak. Uygulama finansal veri
taşıdığı için hız, session güvenliğinin önüne geçmez.

## Akış

1. İlk açılışta kısa değer önerisi ve açık gizlilik mesajı.
2. Kurumsal e-posta + parola veya kuruluşça etkinleştirilen SSO.
3. Risk politikasına göre MFA/step-up challenge.
4. Birden fazla üyelik varsa organizasyon seçimi.
5. Kullanıcı izniyle Face ID/Touch ID veya Android biyometri etkinleştirme.
6. Sonraki açılışlarda biyometri yerel refresh credential erişimini açar.

## Olmaması gerekenler

- Sosyal kanıt carousel'i veya uzun özellik turu.
- E-posta var/yok bilgisini sızdıran hata metni.
- Ağ hatasında demo kullanıcıya sessiz geçiş.
- Biyometriyi parola yerine sunucu kimliği gibi kullanma.
- Kullanıcı kabul etmeden pazarlama izni seçme.

## Tasarım

- İlk ekranda tek güçlü mesaj ve iki eylem.
- Finansal güvenlik küçük metinlere saklanmaz.
- Form hata verdiğinde layout zıplamaz.
- Klavye, Dynamic Type ve ekran okuyucu ilk sınıf kullanıcıdır.
- Şirket seçimi kart kataloğu değil, son kullanım ve rol bağlamlı sade listedir.

