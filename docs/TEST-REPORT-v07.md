# Transit TR 0.7 — cihazda EGO sorgusu ve RAM testi

4 Kasım 2026.

## Sonuç

Android APK tarayıcı veya sunucu çalıştırmadan EGO formunu cihazda okumayı dener. EGO GET formu geliştirme ortamından erişildi, POST ise EGO'nun genel hata sayfasını döndürdü. Android 2.2 emülatöründe gerçek EGO isteği DNS hatasıyla sonuçlandı: UnknownHostException. **Başarılı canlı varış sorgusu doğrulanamadı.** Sadece sonuç bölümünü yerel Android metin ekranına aktarır; web sayfasını render etmez.

## RAM

200 MB = 200.000.000 bayt hedefi Android uygulama sürecinin toplam PSS değeri üzerinden ele alındı. Debug.getMemoryInfo ile örnekleme yapıldı. Enstrümantasyon da aynı süreçte çalışır.

| Senaryo | Ölçülen en yüksek PSS | Durum |
| --- | --- | --- |
| Android 2.2 katalog ekranları ve 100 sonuç ayrıştırma çağrısı | 10042 KiB / 10.28 MB | Geçti |
| Gerçek EGO bağlantı denemesi, 200 ms aralıklı örnekleme | 10343 KiB / 10.59 MB | DNS hatası; site yüklenmedi |
| Başarılı canlı EGO sorgusu | Ölçülmedi | Doğrulanamadı |
| Yeni Android / fiziksel ARMv6, ARMv7, ARM64 cihazlar | Ölçülmedi | Doğrulanamadı |
| PWA ve haricî tarayıcı | Ölçülmedi | Bu Android ölçümünün dışında |

Emülatör: Android 2.2 API 8 ARM926, 128 MB RAM / 16 MB Dalvik heap. Bu değerler tek test ortamının örneklenmiş PSS değerleridir; anlık bütün zirveleri veya gerçek cihaz tüketimini garanti etmez. 200 MB bütün cihazlarda kesin üst sınır olarak doğrulanmış değildir. RSS ölçümü yapılmadı.

## Bellek ve işlem sınırları

- WebView, Chromium, kalıcı servis, reklam/analiz SDK'sı, otomatik EGO yenilemesi yok.
- Aynı anda tek EGO sorgusu; sadece kullanıcı başlatır.
- İstek yanıtı en fazla 256 KiB; en fazla 50 araç; çerez ve form alanı sayısı sınırlı.
- Bağlantı/okuma zaman aşımı ve akış okuma süresi kontrolü var. Sistem DNS çözümleme süresi bağlantı zaman aşımını aşabilir; bu test yaklaşık 20 saniyede DNS hatası verdi.
- Süreç PSS 160 MB olduğunda yeni EGO sorgusu başlatılmaz. İşletim sistemi seviyesinde bellek kotası değildir.
- EGO formundaki güncel alan adı, token ve oturum çerezleri kullanılır; disk üzerinde saklanmaz. TLS doğrulaması kapatılmaz.
- Kaynaktan yalnızca hat, yön, tahmini süre ve kaynak zamanı gösterilir; IP ve plaka aktarılmaz. Hata veya eksik HTML güncel araç sonucu gibi gösterilmez.

## Testler ve derleme

Legacy ve modern APK API 8 jar, ECJ 3.33.0, Android build-tools 30.0.3 ile gerçekten derlendi. APK imzaları ve manifestleri doğrulandı; test imza anahtarı kullanıldı, özel anahtar kaynakta yoktur. Legacy APK API 8 emülatörüne kuruldu. Açılış, veritabanı, M2 araması, yakındaki durak, tarife SQL, Konya ve 100 EGO sonuç ayrıştırma testi geçti. Yerel Java testinde 9 form/sonuç doğrulaması geçti. Katalogda 6 test ve web core testleri geçti. Modern APK fiziksel cihazda çalıştırılmadı.

Ham kanıtlar: ANDROID-RUNTIME-v07.txt, ANDROID-NETWORK-RAM-v07.txt, LOCAL-EGO-TEST-v07.txt, APK-VERIFY-v07.txt.

## Kullanım ve kalanlar

Android: Resmî → Ankara canlı durak (cihazdan). Sunucu adresi gerekmez. Bağlantı deneysel olduğu için başarısız olabilir. Resmî site dış tarayıcıdan açılabilir; dış tarayıcının belleği bu ölçüme dahil değildir.

Web/PWA'daki eski isteğe bağlı EGO sunucu geçidi korunmuştur; tarayıcı CORS kısıtı nedeniyle doğrudan Android yöntemi web'e uygulanmadı. Sunucu açık tutulması gerekmez: çevrimdışı katalog ve mini asistan bağımsızdır. Kart bakiyesi, canlı GPS haritası, bütün şehirlerin veri kapsamı ve TAKSİS yetkili erişimi tamamlanmış değildir. Önceki README'deki veri kapsamı sınırları geçerlidir.
