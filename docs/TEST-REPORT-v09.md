# Transit TR v0.9 — Ankara kataloğu ve dört şehir canlı sorgu

4 Kasım 2026. Teslim edilen APK bu PC'de Android 2.2 / API 8 ARM emülatörüne kuruldu. Gerçek ağ sorgularıyla dört şehir testi **4/4**, altı ayrıştırma kontrolü **6/6** geçti.

| Şehir | Eklenen / doğrulanan özellik | Gerçek sorgu | En yüksek örneklenmiş PSS |
|---|---|---|---:|
| Ankara | 665 hat/yön + 2.813 adlandırılmış durak; EGO canlı dakika | 10940 | 7073 KiB |
| İstanbul | İETT otobüs durağına tahmini varış dakikaları | 224672 | 8339 KiB |
| İzmir | ESHOT canlı yaklaşan otobüsler ve kalan durak sayısı | 21050 | 8611 KiB |
| Konya | ATUS canlı varış dakikaları | 27 | 13598 KiB |

Dört şehir akışında en yüksek örneklenmiş süreç PSS **13.598 KiB = 13,924 MB** oldu; testte 200 MB hedefinin altında kaldı. Örnekler sorgu beklenirken 200 ms aralıklarla alındı; önceki sorguların aynı süreçte kalan verileri de bu süreç ölçümündedir. Bu sonuç tüm anlık zirveleri veya bütün fiziksel telefonları garanti etmez. Bu dört şehir çalıştırmasında WebView, Chrome, haricî geçit veya sunucu kullanılmadı. İzmir/Konya için API 8'de uygulama içinde çalışan Java TLS 1.2 gerekir.

## Ankara kataloğu

Resmî EGO Hareket Saatleri sayfasındaki 665 hat/yön ve egomac'in 2 Mayıs 2026 tarihli OSM durak adı anlık görüntüsünden geçerli kodu ve adı olan 2.813 kayıt eklendi. Bu bir kapsamlılık veya bütün Ankara'nın güncel duraklarının eksiksiz olduğu iddiası değildir. Otobüslerin yanında kaynaktaki Metro/Ankaray hat kayıtları da listelenir; canlı istemci otobüs içindir.

OSM anlık görüntüsünde koordinat yok. Bu duraklarda koordinat/harita uydurulmadı; bu Ankara kayıtları çevrimdışı yakın durak mesafe aramasına katılmaz. Koordinat alanları NULL tutulur, ekranda koordinat olmadığı belirtilir. Ankara hat-durak eşleşmesi bu sürümde yoktur. Mevcut diğer şehir kayıtlarının kimlikleri korundu. Yeni katalog için ayrı SQLite dosyası kullanılır; v0.8'in eski veritabanının yeni kataloğu gizlemesi önlendi.

## Kullanım

Şehri seç → Resmî → Seçili şehir · canlı otobüs → otobüs durak numarası → Kontrol et. Ankara, İstanbul otobüs ve Konya sayısal durak ayrıntılarında Canlı otobüs düğmesi de vardır. İzmir'de raylı sistem istasyon kodu değil, **ESHOT otobüs durak numarası** gerekir. Test örneği: 21050.

İstanbul ve Konya'daki gerçek dakika örnekleri ekran görüntülerindedir. İzmir'in anonim açık servisinin yanıtı kalan durak sayısı içerir; bu değer dakika yapılmadı. İETT'deki B işareti sefer başlamamış/sinyal belirsizliği olarak etiketlenir. Bilinmeyen süre veya eksik durak sayısı sıfır yapılmaz. Boş sonuç veya HTTP/biçim hatası canlı araç sonucu gibi gösterilmez. Kaynakların veri güncelliği garanti edilmez; alınma zamanı Türkiye UTC+3 saatiyle gösterilir. Görüntülerdeki dakikalar test anına aittir.

Kocaeli, Muğla ve Eskişehir için önceki kapsam değişmedi. Bu sürümde onlar için canlı bağlantı eklenmedi. Bütün ulaşım türlerinin canlı GPS haritası/kart bakiyesi tamamlanmış değildir.

## Kaynaklar ve uyarlama

- Ankara hat kataloğu: https://www.ego.gov.tr/hareketsaatleri
- EGO mobil protokolü ve OSM durak adı kaynağı: https://github.com/byigitt/egomac (MIT; OSM ODbL atfıyla). Bildirim/menü çubuğu gibi macOS özellikleri taşınmadı.
- Kullanıcının sağladığı İstanbul/İzmir protokol koleksiyonu incelendi: https://github.com/caglarsarikaya/postman-collections . İçindeki İzmir istekleri hat, durak, duyuru ve planlı tarifedir; bu kaynaklar canlı dakika diye sunulmadı. Koleksiyon dosyaları kaynak paketine kopyalanmadı.
- İETT'nin kendi durak ekranının kullandığı gerçek varış servisi: https://iett.istanbul/tr/RouteStation/GetStationInfo?dcode=224672&langid=1
- İzmir resmî canlı yaklaşan otobüs servisi: https://openapi.izmir.bel.tr/api/iztek/duragayaklasanotobusler/21050
- İzmir resmî belge: https://openfiles.izmir.bel.tr/471231/docs/iztek-WebServisKullanimDokumani_1.0.pdf
- Konya resmî canlı varış tablosu: https://atus.konya.bel.tr/atus/otobusum-nerede?durakNo=27

## Uyumluluk ve sınırlama

Android 2.2'nin yerleşik TLS'i İzmir ve Konya'ya bağlanamadığından Java 5 uyumlu Bouncy Castle 1.86 hafif TLS katmanı eklendi. Android'in sistem BC sınıflarıyla çakışmaması için paket adları org.transittr.bc altına taşındı; R8 kullanılmayan bölümleri çıkardı. Yerel .so dosyası yoktur. Kaynakta lisans, hazır kitaplık ve yeniden adlandırma betiği vardır.

**Kullanıcının önceki açık isteğiyle sertifika ve alan adı doğrulaması kapalıdır.** Bu APK sunucu kimliğini doğrulamaz. Modern TLS eklenmesi bu tercihi değiştirmedi.

## Doğrulama

- Android 2.2: yeni Ankara veritabanı, ortak sorgu penceresi, dört gerçek canlı sorgu, 6 ayrıştırma kontrolü; 4/4 şehir geçti.
- 6 katalog testi, JavaScript çekirdek testleri, Go backend testleri ve web uçtan uca akışı geçti (WEB_EXIT 0).
- Yeni koordinatsız durakların haritada 0,0 olarak gösterilmesi/mesafe hesabına girmesi önlendi; PWA önbellek sürümü güncellendi.
- Teslim edilen legacy APK sıfırdan derlendi, zipalign uygulandı ve API 8 imzası doğrulandı. Java 6 hedefi, resmî API 8 android.jar, ECJ 3.33.0, R8/aapt/zipalign/apksigner 36.1.0 ve JDK 25 kullanıldı.
- Fiziksel ARMv6/ARMv7/ARM64 telefon veya modern Android bu v0.9 çalıştırmasında ayrıca denenmedi. Teslim edilen APK test anahtarıyla imzalıdır; ilk indirilen eski APK'ların imzası farklı olabilir.

Ham kanıt: Transit-TR-v09-Android22-Test.txt. Resimler Android framebuffer çıktılarıdır; maket değildir.
