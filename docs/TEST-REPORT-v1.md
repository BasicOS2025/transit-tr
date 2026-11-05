# Transit TR 1.0 — test raporu

5 Kasım 2026, kullanıcının Windows bilgisayarında. Gerçek ağ sorguları; sentetik sonuçlar uygulama ekranlarına verilmedi. Sabit saatli hesaplama testleri yalnızca tahmin motorunun sınırlarını doğrulayan test verileridir.

## Son APK sonuçları

| Kontrol | Android 2.2 / API 8 ARM | Android 6.0 / API 23 x86 |
|---|---|---|
| Ankara gerçek GPS | geçti, 4 araç | geçti, 4 araç |
| İstanbul gerçek GPS, 500T | geçti, 36 araç | geçti, 35 araç |
| Ön planda 20 sn yenileme | geçti | geçti |
| Arka planda 22 sn sonuç değişmiyor | geçti | geçti |
| Ana ekrandan uygulamaya dönüş | geçti, ADB açılışı | geçti |
| Material Harita ikonu | geçti | geçti |
| Metro hattı/istasyona odaklanma | geçti | geçti |
| Tahmin motoru 7 kontrol | geçti | geçti |
| Metro ağı / GPS ayrımı | ayrı haritalar | ayrı haritalar |
| En yüksek örneklenmiş uygulama PSS | 8704 KiB = 8,5 MiB | 17952 KiB = 17,53 MiB |
| 100 ms örnek sayısı | 648 | 683 |

Araç sayıları test anına aittir. PSS uygulama işlemine aittir; emülatörün PC RAM tüketimi değildir. Fiziksel cihaz, uzun süreli bellek sızıntısı testi veya tüm Android sürümleri testi yapılmadı.

Ankara 5 hat / 54 ad-koordinat kaydı; İstanbul 19 raylı hat / 250 ad-koordinat kaydı. M11 varlığı ve geçerli koordinatlar kontrol edildi. Sayılar benzersiz fiziksel istasyon sayısı olarak yorumlanmamalıdır.

Tahmin motoru: iki yön, istasyonda bekleme, kalkış öncesi/varış sonrası sınırlar, tahmin etiketi ve sabit gündüz saatinde iki şehrin tren üretimi. Ankara gerçek kalkış çizelgesi; İstanbul sıklık modeli. Araç gözlemi veya kesin varış değildir. Gecikme/iptal/resmî tatil/ek sefer tahmini yapılmaz. Ayrıntılar SOURCES-v1.md ve uygulamadaki Model düğmesinde.

## Diğer kontroller

- Android 2.2’de mevcut katalog/canlı sorgu regresyonu: Ankara, İstanbul, İzmir ve Konya 4/4. Ankara katalog 665 hat/yön ve 2813 koordinatsız durak adı.
- 6 katalog testi ve JavaScript çekirdek testleri geçti.
- Metro veri paketleme kontrolleri: benzersiz hat ID, geçerli istasyon ad/koordinatları, sıralı kalkışlar, iki yön/üç gün tipi ve süre sınırları geçti.
- Modern APK minSdk 23/targetSdk 28; legacy minSdk 8/targetSdk 8. İmzalar apksigner ile v1/v2/v3 olarak doğrulandı. API 8 android.jar’a karşı derlendi.
- Ayrı metro ekranı, otobüs GPS ekranı, Material harita ikonu ve Model açıklaması gerçek emülatör ekran görüntülerinde incelendi. 320dp, 360dp ve 390dp genişlikler kontrol edildi. Web DOM inceleyicisi yerel Android Canvas/Views için uygulanabilir değildir.
- İlk arka plandan dönüş testleri Android 2.2’de test etkinliği kendi arka plan bağlamından açmaya çalıştığında başarısızdı. Çalıştırıcı artık ana ekran dönüşünü ADB ile tetikler ve ActivityMonitor ile dönen etkinliği izler; son test geçti. Uygulama APK’sı bu test düzeltmesi için değiştirilmedi.

## Görsel kararlar

Tasarım becerisi: achromatic-ledger, seed 1962304054. Ana soru Harita’da “Otobüs nerede ve kaç dakika?”, Metro’da “Hangi hatta, saate göre tren nerede olabilir?”. Ürünün görsel öğesi şehir haritası üzerindeki ulaşım ağıdır. Geist paketlenmiştir; beyaz/nötr yüzeyler, koyu temel eylem, veride hat renkleri. Renk bütçesi istisnası resmî hat kimlikleri ve OSM kartografisidir. Material 24dp ikon kullanıcı talebidir. Sahte KPI, görsel tren GPS iddiası veya örnek araç verisi yoktur.

## Sınırlar

İstanbul sıklık modeli gerçek tekil kalkış listesi değildir; pik sıklık tüm gün için sabit varsayılır. M11’in sıklığı/penceresi model varsayımıdır. Ankara kısmi ara istasyon başlangıçları modele dahil edilmedi. Yerel veri kümesi otomatik güncellenmez. İzmir GPS ayrıştırıcısı var ama harita GPS çalışma testi bu sürümde Ankara/İstanbul ile sınırlı; Konya GPS haritası yoktur. Yoğun hatlarda işaretler örtüşebilir; harita yakınlaştırılabilir.

Sertifika ve alan adı doğrulaması önceki açık kullanıcı talebiyle kapalıdır. Paketler test anahtarıyla imzalıdır; üretim imza anahtarı dağıtılmaz. Play Store yayını hazırlanmadı.

Ham sonuçlar: Transit-TR-v1-API8-test.txt, Transit-TR-v1-API23-test.txt ve Transit-TR-v1-API8-catalog-live.txt.
