# Transit TR 1.2.0 — widget, tema, depolama ve raylı ağ

8 Kasım 2026. Yerel Java 6/API 8 uygulaması, Android 2.2 ARM ve Android 6 x86 emülatörleri. Modern paket minSdk 23 / targetSdk 28; legacy minSdk 8 / targetSdk 8.

## Değişiklikler

- Dört şehir için launcher widget’ı; kullanıcı tarafından kaydedilen en fazla 12 durak + hat çifti, boş başlangıç, elle yenileme, sayfalama ve sorguya dönüş. API 16+ boyut değişikliği; küçük/normal/büyük görünüm 1/2/3 satır. Android 2.2 launcher boyutlandırma desteği sunmadığından kompakt sabit görünüm kullanır.
- Açık/koyu tema Ayarlar’dan seçilir ve saklanır. Ana ekran, harita, katalog, kayıt düzenleyici ve widget aynı tercihi kullanır. Koyu harita mevcut OSM karolarına renk filtresi uygular; ikinci karo önbelleği oluşturmaz.
- Otobüs, Metro, Katalog ve Ayarlar menülerinde Google Material ikonları. Otobüs menüsü önceki Harita adının yerini alır.
- Karo çiziminde Bitmap/Canvas ekran yoğunluğu çakışması giderildi: açık hedef dikdörtgeni kullanılır. Otobüs ve katalog aynı OsmMapView sınıfını kullanır.
- Şematik otobüs rotası, koordinatsız durakta bölünmek yerine bilinen durakları sırasıyla tek kesintisiz çizgide birleştirir. Koordinatsız duraklara konum atanmaz; gerçek yol geometrisi varsa değiştirilmez. Otobüs ve katalog ekranlarına birlikte uygulanır.
- İstanbul: 25 hat + M2-S mekik kolu = 26 rota. M5 Sultanbeyli uzatması 24 istasyon; M2 ana rota 15 istasyon ve Seyrantepe ayrı kol. B1 Marmaray 43, B2 5, T6 8, T2 5, F2 2, F3 2 istasyon. Ankara’daki beş hat korunur.

## Depolama

SQLite 28,93 MiB’den 5,71 MiB’ye küçültüldü (5.988.352 bayt). 451.793 rota koordinatı ve 51.934 statik kalkış kaydı kimlik başına sıkıştırılmış APK varlıklarına kayıpsız aktarıldı; tümü cihazda açılmaz. Önceki veri ile her koordinat/kayıt karşılaştırıldı. Kimlikler, adlar, kullanıcı kayıtları ve arama davranışı korunur.

Harita ve rota disk önbelleği toplam 1 MiB; EGO güncellemeleri tek 1,25 MiB sınırındaki arşivde. Eski üretilmiş katalog dosyaları uygulama açılışında temizlenir; kullanıcı favorileri silinmez. Android 6 Depolama ekranında toplam 12,43 MB, uygulama 6,67 MB, veri 5,76 MB, ayrıca önbellek yaklaşık 0,98 MB ölçüldü. Derlenmiş kod dahil kök dosya sistemi ölçümü yaklaşık 17,2 MiB; önbellek ve tarife bütçeleriyle test emülatörlerinde 20 MiB hedefinin altında. İşletim sisteminin derleme dosyaları başka cihazlarda değişebilir; evrensel sabit boyut garantisi verilmez.

## Doğrulama

Gerçek AppWidgetHost, boyut seçenekleri, sayfalama, kayıt düzenleme ve satırdan doğru sorguya dönüş test edildi. Dört şehir örnekleri: Ankara 10940/481, İstanbul 205471/500T, İzmir 21050/446, Konya 27/6-A. Test kayıtları izole tercihlerde tutulur; kullanıcı favorilerine yazılmaz. Gerçek kullanıcı kaydı Ankara 10788/419 korunmuştur. API 8’de önceki dört test örneği tek seferlik temizlendi; yeni kurulum örnek oluşturmaz.

Widget canlı testinde son Android 6 testinde tepe PSS 21.862 KiB, Android 2.2 testinde 13.299 KiB ölçüldü. Bunlar RAM/PSS değeridir; depolama ölçümü değildir. İzmir kalan durak sayısını dakika diye göstermez; Konya süre yayımlamayan sonuçta “Dakika yok” yazar. Başarısız sorgu eski sonucu ve zamanını korur. Kaynak sonuçları zamana ve hizmet durumuna bağlıdır; şehirdeki her durak test edilmiş sayılmaz.

Son paket kontrollerinin ayrıntıları `v120-test-logs/` altındadır: ThemeCheck (gerçek düğmeler, kalıcılık, widget/editör/katalog), RailCheck (26 rota, M5 ve M2 topolojisi, sahte tren üretmeme), MapSeamCheck (160 dpi bitmap, 120/160/240/320 dpi canvas ve sınır pikselleri). StorageCheck her iki API’de katalog, Türkçe arama, kayıpsız varlık okuma ve önbellek sınırını doğrular.

## Kaynaklar ve sınırlar

- [Metro İstanbul M5](https://www.metro.istanbul/Hatlarimiz/HatDetay?hat=M5): Sultanbeyli uzatması 22 Mayıs 2026, 24 istasyon, 50 dakika. Mevcut M5 sıklık modeli bu güzergâhta kullanılır; gerçek kalkış veya GPS değildir.
- [Metro İstanbul M2](https://metro.istanbul/Hatlarimiz/HatDetay?hat=M2): ana hat ve Seyrantepe kolu. Kol ayrı çizilerek Hacıosman yolculuğunda yanlış sapma önlenir.
- [TCDD Marmaray](https://www.tcddtasimacilik.gov.tr/marmaray/tr/neredennereye) ve [TCDD 2025 Faaliyet Raporu](https://adminapi.tcddtasimacilik.gov.tr/files/pdfs/Faaliyet%20Raporu%202025.pdf): Marmaray ve banliyö hizmetleri.
- Yeni istasyon koordinatları/sırası © OpenStreetMap contributors, ODbL 1.0: B1 relation 9987139, B2 14039181, T6 19587363, T2 301617, F2 301616; F3 gerçek füniküler hat uçları. M5 uzatma koordinatları mevcut atıflı OSM ağ kaynağından. Kaynak tarihi ve kapsamı rail-networks JSON’unda yer alır.
- Yeni eklenen hatlarda doğrulanmış model yoksa yalnız istasyon ağı gösterilir. Harita bağlantıları şematiktir, ray geometrisi değildir. Yapım aşamasındaki M12/M14 eklenmedi.
- Kullanıcının önceki talebiyle sertifika ve sunucu adı doğrulaması kapalıdır. Bu sürüm bu davranışı değiştirmez.

## Tasarım

Mevcut sade yüzeyler ve satır düzeni korunur; hat kodu mavi, dakika sağa hizalıdır. Native RemoteViews/API 8 sınırlamaları nedeniyle widget sistem yazı tipini kullanır. Menü ikonları Canvas yollarıdır; ek raster kaynak ve kitaplık gerekmez. Koyu tema aynı görsel hiyerarşiyi korur.

Son rota testinde Ankara 263-7 için 100 duraklı şematik bağlantı tek parça olarak doğrulandı. Dört şehirde seçili durak pinleri ve çizilebilir rota alımı, mavi hat koduna dokunma, katalogdan rota açma ve EGO saat yenilemesi gerçek emülatörlerde test edildi. Sonuçlar ve ekran görüntüleri test paketindedir.

Son dört şehir rota/katalog testinin tepe PSS değerleri: Android 6 21.409 KiB (386 örnek), Android 2.2 18.676 KiB (805 örnek). Son Android 6 kök dosya sistemi ölçümü: APK/derlenmiş kod 10.768 KiB, özel veri ve mevcut önbellek 6.504 KiB, toplam 17.272 KiB (yaklaşık 16,87 MiB).
