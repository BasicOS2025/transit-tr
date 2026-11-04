# Transit TR 0.6 — EGO tarayıcı entegrasyonu

04.11.2026

- EGO'nun https://www.ego.gov.tr/otobusnerede sayfası bulut tarayıcısında açıldı. 10001 numaralı durak normal form ve düğme ile sorgulandı. Sonuç ekranında 591-1 ve 526 hatları ile tahmini süreler görüldü. Bu tek sorgu gözlemidir; süreler anlık değişir.
- Arka plan Chromium servisi eklendi. Go geçidi, web formu ve Android menüsü worker'a bağlandı. Üçüncü taraf API anahtarı gerekmez; kendi Transit TR sunucusu ve worker kurulumu gerekir.
- Gerçek tarayıcıyla yerel fixture testi: form doldurma, normal form alanlarının gönderimi, sonuç kartı okuma ve IP/plaka alanlarını dışarı çıkarmama geçti. ETA ayrıştırma testi geçti.
- Go geçidinde 5 haneli durak doğrulama, yapılandırma eksikliği ve worker aktarımı testleri eklendi; tüm sunucu testleri geçti.
- Android kaynakları API 8'e karşı derlendi; legacy/modern APK'lar üretildi ve imzaları doğrulandı. 0.5'te API 8 emülatör testi geçmişti. 0.6 için aşağıdaki güncel emülatör sonucu esas alınmalı.
- Yerel Chromium → gerçek EGO ağı denemesi proxy ile ERR_CERT_AUTHORITY_INVALID, doğrudan bağlantıda ERR_EMPTY_RESPONSE döndürdü. TLS denetimi kapatılmadı. Bu yüzden bu ortamda worker'ın gerçek EGO'ya uçtan uca canlı bağlantısı doğrulanamadı; bulut tarayıcısındaki gerçek sorguyla yerel fixture testi ayrı sonuçlardır.
- Çalışan bir ortak/public sunucu yayınlanmadı. Kullanıcının sunucu adresini yapılandırması gerekir. Android 2.2 HTTPS uyumluluğu hâlâ kullanılan sunucuya bağlıdır.
- Canlı araç harita koordinatı, kart bakiyesi ve tüm şehirlerde ETA desteği eklenmedi. Ankara özelliği sitenin gösterdiği tahmini varış sürelerini okur.

Ayrıntılı kurulum: browser-worker/README.md. Önceki katalog, çevrimdışı ve tasarım doğrulamaları docs/TEST-REPORT-v05.md içinde korunmuştur.

Son emülatör sonucu: 0.6 legacy APK Android 2.2 emülatörüne kuruldu ve açılış, veritabanı, M2 araması, görünür liste, yerel yakın durak, tarife SQL ve Konya kontrolleri PASS döndürdü (ANDROID-RUNTIME-v06.txt). Bu test gerçek EGO ağı/telefon GPS'i/Android 2.2 HTTPS bağlantısı testi değildir.
