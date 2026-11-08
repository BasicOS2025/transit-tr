# 1.0 veri ve API kaynakları

Kontrol tarihi: 5 Kasım 2026. Kaynakların erişilebilirliği ve araç sayıları zamanla değişir. Uygulama servis yanıtlarından olmayan araç, dakika veya tren konumu üretmez.

## Canlı otobüs

- Ankara EGO: `https://egocptsrvand.ego.gov.tr/mblSrv14/service.asp?FNC=Otobusler&VER=3.1.0&LAN=tr&DURAK=10940` mobil JSON protokolü. `lat/lng`, `saniye`, `konum_tarihi`; tarifeli `arac_no=-` kayıtları harita aracı sayılmaz. EGO entegrasyonunun keşfinde [byigitt/egomac](https://github.com/byigitt/egomac), MIT.
- İstanbul İETT GPS: `https://api.ibb.gov.tr/iett/FiloDurum/SeferGerceklesme.asmx?WSDL`. SOAP `GetHatOtoKonum_json`, parametre **HatKodu**. `kapino`, `hatkodu`, `enlem/boylam`, `son_konum_zamani`, `yon`. `kapino` kullanıcıya kişisel veri gibi sunulmaz; araç eşleme kimliğidir. Gerçek test hattı `500T`.
- İstanbul durak varışları: `https://iett.istanbul/tr/RouteStation/GetStationInfo?dcode=224672&langid=1`. HTML içinde kaynakta belirtilen dakika gösterilir; B işaretli/sinyalsiz kayıtların kaynak uyarısı korunur. GPS konumundan geliş dakikası hesaplanmaz. Durak sorgusu ve GPS hattı bağımsızdır: seçilen hattın tüm araçları seçilen durağa gelecek araçlar olarak etiketlenmez.
- İzmir: `https://openapi.izmir.bel.tr/api/iztek/duragayaklasanotobusler/21050`. `KoorX` enlem, `KoorY` boylam; virgüllü ondalık desteklenir. `KalanDurakSayisi` durak sayısıdır. Kaynak zamanı yoksa yeni zaman damgası üretilmez. GPS ayrıştırıcı var; bu sürümde gerçek harita GPS testi Ankara/İstanbul ile sınırlıdır.
- Konya: `https://atus.konya.bel.tr/atus/otobusum-nerede?durakNo=27`. ATUS HTML `tbody#searchBody`; varış bilgisi var, bu sürümde konum katmanı yok.
- [caglarsarikaya/postman-collections](https://github.com/caglarsarikaya/postman-collections) uç nokta keşfi için referanstır; koleksiyon dosyaları kaynak paketine kopyalanmadı. Koleksiyondaki HatNo örneği yerine resmî WSDL’nin HatKodu parametresi kullanıldı.

## Raylı sistem

Paketlenen veri: `data/raw/rail-networks-v1.json` → `tools/build_rail.py` → `android-legacy/assets/rail-networks.json`. Dosya metadata alanı kaynak bağlantılarını saklar. Araçlar veya görünüm kodu kopyalanmadı; yalnızca ad/koordinat/ağ kayıtları kullanıldı.

- Ankara M1, M2, M3, M4 ve A1: [Kaç Durak Ankara haritası](https://www.kacdurak.com/ankara/harita/) üzerinde yayımlanan OpenStreetMap tabanlı ODbL veri. 5 Kasım 2026 alınan durağan ağ, 54 ad/koordinat kaydı. Banliyö/teleferik bu katmana eklenmedi.
- İstanbul 18 hat: mevcut Metro İstanbul hat/istasyon açık servisinden alınmış proje katalog verileri (`data/raw/istanbul-lines.json` ve istasyon girdileri), [önceki kaynak belgesi](SOURCES-v05.md) ve İBB Açık Veri Lisansı.
- İstanbul M11: [Kaç Durak İstanbul haritası](https://www.kacdurak.com/istanbul/harita/) üzerindeki OSM/ODbL istasyon kaydı. Gayrettepe–Halkalı 15 istasyon. Halkalı uzatmasının işletmeye alınması ayrıca [Ulaştırma Bakanlığının açıklaması](https://www.uab.gov.tr/haberler/tuerkiye-nin-en-uzun-ve-hizli-metrosundan-rekor/) ile kontrol edildi. Bu bilgi anlık işletme/arıza garantisi değildir.
- İstanbul 19 hat/250 ad-koordinat kaydı; bu sayı farklı peron/konum kayıtları nedeniyle benzersiz fiziksel istasyon sayısı olarak yorumlanmamalıdır. Tüm İstanbul banliyö hatlarını içeren eksiksiz ağ iddiası yoktur.

Çizgiler istasyonlar arasında düz temsili bağlantılardır. Ray/tünel/karayolu gerçek geometrisi değildir. Trenlere ait canlı GPS kaynağı yoktur. Ayrı metro haritasında hareketli işaretler açıkça saate dayalı tahmin olarak sunulur; araç kimliği, gözlenmiş konum veya kesin varış bilgisi değildir.

OSM türevi istasyon verisinin lisansı ODbL 1.0: https://opendatacommons.org/licenses/odbl/1-0/ . © OpenStreetMap contributors. Diğer veri katmanlarının kendi lisansları korunur; kodun Apache-2.0 lisansı bu veri lisanslarını değiştirmez.

## Harita karoları ve ikon

Karolar: `https://tile.openstreetmap.org/{z}/{x}/{y}.png`, © OpenStreetMap contributors. [OSM karo kullanım politikası](https://operations.osmfoundation.org/policies/tiles/). İstek User-Agent değeri `TransitTR/1.0 (org.transittr; Android)`. Sadece görünür alan istenir; prefetch, toplu indirme ve çevrimdışı bölge paketi yoktur. Disk önbelleği 7 gün tekrar kullanılır, bozuk karo önbelleğe yazılmaz; hatalı istek 30 saniye bekletilir. RGB565 RAM önbelleği Android 2.2’de 6, modern sürümde 40 karoyla sınırlıdır. Ekranda tıklanabilir OSM atfı bulunur. Standart OSM sunucularının kullanılabilirlik garantisi yoktur; geniş ölçekli dağıtımda sağlayıcı kapasitesi ayrıca planlanmalıdır.

Harita sekmesi ikonu: [Google Material Icons map](https://github.com/google/material-design-icons/blob/master/src/maps/map/materialicons/24px.svg), Apache-2.0. Resmî 24×24 SVG yolu Android Canvas Path ile çizilir; API 8’de VectorDrawable bağımlılığı yaratmaz. 24dp ikon ve 56dp dokunma alanı. [Lisans bildirimleri](../THIRD-PARTY-NOTICES.md).

## Saate dayalı metro konumu

`data/raw/rail-estimates-v1.json` → `tools/build_rail.py` → `android-legacy/assets/rail-estimates.json`. Kaynak anlık değil, 5 Kasım 2026 tarihli kayıt.

Ankara: EGO [M1-D](https://www.ego.gov.tr/hareketsaatleri?hat_no=M1-D), [M1-G](https://www.ego.gov.tr/hareketsaatleri?hat_no=M1-G), M4-D/G ve A1-D/G sayfalarındaki üç günlük tipteki `hs-times` hücrelerinden yalnızca tam terminal kalkışları alındı. Ara istasyondan başlayan özel 06:00 kayıtları modele dahil edilmedi. Yayınlanan yön süreleri M123: 59/60 dk, M4: 19/19 dk, A1: 15/15 dk. Bu sürelerle mesafeye orantılı ara hareket ve 20 sn duruş hesaplanır; segment zamanları resmî peron zamanları değildir.

İstanbul: mevcut resmî GetLines içeriklerindeki tek yön yolculuk süresi ve pik sefer aralığı. M2 ana hat 235 sn aralık; Seyrantepe mekik kolu ayrıca modellenmez. Diğer metro hatlarında yayımlanan pik sıklık tam gün için sabit varsayılır. Gece metrosu uygulanan M1A/M1B/M2/M4/M5/M6/M7 için cuma/cumartesi hizmet gecesinde 00:00–06:00 arası 30 dk modeli; [resmî SSS](https://metro.istanbul/Home/SikcaSorulanSorular?q=27). Başlangıç fazı yaklaşık 06:00’dır; tek tek tren kalkış çizelgesi değildir. M11 57 dk yolculuk süresi Bakanlık bilgisi; 10 dk sıklık ve 06:00–00:00 pencere açık model varsayımlarıdır. Tramvay/füniküler/teleferik için hareket tahmini üretilmez; istasyonlar gösterilebilir.

Hizmet günü Türkiye UTC+3 saatinde 06:00 sınırında seçilir. Gece yarısından sonraki geç seferler önceki hizmet gününe bağlanır. Model sürerken her tren kendi kalkışından toplam süre kadar gösterilir; son kalkıştan sonraki yolculuk tamamlanabilir. Android 2.2’de 5 sn, modern görünümde 1 sn hesaplama; ağ isteği veya arka plan servisi yoktur. Gerçek tren kimlikleri yoktur.



## 1.1.0 kaynakları (7 Kasım 2026)
- EGO saat/metadata/sıralı duraklar: https://www.ego.gov.tr/hareketsaatleri?hat_no=481 . Uygulama üç gün sütununu ve saat notlarını saklar. 665/665 indirildi; 20 hat kaynakta saat yayımlamıyor. Tarife gerçek zamanlı varış değildir.
- Ankara koordinatları: OSM Ankara il sınırı `node[highway=bus_stop][ref]`, https://overpass-api.de/api/interpreter üzerinden bağımsız indirme. Çakışan koordinatlı aynı ref kayıtları eşleştirilmez. ODbL; ham veri kaynak paketinde.
- İETT rota/stop yedeği: https://otobusum.metkm.win/v1/routes/500T?city=istanbul ve `/v1/stop/224672?city=istanbul`. Otobüs GPS'inde resmî İETT SOAP servisi önceliklidir. Kullanıcının sağladığı otobusum-anlik-main.zip uyarlamasına ait MIT bildirimi korunmuştur.
- ESHOT resmi CSV: https://openfiles.izmir.bel.tr/211488/docs/eshot-otobus-hat-guzergahlari.csv , aynı klasörde `eshot-otobus-hatlari.csv` ve `eshot-otobus-duraklari.csv`. Lisans: https://acikveri.bizizmir.com/tr/license . Kaynakta yuvarlanmış koordinatlar hassaslaştırılmaz. Durakları hatta/yöne göre tam sıralı eşleştiren veri bu sürümde yoktur.
- Konya: mevcut Kasım 2025 GTFS şekilleri ve ATUS güncel varış sayfası ayrı kaynaklardır. GTFS geometrisi güncel seferin çalıştığını kanıtlamaz. Sayısal durak kodunun GTFS son harfli kaydı yalnız tek eşleşme varsa konuma dönüştürülür; belirsiz durumlarda pin üretilmez.
-https://github.com/metkm/otobusum-anlik (MIT, Copyright 2025 metkm) içindeki API sözleşmeleri uyarlandı. Arşiv SHA256 ve kapsam data/raw/otobusum-integration.json içinde. Çalışan GET uçları: `https://otobusum.metkm.win/v1/stop/{code}/arrivals?city=istanbul`, `/v1/route-stops/{line}?direction=G|D&city=istanbul`, `/v1/bus-locations/{line}?city=istanbul`. Bu servis belediyenin resmî alan adı değildir; aracı Otobüsüm Anlık hizmetidir. Hat GPS birincil kaynağı İETT açık SOAP servisidir.

Alan eşlemesi: arrivals `door_no,line_code,route_code,minutes_left,lat,lng,last_location_time`; GPS yedeği `bus_id,route_code,lat,lng,last_update_time`; hat durakları `code,name,lat,lng`. Dakika yalnızca aynı araç + hat + güzergâh kodu eşleşmesinde GPS satırına taşınır. Zaman boşsa veya yalnız saat varsa gün/tarih uydurulmaz; tam ISO UTC/offset zamanlar saat dilimiyle ayrıştırılır. Hat durakları 24 saat/8 anahtarla sınırlandırılmış bellek önbelleğinde; JSON ve sonuç sayısı sınırları vardır. İETT ön plan yenilemesi 60 sn.

NTC OAuth kolu boş client ayarları nedeniyle etkinleştirilmedi. Arşivdeki gömülü servis kullanıcı adı/parolası kopyalanmadı veya kullanılmadı. Diğer uygulamanın React Native UI/MapLibre bağımlılıkları eklenmedi. Yaklaşık 2 MB olan çok varyantlı yol geometrisi yanıtı eski cihazda yüklenmez; haritada yalnız hat duraklarının gerçek koordinatları gösterilir. Tramvay/metro tahminleri ve OSM karo politikası değişmedi.

İETT arayüz düzeltmesi: Snapshot filo GPS konumları ile durak varış satırlarını ayrı tutar. `/v1/stop/{code}` içindeki `buses` listesi hat-durak eşleşmesini doğrular (24 saat, 16 durak bellek önbelleği). 224672 Kadıköy Çarşı listesinde 500T yoktur; 205471’de 500T kaynak dakikası vardır. Hat seçimi dakika listesini filtreler, tüm filonun GPS kayıtları dakika satırı gibi sunulmaz. Uygun durak seçimi için aramalı Hat durağı seç diyaloğu eklendi. HTTP 500/502/503/504 için GET isteği en fazla bir kez, 350 ms sonra tekrarlanır; diğer hata türleri sonsuz denemeye dönüştürülmez.

