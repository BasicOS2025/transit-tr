# Transit TR 1.1.0 — doğrulama raporu

7 Kasım 2026, Windows PC üzerinde görünen Android 2.2/API 8 ARM ve Android 6.0/API 23 x86 emülatörleri. Fiziksel telefon ölçümü değildir.

## Durak ve aynı haritada rota

`RoutePinCheck`: dört şehrin gerçek durak koordinatları ve seçilen hatların çizilebilir verileri doğrulandı:

| Şehir | Durak | Doğrulanan hat | Çizgi kaynağı |
|---|---|---|---|
| Ankara | 10940, 1231 SK. | 263-7 | EGO sırası + OSM konumları; şematik, 99/100 koordinat |
| İstanbul | 224672, Kadıköy Çarşı | 500T | Otobüsüm Anlık / İETT geometrisi, 80 durak |
| İzmir | 21050, Münevver Öğretmen Ortaokulu | 446 | Resmî ESHOT CSV geometrisi |
| Konya | 27, Zafer 1 | 6-A | Mevcut Konya GTFS geometrisi |

Gerçek Ankara durak sorgusu yapıldı. Canlı listedeki mavi hat numarasının gerçek `performClick` olayı rotayı aynı ana haritaya açtı; Activity ekranı 0 olarak kaldı. Sorgulanan 10940 pininin korunduğu, rota etiketiyle çizgi temizlenince pinin kaldığı doğrulandı. Durağın adı/numarası otobüs işaretlerinden ayrı siyah pin ile çizilir. Durak değiştirilince eski sonuç/pin temizlenir. Koordinat bulunamazsa sahte veya otobüsten türetilen bir durak işareti üretilmez.

## Katalog ve saatler

Her iki sürümde aynı uygulama içindeki katalog açıldı, ilk 50 sonuçtan kaydırma ile 100 sonuca geçildi, `263-7` araması tek sonuç verdi, yerel hat ekranı ve üç günlük EGO saat penceresi açıldı. EGO `481` saatleri ağdan zorla yenilendi; eski önbelleğe düşmediği ayrıca kontrol edildi.

PC eşitlemesi: 665/665 EGO hat/yönü, sıfır indirme hatası. 34.657 günlük saat girdisi, 52.079 sıralı durak girdisi. 20 hat kaynakta saat yayımlamıyor. Saatlerin yanındaki özel başlangıç/durak notları saklanır. Altı metro/Ankaray yönü tahmin modeline bağlandı; tahmin GPS değildir.

## İstanbul canlı varış regresyonu

`IettCheck`, iki sürümde geçti: 9 ayrıştırma/eşleştirme kontrolü, tüm varışların kaydırılması (40 gerçek varış satırı), 224672+500T durağa uğramama mesajı ve durak seçici, 205471+500T gerçek dakika + ayrı filo GPS katmanı. Son sorguda API 8: 2 varış ve 17 GPS aracı; API 23: 1 varış ve 17 GPS aracı. Sonuç sayıları zamana bağlıdır.

## Bellek

100 ms aralıkla `Debug.MemoryInfo.getTotalPss()` örneklemesi; Java yığını dahil toplam süreç PSS değeridir. Tepe değerler test senaryosuna aittir; boşta tüketim veya fiziksel cihaz garantisi değildir.

| Senaryo | API 8 | API 23 |
|---|---:|---:|
| Dört şehir durak/rota + canlı Ankara + katalog/saatler | 17.870 KiB | 23.374 KiB |
| İstanbul durak/hat + kaydırma + dakika/GPS ayrımı | 8.871 KiB | 18.406 KiB |

İlk geliştirme denemesinde sıkıştırılmamış büyük katalogla emülatör depolaması doldu. Son paket SQLite verisini `.dbz` içinde GZIP ile, blok blok açar. Eski Android'in büyük sıkıştırılmış Asset sınırı nedeniyle `.dbz` APK içinde tekrar sıkıştırılmaz. APK yaklaşık 13,3 MiB, açılmış katalog yaklaşık 28,9 MiB. Katalog ilk açılıştan sonra kopyalanmaz. Eski favori kimlikleri ve SharedPreferences kayıtları korunur. Paketlenen verinin SHA256 değeri ham SQLite ile açıldıktan sonra birebir karşılaştırıldı.

## Görsel kontrol ve yerel kontroller

320 dp (API 8), 360 dp ve yaklaşık 390 dp (API 23) gerçek ekranları incelendi. Web DOM denetimi native Android Canvas/View ekranlarına uygulanamaz; gerçek emülatör ekranları ve UIAutomator hiyerarşisi kullanıldı. Küçük ekrandaki saat günü düğmeleri tek satıra düzeltildi. Durak pini, katalog, rota ve saat ekranlarının PNG dosyaları dağıtımın test klasöründedir.

6 SQLite/katalog testi ve JavaScript temel hesaplama testleri geçti. Katalog yeniden üretimi çalıştırıldı, bütünlük `ok`; yeni veriler eklendikten sonra eski kayıtların kimlik eşleşmesi korundu. APK imzaları v1/v2/v3 doğrulandı. Modern minSdk 23 / targetSdk 28; eski sürüm minSdk 8 / targetSdk 8.

## Sınırlar

- Bu test dört örnek durak ve dört hat içindir; her şehirdeki her durağın çalıştığı iddiası değildir.
- Ankara çizgisi gerçek yol geometrisi değildir; konumu eksik durakta kesilir. 481 gibi hatlarda eksik koordinatlar daha fazladır.
- İzmir dakika vermediğinde kalan durak sayısı gösterilir; GPS'ten dakika uydurulmaz. Otobüs geometrileriyle durakların sıralı/yönlü tam eşleşmesi yoktur.
- Konya GTFS 2025 tarihlidir, bazı hatlarda sıralı durak verisi yoktur. ATUS varışları canlıdır; bu uygulama Konya'da canlı GPS araç noktası üretmez.
- Kullanıcı isteğiyle sertifika ve alan adı doğrulaması kapalı bırakılmıştır.

Son küçük ekran düğmesi düzeltmesinden sonra API 8 RoutePinCheck yeniden geçti (697 örnek, 17.870 KiB tepe). Bir önceki soğuk açılışta 19.043 KiB ölçülmüştü.
