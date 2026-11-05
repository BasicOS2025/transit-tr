# Transit TR 0.9

Ankara: 665 hat/yön, 2813 adlandırılmış OSM durak kaydı (koordinatsız, kısmi). Android 2.2 üzerinde Ankara/İstanbul/Konya canlı otobüs dakikası, İzmir canlı yaklaşan araç/kalan durak sorgusu doğrulandı; 4/4 şehir geçti. En yüksek örneklenmiş PSS 13,924 MB.

Ayrıntılar: `docs/TEST-REPORT-v09.md`. Derleme: `docs/BUILD-v09.md`. Sertifika ve alan adı doğrulaması kullanıcı isteğiyle kapalıdır.

## Önceki sürümler ve tarihsel kapsam

# Transit TR 0.8

EGO mobil JSON bağlantısı Android 2.2/API 8 üzerinde başarılı canlı sorguyla test edildi. Son testte en yüksek örneklenmiş PSS 6196 KiB (6,345 MB), 22 örnek. Ayrıntılar: `docs/ANDROID22-LIVE-RAM-v08.md`. Sertifika ve alan adı doğrulaması kullanıcı isteğiyle kapalıdır.

EGO entegrasyonu byigitt/egomac (MIT) keşiflerine dayanır; `THIRD-PARTY-NOTICES.md` içerir.

## Önceki sürümün kapsamı ve geçmiş doğrulaması

# Transit TR 0.7

Açık kaynak yerel Android uygulaması ve web/PWA. APK'lar gerçekten derlendi, hizalandı ve imzaları doğrulandı. Legacy APK Android 2.2 ARM emülatörüne kuruldu; uygulama açılışı, arama, yakın durak ve yerel tarife testleri geçti. **Tüm şehirlerde canlı ulaşım ve kart bakiyesi sağlayan tamamlanmış bir ürün değildir.** Güncel doğrulama ayrıntıları `docs/TEST-REPORT-v07.md` içinde.

## Bu sürümde çalışan kapsam

- İstanbul: İBB/İETT otobüs hat/yön kataloğu + Metro İstanbul raylı sistem hat ve istasyonları. Toplam 5.413 hat/yön kaydı, 15.631 durak/istasyon kaydı. Bu sayılar benzersiz hat veya fiziksel istasyon sayısı değildir; yön/varyant ve peron kayıtları içerir.
- İzmir: Metro, tramvay, İZBAN kataloğu: 15 hat/yön kaydı, 118 istasyon/peron kaydı. Metro ve tramvayda geçerli GTFS takvimine göre planlı kalkışlar. İZBAN kaynağı 2025'te sona eren eski takvim yayımlıyor; güncel kalkış gibi gösterilmez.
- Konya: resmî GTFS 11/2025 kataloğundan 165 hat/yön, 4.463 durak ve temsilî güzergâh dizileri. Otobüs tarifeleri bu sürümde aktarılmadı.
- Hat/durak arama, sayfalama, favoriler, raylı hatların temsilî durak dizisi, istasyon ayrıntıları, OSM harita bağlantıları.
- GPS veya elle koordinat ile 2 km içindeki yerel durakları bulma. **Sunucu, internet, API anahtarı gerekmez; koordinat cihazda işlenir.** Katalog bulunmayan bölgelerde sonuç yoktur; gerçekte durak olmadığı anlamına gelmez.
- Çevrimdışı if/else mini asistan. GPU, LLM, hesap veya model dosyası yoktur.
- İstanbul hizmet duyuruları resmî API'den alınır. Bu duyurular canlı araç konumu/ETA değildir. Kaynak güncelleme tarihi gösterilir. Web'de Go sunucusunun aynı alan adlı aktarım uç noktası kullanılır; erişimi gerçek ağ sorgusuyla test edildi. Eski Android'in TLS desteği yetersiz kalabilir.

Ankara, Kocaeli, Muğla, Eskişehir için bu sürümde yerel katalog yok; Resmî düğmesi işletmeci web sitesini açar. Diğer şehirler eklenmedi. Canlı araç koordinatları, canlı varış tahmini, kart bakiyesi, aktarmalı yol planlama, OSM içindeki eczane/market gibi genel POI taraması hazır değildir. Eski sürümün Ankara örnek kayıtları kaldırıldı.

## Kullanım

Android'de şehri seçip hat/durak ara. Kayda dokunarak ayrıntıları, uzun basarak favoriyi aç/kapat. Yakınımda → GPS veya Koordinat gir. Konum izni istendiğinde izin verdikten sonra GPS düğmesine tekrar bas. Veritabanı ilk açılışta uygulamanın özel alanına kopyalanır; yeni bir sunucu adresi girmen gerekmez.

- `transit-tr-v0.7-legacy.apk`: minSdk 8 / targetSdk 8; Android 2.2+ hedefli. Yeni Android'de düşük hedef SDK nedeniyle kurulum engellenebilir.
- `transit-tr-v0.7-modern.apk`: minSdk 23 / targetSdk 28; Android 6+ hedefli. Play Store dağıtımı için hazırlanmadı.

İki paket aynı uygulama kimliğini kullanır, birlikte kurulmaz. ARMv6/v7/64 için ayrı `.so` dosyası yoktur; Java/Dalvik kodudur. İşlemci bazında gerçek cihaz testi henüz yapılmadı. Donanımda performans garantisi yoktur. APK'lar test anahtarıyla imzalı; üretim için kendi kalıcı imza anahtarınızı kullanın. Özel anahtar kaynak ZIP'inde değildir.

## Web / PWA

Go 1.18+:

```sh
cd backend
go run .
```

`http://localhost:8080` açın. Telefonda GPS/PWA için HTTPS üzerinden yayınlayın. Başka klasörden çalıştırırken `WEB_DIR` değişkenini `web/` klasörünün mutlak yoluna ayarlayın. `PORT` varsayılanı 8080'dir.

Go sunucusu olmadan herhangi bir statik HTTP/HTTPS sunucusunda da katalog, asistan, favoriler ve yakın durak bulma çalışır. Dosyayı `file://` ile açmak desteklenmez. İlk önbellekleme için internet/yerel sunucu gerekir; sonra kataloglar ve arayüz çevrimdışı açılır. İstanbul hizmet duyuruları statik yayında tarayıcı CORS kısıtına takılabilir; Go sunucusuyla çalıştırın. PWA, eski Android 2.2 tarayıcısını hedeflemez; o cihaz için yerel APK vardır.

## Android derleme

Java 17, ECJ 3.33.0, Android API 8 `android.jar`, Android build-tools 30.0.3 (aapt/dx/zipalign/apksigner) kullanıldı. Gradle, Ant veya AndroidX gerekmez. SDK araçları kaynak ZIP'inde yoktur; kendi SDK kurulumunuzun lisanslarını ayrıca kabul edin.

```sh
keytool -genkeypair -keystore /guvenli/transit.p12 -alias transittr -keyalg RSA -keysize 2048 -validity 3650
export ANDROID_JAR=/sdk/platforms/android-8/android.jar
export BUILD_TOOLS=/sdk/build-tools/30.0.3
export ECJ_JAR=/araclar/ecj-3.33.0.jar
export KEYSTORE=/guvenli/transit.p12
export KS_PASS='kendi-parolaniz'
./tools/build_android.sh
```

Çıktılar `dist/` içinde. İmzalar betikte doğrulanır. API 8'e karşı derleme, çalışma testi yerine geçmez. Android 6+ izin çağrıları sürüm kontrollü reflection kullanır. Android 2.2'nin eski saat dilimi veritabanından etkilenmemek için tarife saatleri sabit Türkiye UTC+3 olarak işlenir; yalnızca bu veri setleri için geçerlidir.

## Veriler ve testler

```sh
python3 tools/build_catalog.py
python3 tests/catalog.test.py
node tests/core.test.cjs
(cd backend && go test -v ./...)
# Backend açıkken; npm install ile Playwright 1.62.1 kurulmalı:
node tests/web.test.cjs
```

`data/raw/` lisanslı kaynak girdilerini, `data/manifest.json` kaynak/hash bilgilerini içerir. Üretici SQLite ve aynı kayıtların web JSON'larını çıkarır. Hatalı İETT CSV koordinat biçimi ve karakter kodlaması düzeltilmiştir. GTFS'de her hat için en uzun durak dizili sefer temsilî güzergâh olarak seçilir; tüm varyantlar anlamına gelmez. Takvim istisnaları ve 24:00 sonrası saatler desteklenir; tarife gecikme/iptal bilgisi içermez.

Kod Apache-2.0. İBB verileri İBB Açık Veri Lisansı, İzmir verileri CC BY 4.0 altındadır. `docs/SOURCES-v05.md` atıflarını koruyun. OSM harita bağlantısı ODbL atfıyla açılır. Yerel veriler otomatik güncellenmez; kaynakların tekrar indirilmesi ve kataloğun yeniden üretilmesi gerekir.

Reklam/analiz SDK'sı ve arka plan konum takibi yoktur. Harita veya resmî site açılırsa ilgili üçüncü taraf istek alır. Duyuru sorguları İBB API'sine gider; konum göndermez. Kamuya açık sunucuya yayınlarken HTTPS ve trafik sınırlarını yapılandırın.

Android enstrümantasyon testi için `tools/build_android_tests.sh` aynı derleme değişkenlerini kullanır; test APK'sı `build/tests/smoke.apk` içine yazılır. API 8 cihaz/emülatörde uygulama ve test APK'sını kurduktan sonra:

```sh
adb shell am instrument -w org.transittr.tests/.Smoke
```

Test uygulamayı açar, M2 aramasını, yerel yakın durak sorgusunu, yerel tarife SQL'ini ve Konya ekranını kontrol eder. Test paketi son kullanıcıya kurulması gereken bir bağımlılık değildir.

## Ankara TAKSİS ve yeni arayüz

Verdiğiniz TAKSİS API'si bağlandı; anonim sorgular resmî sunucudan 401 döndürüyor. EGO tarafından sağlanmış yetkili erişim varsa sunucuda `TAKSIS_BEARER_TOKEN` yapılandırılmalı. Web'de Servisler, Android'de Resmî → Ankara EGO TAKSİS menüsü adaptörü kullanır. Gerçek yetkili Ankara sorgusu test edilemedi. Ayrıntı: `docs/ANKARA-TAKSIS.md`.

Web arayüzü Keşfet, Yakınımda, Favoriler, Mini asistan, Servisler ve Kaynaklar ekranlarına ayrıldı. Geist yerel yazı tipi, Phosphor favori ikonları ve nötr yüzeyler kullanılır; animasyon yalnızca küçük basma/odak geri bildirimidir. Android'de aynı görsel dil, geri dönüştürülen yerel liste satırlarıyla uygulanır. Font SIL OFL, web ikonları MIT lisanslıdır; lisans metinleri ilgili varlık klasörlerindedir.

Android 2.2 için SQLite varlığı APK içinde sıkıştırılmadan tutulur (`aapt -0 sqlite`). Bu nedenle APK yaklaşık 5 MiB olur; açılışta büyük bir sıkıştırılmış tampon RAM'e alınmaz. Veritabanı `NO_LOCALIZED_COLLATORS` ile açılır. Bu iki ayar eski Android uyumluluğu için gereklidir.


## İsteğe bağlı web sunucusu: 0.6 tarayıcı geçidi (Android 0.7 kullanmaz)

Ankara → Servisler → Otobüs nerede formu eklendi. Durak numarası Transit TR sunucusuna gider; sunucudaki Chromium EGO'nun yolcuya açık sayfasını açar, formu doldurur ve görünen tahmini süreleri okur. TAKSİS yetkisi/API anahtarı gerekmez. Tarayıcı telefonda çalıştırılmaz; eski Android'in RAM/CPU tüketimi korunur.

Çalıştırma ve sınırlar için `browser-worker/README.md` dosyasını okuyun. `npm ci`, `npx playwright install chromium`, `npm run ego:browser` ile worker başlatılır; Go sunucusu `EGO_BROWSER_URL=http://127.0.0.1:8091` ile çalıştırılır. Telefon/PWA için sunucunuzu HTTPS üzerinden yayınlayın. Ortak/public bir sunucu sağlanmadı.

Sonuçlarda kaynak saati, kontrol saati ve önbellek durumu görünür. Başarısızlıkta eski süre veya tahmin uydurulmaz. Aynı durak için 30 saniye önbellek, tek eşzamanlı tarayıcı ve en az 5 saniye sorgu aralığı vardır. IP/plaka alanları aktarılmaz.

EGO'nun gerçek sonuç verdiği bulut tarayıcısında doğrulandı. Yerel browser-worker gerçek ağ denemesi bu çalışma ortamındaki sertifika/ağ engeli nedeniyle tamamlanamadı; yerel gerçek-tarayıcı fixture testi ve geçit testleri başarılıdır. Sertifika denetimi kapatılmadı. Uçtan uca canlı worker doğrulaması yapılmış gibi sunulmaz. Güncel rapor: `docs/TEST-REPORT-v06.md`.

## 0.7: Android cihazda sorgu ve 200 MB RAM hedefi

Android APK EGO formunu cihazda HttpURLConnection ile GET/POST yapar. Sunucu adresi, Chromium, WebView, sürekli servis veya otomatik yenileme yoktur. İstek başına yanıt en fazla 256 KiB, sonuç en fazla 50 araç, aynı anda tek sorgu, bağlantı/okuma zaman aşımı vardır. Çerez ve form tokeni yalnızca o sorguda bellekte tutulur. TLS doğrulaması kapatılmaz; CAPTCHA aşılmaz. Süreç PSS değeri 160 MB'a ulaştığında yeni EGO sorgusu başlatılmaz. Bu koruma işletim sistemi seviyesinde 200 MB kotası değildir.

EGO gerçek GET formu erişilebilir, fakat gerçek POST denemesi EGO'nun hata sayfasını döndürdü. Doğrudan canlı varışlar bu sürümde deneysel ve doğrulanamadı. Hata halinde araç/süre uydurulmaz; resmî site dış tarayıcıda açılabilir. Android 2.2'nin güncel TLS desteği ayrıca sınırlıdır. Web/PWA için CORS nedeniyle aynı doğrudan sorgu uygulanmadı; önceki isteğe bağlı sunucu geçidi korunur ve RAM hedefi bu sunucuya veya tarayıcıya uygulanmaz.

RAM ölçümü ve doğrulanan kapsam: docs/TEST-REPORT-v07.md. 200 MB = 200.000.000 bayt; bütün cihazlarda kesin üst sınır garanti edilmez.
