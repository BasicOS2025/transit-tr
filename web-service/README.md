# Transit TR Web — 1.4.0

Android projesinden ayrı çalışabilen web arayüzü ve ulaşım API sunucusu. Node.js 22 veya üzeri gerekir; çalıştırmak için ek npm paketi kurulmaz.

## Çalıştırma

Bu arşivi açın. `web-service` klasöründe:

```sh
node server.mjs
```

Tarayıcıda http://127.0.0.1:8080 adresini açın. Başka port için `PORT` ortam değişkenini ayarlayın. Sunucu varsayılan olarak yalnız bu bilgisayardan erişilir. Başka cihazlardan erişim gerekiyorsa `HOST=0.0.0.0` kullanılabilir. Uzak cihazda PWA kurulumu ve konum izni için HTTPS gerekir.

## Özellikler

- 67 şehir/işletme seçeneği; destek kaynak servisine göre değişir.
- Kaynak GPS konumları ve gerçek durak varış dakikaları. Filo GPS’i seçilen durağa varış olarak sunulmaz.
- Canvas üzerinde görünür OpenStreetMap karoları, sürükleme/yakınlaştırma, sorgulanan durak pini.
- Hat koduna basınca renkli rota; katalogda ayrı rota ve sıralı durak ekranı.
- Ankara ve İstanbul metro ağı; saat temelli tahmini tren konumları ve model açıklaması. Trenler GPS değildir.
- Katalog araması/sayfalaması, yakın duraklar, yıldızlı kayıtlar; eski statik tarifeler için planlı kalkışlar.
- En fazla 12 durak + hat favorisi; gerçek kaynak dakikası, sorgu saati, yeniden boyutlandırılabilen kutular. İlk açılışta örnek favori yoktur.
- Açık/koyu tema, görünür sekmede otomatik yenileme, EGO tek hat ve tüm hat saat eşitleme, Metro İstanbul duyuruları, yerel kullanım asistanı.
- PWA uygulama kabuğu ve sıkıştırılmış şehir kataloglarını sınırlandırılmış çevrimdışı önbelleğe alma. Canlı sorgular, saat eşitleme ve metro tahmin hesaplaması sunucu bağlantısı gerektirir; çevrimdışı GPS üretilmez.

Web favori kutuları Android launcher widget’ı değildir. Android sistem widget’ı yalnız APK paketlerinde bulunur. Web’de dosya sistemi ve cache yönetimi tarayıcıya bağlıdır; uygulamanın tuttuğu ek şehir paketleri 2 MiB, sorgu önbelleği 256 KiB ile sınırlandırılır. Harita karoları service worker’a topluca indirilmez.

## API

Tüm sorgular aynı kök altında `/api/v2/` kullanır:

| Yol | Parametreler |
|---|---|
| `cities`, `health` | — |
| `live` | `city`, `stop`, isteğe bağlı `line`, `direction` |
| `route` | `city`, `line`, isteğe bağlı `direction`, `variant` |
| `catalog` | `city`, `kind=routes/stops`, `q`, `offset` |
| `nearby` | `city`, `lat`, `lon` |
| `rail` | `city`, `line` |
| `schedule` | `line`, isteğe bağlı `refresh=1` |
| `stop-times` | `city`, `stop`; planlı kalkış, canlı değildir |
| `sync` | GET durum; POST başlat; POST `cancel=1` durdur |
| `statuses` | Metro İstanbul duyuruları |
| `offline` | `city`; sıkıştırılmış statik katalog paketi |

Kimlikler Android ile ortaktır: Ankara 0, İstanbul 1, İzmir 2, Kocaeli 3, Konya 6, Aydın 7, Antalya 8, Bursa 9. Ek ASİS kimliği `1000 + ASİS şehir kodu`dur. Aydın’ın önceki 7 kimliği korunur. Her şehrin destek tablosu `docs/ASIS-CITIES-v140.md` dosyasındadır.

Anonim erişim tokenları yalnız sunucu belleğinde tutulur. Tarayıcıya token veya sağlayıcı anahtarı gönderilmez. `data` klasörü statik web kökünün dışındadır. Kullanıcı hesabı, kart bakiyesi ve ödeme işlemi bulunmaz. İşletmeci isteklerinde TLS sertifika/alan adı doğrulaması açıktır; doğrulama başarısızsa bağlantı reddedilir. Yayıncı Ata Köse (atakose2012@gmail.com). Gizlilik ve kullanım bildirimleri web/privacy.txt ve web/terms.txt içindedir. Sağlayıcı erişim/veri dağıtım haklarının incelemesi henüz tamamlanmamıştır.

Bursa’nın durak varış/GPS ve rota sorguları doğrulandı. Ayrı hat filosu servisi testin bir bölümünde HTTP 400 döndürdü; kaynak hatası saklanmaz. ASİS’te 60 kayıt içinden 32’sinde hat ve 13’ünde durak kataloğu alınabildi; tüm şehirlerde GPS garantisi verilmez.

## Test

```sh
node --test test/*.test.mjs
```

Testler HTTP doğrulaması, statik dosya sınırları, şehir kimlikleri, imza biçimleri, katalog araması, metro modeli ve eksik konumların ele alınmasını kapsar. Tarayıcı ve canlı sorgu test kayıtları `docs/v140-test-logs` altındadır. Emülatör ve tarayıcı ölçümleri fiziksel cihaz/haricî servis garantisi değildir.

## Kaynak yapısı ve lisans

`../web`: arayüz, OSM harita çizimi, PWA. `server.mjs`: yerel HTTP/API katmanı. `transport.mjs`: sağlayıcı sözleşmeleri. `journeys.mjs`: katalog/rota/saat/metro işlemleri. `data`: Android ile aynı kaynaklardan üretilen katalog ve modeller.

Kod Apache-2.0. Veri ve üçüncü taraf bileşenler için arşivdeki LICENSE ve THIRD-PARTY-NOTICES.md korunmalıdır. Verilen APK’lar ve decompile edilmiş üçüncü taraf kod/binaryler dağıtım paketine dahil değildir.
