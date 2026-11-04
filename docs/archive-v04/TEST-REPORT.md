# Transit TR 0.4 — doğrulama raporu

4 Kasım 2026

## Başarılı

- Android Java kaynakları ECJ 3.33.0 ile Android API 8 android.jar'a karşı Java 6 bytecode olarak derlendi.
- dx ile DEX üretildi; aapt paketleme ve zipalign tamamlandı. Eski APK minSdk 8 / targetSdk 8; yeni APK minSdk 23 / targetSdk 28.
- Her iki son APK apksigner doğrulamasından geçti: v1/v2/v3. APK-VERIFY.txt ham çıktıyı içerir. ZIP yapısı, manifest ve DEX incelendi; native `.so` yok.
- Go 1.22.2: koordinat/kategori doğrulama, mesafe, sahte Overpass sunucusuyla ayrıştırma-sıralama-önbellek, upstream hata davranışı testleri geçti (4 test).
- Node: Türkçe büyük harf, canlı sorgu önceliği, kart, yakın yerler, hat, selamlama, boş girdi kapsamındaki 6 asistan testi geçti.
- Playwright 1.62.1 / Chrome Headless Shell 131.0.6778.204: resmî İzmir hatları, 24 istasyon, canlı soruya veri yok yanıtı, favoriler, 360 px görünüm, service worker, çevrimdışı yeniden yükleme ve çevrimdışı asistan/katalog testleri geçti; sayfa JS hatası yok. Görünüm incelendi.
- Resmî İzmir Metro GTFS ZIP'i indirildi ve CSV'ler ayrıştırıldı. Kaynak beyanı 2026-07-09—2027-07-09; uygulama sefer güncelliği garantisi vermez.

## Başarısız / doğrulanamadı

- Gerçek Overpass sorgusunda backend `upstream unavailable` döndürdü. Sahte sunucu testi başarılıdır; gerçek ağ hizmeti uçtan uca doğrulanmadı. GPS izinleri ve cihaz konumu test edilmedi.
- Android cihaz veya Android emülatörü mevcut değildi. APK kurma, uygulamayı açma, ARMv6/v7/64 çalışma davranışı, Android 2.2 TLS, konum izin diyaloğu, küçük Android ekranı ve pil/RAM/CPU ölçümü yapılmadı. İmza doğrulaması kurulum/çalıştırma testi değildir.
- PWA'nın tarayıcı dışına kurulması ve farklı tarayıcılar test edilmedi.
- Canlı konum, ETA, kart bakiyesi, rota planlama ve birçok şehir/ulaşım türü entegrasyonu tamamlanmadı.

## Yapılan düzeltmeler

- Android tüm ekran kaydırılabilir; eskimiş GPS konumu yerine süre sınırlı konum güncellemesi, duraklatmada GPS aboneliği iptali, eşzamanlı ağ isteği engeli ve yanıt boyutu sınırı.
- Yeni Android için izin isteme ve ayrı hedef SDK manifesti; uygulama ikonu ve konum donanımının isteğe bağlı işaretlenmesi.
- Web asistanında “canlı metro ne zaman” sorusunun yanlışlıkla statik hat yanıtına düşmesi düzeltildi; depolama kapalıyken favori kaydetme hatası yakalanır.
- Uydurma şehir hat kodları kaldırıldı; resmî İzmir statik verisi örnek Ankara verisinden ayrıldı.
- PWA API yanıtlarını önbelleklemez, eski Transit TR önbelleklerini temizler; ikonlar ve katalog çevrimdışı kabuğa dahil edildi.
- Backend sorgu eşzamanlılığı sınırlı, HTTP zaman aşımları ve uygulama User-Agent'ı var; konum önbelleği hassasiyeti artırıldı; OSM atfı ve mesafe türü eklendi.
- Tam Apache-2.0 lisansı ve veri kaynak/atıf belgeleri eklendi.
