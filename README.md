# Transit TR 

Android 6+ ve Android 2.2 için yerel toplu taşıma uygulaması. OpenStreetMap üzerinde belediye kaynaklarından gelen otobüs konumlarını, Ankara ve İstanbul metro/istasyon ağını gösterir. WebView veya Google Play Services gerektirmez.

## Özellikler

- **Canlı harita:** Ankara EGO durak sorgusundan GPS ve varış dakikaları; İstanbul İETT hat sorgusundan GPS konumları. İstanbul durağının dakika sonuçları “Tüm sonuçlar” bölümündedir; GPS konumu dakika tahminine çevrilmez.
- **Metro:** Ankara M1–M4 ve Ankaray (5 hat, 54 istasyon kaydı); İstanbul 19 raylı hat, 250 istasyon kaydı, M11 dahil. Hat seçme, istasyon listesi ve istasyona odaklanma.
- **OpenStreetMap:** sürükleme, iki parmakla yakınlaştırma, yakınlaştır/uzaklaştır, görünür alan için karo önbelleği ve kaynak atfı.
- **Hızlı yerel arayüz:** Geist yazı tipi, sade açık yüzeyler, Material harita ikonu. Android 2.2’de daha küçük karo belleği ve tek ağ işçisi.
- Harita açıkken 20 saniyede yenileme; arka planda takip servisi yoktur. Hata sırasında önceki konumlar korunur ve yeniden deneme aralığı uzar.
- **Katalog:** Ankara, İstanbul, İzmir ve Konya hat/durak arama, favoriler, yerel yakın durak ve mevcut tarife ekranları.
- **Diğer canlı sorgular:** İzmir yaklaşan araç/kalan durak ve Konya varış bilgileri. İzmir harita GPS desteği vardır; bu sürümün gerçek GPS harita testi Ankara ve İstanbul’da yapıldı. Konya harita GPS katmanı yoktur.

Metro için ayrı bir harita bulunur. **Tren konumları saate dayalı tahmindir, GPS değildir.** Ankara’da EGO’nun iki yöndeki hafta içi/cumartesi/pazar kalkış çizelgeleri; İstanbul’da yayımlanan yolculuk süreleri ve sıklık modeli kullanılır. İstanbul’daki pik sıklık gün boyunca sabit kabul edilir; M11’in 10 dakikalık sıklığı model varsayımıdır. İstasyon arası süre mesafeye oranlanır, ara duraklarda 20 saniye bekleme varsayılır. M1–M2–M3, Ankara’da birleşik Koru–OSB koridoru olarak hesaplanır; seçilen etaptaki tahmini trenler gösterilir. Model ayrıntıları ve kaynaklar **Model** düğmesindedir. Gecikme, iptal, ek sefer ve resmî tatil değişiklikleri hesaba katılmaz. İstasyonlar arası çizgiler temsili bağlantıdır; gerçek ray geometrisi veya aktarmalı rota hesaplaması değildir. İstasyon sayıları veri kümesindeki ad/koordinat kayıtlarını ifade eder. Yerel ağ/katalog anlık hizmet kesintilerini takip etmez.

## Kurulum ve kullanım(releases kısmındadır)

- Android 6+ için `Transit-TR-v1.0-android6-plus.apk` (minSdk 23, targetSdk 28).
- Android 2.2 için `Transit-TR-v1.0-android22.apk` (minSdk 8, targetSdk 8).

İki APK aynı paket kimliğini (`org.transittr`) kullanır; aynı anda kurulmaz. APK’lar test anahtarıyla imzalıdır. Üretim dağıtımında kendi kalıcı anahtarınızı kullanın. Özel anahtar kaynak paketinde bulunmaz. Yeni Android sürümlerinin tamamında ve fiziksel telefonlarda test yapılmadı; Play Store yayını hazırlanmadı.

Şehri seçin, otobüs durağı numarasını yazın ve **Başlat** düğmesine dokunun. İstanbul’da GPS için hat kodunu da girin. Test örnekleri: Ankara `10940`, İstanbul `224672` + `500T`, İzmir `21050`, Konya `27`. Sonuçlar saatin, hattın ve servis erişiminin durumuna göre değişir. Araç veya istasyon işaretine dokunarak ayrıntıları açın. **Metro** sekmesinden hattı ve istasyonu seçin; **Katalog** eski arama ekranını açar.

Konum zamanı kaynak tarafından verilirse ayrıntılarda gösterilir; 3 dakikadan eski zaman damgalı araçlar haritada gri çizilir. Kaynak zamanı bilinmeyen sonuçlar güncel zaman damgası verilmiş gibi sunulmaz. Canlı takibin çalışması için telefon GPS izni gerekmez; “Yakınımda” özelliği cihaz konumunu kullanır.

## Test

Android 2.2/API 8 ARM ve Android 6.0/API 23 x86 emülatörlerinde gerçek Ankara/İstanbul GPS, metro ağı, yenileme ve PSS ölçüldü. Ayrıntılar: [test raporu](docs/TEST-REPORT-v1.md). Testler gerçek servisleri kullanır; boş durak veya servis kesintisi başarısız sonuç üretebilir. Emülatör RAM ölçümü fiziksel cihaz garantisi değildir.

```sh
python tests/catalog.test.py
node tests/core.test.cjs
python tools/run_map_tests.py emulator-5556 --output test-results
adb shell am instrument -w org.transittr.tests/.V09Check
```

## Derleme

[Android derleme rehberi](docs/BUILD-v1.md), [veri/API kaynakları](docs/SOURCES-v1.md). Java/ECJ, Android SDK araçları ve kendi imza anahtarınız gereklidir. Gradle veya AndroidX kullanılmaz. `python tools/build_android.py`; Android 6+ için `ANDROID_VARIANT=modern`, test APK’sı için `ANDROID_TESTS=1`.

Web/PWA kataloğunu çalıştırmak için Go 1.18+ ile `backend` klasöründe `go run .` komutunu çalıştırın ve `http://localhost:8080` adresini açın. Bu sürümün yeni yerel harita/GPS arayüzü Android APK’larına eklendi. Önceki sürümlerin ayrıntıları [tarihsel README](docs/README-v09-history.md) içindedir.

##Çalışma diyagramı
[![Architecture diagram of basicos2025/transit-tr](https://gitdiagram.com/basicos2025/transit-tr/diagram.png)](https://gitdiagram.com/basicos2025/transit-tr?utm_source=readme&utm_medium=picture)

## Veri, gizlilik ve lisans

Resmî olmayan bağımsız projedir; belediyeler tarafından yayımlanmış bir uygulama değildir. Kullanıcı hesabı, reklam veya analiz SDK’sı yoktur. Canlı sorgular belediye servislerine, harita istekleri OpenStreetMap sunucularına gider. Yalnızca görüntülenen karolar istenir; toplu veya çevrimdışı bölge indirme yoktur. Yerel karo önbelleği en az 7 gün yeniden kullanılır.

**Sertifika ve alan adı doğrulaması eski sürüm limitlemelerinden dolayı kapalıdır.** Kesin web güvenliği vaat edilemez.

Kod Apache-2.0. Google Material Icons Apache-2.0; Geist SIL OFL; Bouncy Castle kendi lisansı; OpenStreetMap verileri ODbL, İBB ve İzmir verileri ilgili açık veri lisansları altındadır. Veri lisansları kod lisansından ayrıdır. [Üçüncü taraf bildirimleri](THIRD-PARTY-NOTICES.md) ve [kaynaklar](docs/SOURCES-v1.md) korunmalıdır.

MapCheck arka plan kontrolü sırasında `RESUME_FROM_HOME` durumunu üretir; Python çalıştırıcı ADB ile uygulamayı öne getirir. Bu nedenle yaşam döngüsü kontrolü için çalıştırıcıyı kullanın.
