# Transit TR 0.5 — test raporu

04.11.2026

## Doğrulananlar

- Resmî SDK Platform 8'in android.jar dosyasına karşı Java 6 bytecode derlemesi başarılı. Eski APK minSdk 8 / targetSdk 8; modern APK minSdk 23 / targetSdk 28.
- DEX üretimi, aapt paketleme, zipalign ve apksigner doğrulaması başarılı. V1/V2/V3 imzaları doğrulandı. Her iki APK test anahtarıyla imzalıdır; üretim anahtarı değildir. APK-VERIFY-v05.txt ham çıktı.
- Yerel SQLite katalog: 5.593 hat/yön kaydı, 20.212 durak/istasyon kaydı. SQLite bütünlük denetimi, ilişkiler, koordinat sınırları, karakter kodlaması ve web/Android katalog eşitliği testleri geçti (6 test).
- Çevrimdışı mantık testleri: Türkçe arama normalizasyonu; mesafe ve 2 km sınırı; hatalı koordinat; UTC+3 gece yarısı; GTFS 24:00 sonrası saatleri; eklenen/iptal edilen hizmet günleri; süresi bitmiş tarife; asistanın canlı sorgu önceliği. CORE-TEST-v05.txt.
- Go sunucu testleri: yakın yerler, duyuru aktarımı ve TAKSİS yetki/alan eşleme/güvenli çıktı testleri (9 test). BACKEND-TEST-v05.txt.
- Playwright 1.62.1 / Chrome Headless Shell 131: hat ayrıntısı, favoriler, taklit GPS konumu, elle koordinat, planlı saat paneli, 360 px görünüm, çevrimdışı yeniden açma, çevrimdışı İstanbul/İzmir/Konya şehir değiştirme, çevrimdışı asistan ve yakın durak bulma geçti. Sayfa JavaScript hatası yok.
- Gerçek ağ: İBB GetServiceStatuses yanıtı, Go aktarımı üzerinden tarayıcıda alındı; hat duyuru metni ve kaynak güncelleme tarihi doğrulandı. Bu bir araç takip testi değildir. WEB-TEST-v05.txt çıktıyı içerir.
- Web görünümü görsel olarak incelendi. İETT kaynaklarındaki bozuk karakter kodlaması ve noktalarla ayrılmış koordinatlar onarıldı; tam hat kodu araması sonuçlarda öne alındı.

## Kapsam ve sınırlar

- İstanbul: 5.413 hat/yön kaydı ve 15.631 durak/istasyon kaydı. Otobüs için hat-durak eşlemesi/sefer takvimi henüz aktarılmadı; raylı sistem durak dizileri mevcut.
- İzmir: 15 hat/yön ve 118 istasyon/peron kaydı. Metro/tramvay takvimleri geçerli kaynak tarihleri içinde sorgulanır. İZBAN takvimi 2025'te bitti; eski olduğu açıkça belirtilir ve güncel kalkış üretilmez.
- Konya: resmî GTFS 11/2025 kataloğundan 165 hat/yön ve 4.463 durak kaydı; temsilî güzergâhlar var, otobüs saatleri aktarılmadı.
- Ankara, Kocaeli, Muğla, Eskişehir ve diğer şehirlerde yerel katalog tamamlanmadı. Resmî düğmesi dış işletmeci sitesini açar; bu, yerel entegrasyon değildir.
- Canlı araç konumu, canlı ETA, kart bakiyesi, aktarmalı yol planlama ve genel OSM POI sorgusu tamamlanmadı. Statik tarifeler gerçek zamanlı veri gibi sunulmaz.
- GPS testinde tarayıcıya örnek konum verildi; fiziksel GPS, gerçek Android cihazı, ARMv6/v7/64, Android 6+ izin diyaloğu, RAM/CPU/pil tüketimi test edilmedi.
- Web duyuru servisine doğrudan erişim tarayıcıda başarısız oldu; aynı alan adlı Go aktarımı ile gerçek yanıt alındı. Yalnızca statik sunucuda duyuru özelliği CORS nedeniyle çalışmayabilir.
- Android 2.2'nin eski TLS/CA desteği resmî HTTPS servisine bağlanamayabilir. Yerel katalog ve yakın durak araması internet gerektirmez.
- PWA'nın işletim sistemine kurulum diyaloğu ve farklı tarayıcılar test edilmedi; Chromium'da çevrimdışı yeniden açma doğrulandı.
- Kataloglar otomatik güncellenmez. Kaynak veri ne yayımlıyorsa onu içerir; güncellik ve kesintisiz servis garantisi yoktur.

## Android 2.2 gerçek emülatör sonucu

Son APK Android SDK Platform 8 / Android 2.2 ARM emülatörüne gerçekten kuruldu. Paket yöneticisi Success döndürdü. Ayrı enstrümantasyon test paketi aynı anahtarla imzalanıp kuruldu ve aşağıdaki kontroller PASS döndürdü:

- Activity açılması, SQLite dosyasının kopyalanması/açılması;
- ekran alanında listenin gerçekten görünmesi;
- M2 tam hat kodunun arama sonucunda öne alınması;
- yerel yakın durak sorgusu;
- Android'in kendi SQLite sürümünde tarife sorgusu;
- Konya kataloğunun açılması.

Ham çıktı ANDROID-RUNTIME-v05.txt içindedir. Bu bir gerçek telefon veya ARMv6/v7/64 donanım testi değildir. Enstrümantasyon bitince hedef süreç kapatıldığı için son meminfo çağrısı süreç bulamadı; bu çıktıdan RAM/CPU tüketimi çıkarılmadı.

Test sırasında yakalanıp giderilen uyumsuzluklar: Android 2.2 sıkıştırılmış varlıklar için 1 MiB açma sınırı uyguluyor; SQLite dosyası aapt -0 sqlite ile APK'ya sıkıştırılmadan konuyor. Eski SQLite android_metadata tablosunu bekliyordu; uygulama kendi normalleştirilmiş aramasını kullandığı için NO_LOCALIZED_COLLATORS ile salt okunur açılıyor. Dar/yatay ekranda listeyi kapatan araç çubukları birleştirildi; ilk açılışta klavye odağı önlendi.

## Arayüz ve Ankara

Taste Skill kuruldu ve kullanıldı. Seçilen app stili Achromatic ledger, seed 713288367; gerçek hat kodları veri vurgusu olarak kullanılır. Web arayüzü 1440/768/390 px boyutlarda ölçüldü: 0 FAIL, 0 WARN. Kaynak lint'inde tek uyarı saatlerin 01:05 biçiminde yazılmasıdır; bu dekoratif numaralandırma değildir. Ekranlar görsel olarak incelendi; boş kaynak hat satırları çıkarıldı.

TAKSİS Swagger tanımı incelendi. Durak/istasyon/araç gerçek GET sorguları 401 döndürdü. Adaptör eklendi ve sahte yetkili sunucuyla alan eşlemesi test edildi; gerçek yetkili veri alınamadı. TAKSİS_BEARER_TOKEN yalnızca sunucuda yapılandırılır. Ankara canlı araç/ETA/kart bakiyesi çalışıyor iddiası yoktur.
