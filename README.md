# Transit TR 🚍

Türkiye’de şehir içi ulaşım bilgilerine erişmek için geliştirilen açık kaynak Android uygulaması.

Eski cihazlarda çalışmaya ve düşük bellek tüketimine odaklanır. Android 2.2 desteği hedeflenir; Android 2.2 / API 8 ARM emülatöründe test edilmiştir.

## Özellikler

- Hat ve durak arama
- Favorilere ekleme
- İnternet olmadan kullanılabilen yerel kataloglar
- Desteklenen şehirlerde canlı otobüs sorgulama
- Geçerli veri bulunan duraklarda planlı kalkış saatleri
- Koordinatı bulunan kayıtlarla yakındaki durakları bulma
- GPU veya yapay zekâ modeli gerektirmeyen kural tabanlı mini asistan

Projede ayrıca Web/PWA istemcisi ve isteğe bağlı Go backend bulunur. Aşağıdaki canlı bağlantı testleri Android uygulamasına aittir.

## Şehir desteği

| Şehir | Katalog | Canlı bağlantı |
|---|---|---|
| Ankara | 665 hat/yön, 2.813 adlandırılmış durak | EGO tahmini varış dakikaları |
| İstanbul | İETT ve raylı sistem kataloğu | İETT otobüs varış dakikaları |
| İzmir | Metro, tramvay ve eski İZBAN kataloğu | ESHOT yaklaşan araçlar ve kalan durak sayısı |
| Konya | Statik GTFS hat/durak kataloğu | ATUS varış dakikaları |
| Kocaeli | Henüz eklenmedi | Henüz eklenmedi |
| Muğla | Henüz eklenmedi | Henüz eklenmedi |
| Eskişehir | Henüz eklenmedi | Henüz eklenmedi |

Kataloglar bütün şehirleri ve ulaşım türlerini eksiksiz kapsamaz. Hat/yön ve durak kayıt sayıları benzersiz fiziksel hat veya durak sayısı değildir.

**İzmir’de kalan durak sayısı dakika olarak gösterilmez. Planlı kalkış saatleri de canlı varış tahmini değildir.**

## Canlı sorgulama

1. Şehri seç.
2. **Resmî → Seçili şehir · canlı otobüs** bölümünü aç.
3. Otobüs durak numarasını gir.
4. **Kontrol et** düğmesine bas.

İzmir sorgusunda ESHOT otobüs durak numarası kullanılmalıdır; metro istasyon kodları bu servise ait değildir.

Android canlı sorguları cihazdan doğrudan yapılır. WebView veya haricî geçit sunucusu kullanılmaz. Veriler yalnızca kullanıcı sorgu başlattığında yenilenir.

## Android 2.2 test sonuçları

Android 2.2 / API 8 ARM emülatöründe test edildi:

- Dört şehirde gerçek canlı sorgu: **4/4 başarılı**
- Ayrıştırma kontrolleri: **6/6 başarılı**
- En yüksek örneklenmiş süreç PSS belleği: **13.598 KiB ≈ 13,9 MB**

Bu değer emülatörün toplam belleği değil, uygulama sürecinin ölçümüdür. Bütün cihazlar ve anlık bellek zirveleri için garanti değildir.

Ayrıntılar: [Test raporu](docs/TEST-REPORT-v09.md)

## Bilinen sınırlamalar

- Ankara durak kataloğu kısmi bir OSM anlık görüntüsüdür. Bu kayıtlarda koordinat bulunmadığından yakın durak mesafe aramasında kullanılmaz.
- Ankara hat-durak eşleşmeleri henüz eklenmedi.
- İZBAN takvimi eski olduğundan güncel tarife gibi kullanılmaz.
- Kart bakiyesi ve bütün şehirleri kapsayan canlı GPS haritası tamamlanmadı.
- Fiziksel ARMv6, ARMv7 ve ARM64 cihazlarda ayrıca test yapılması gerekir.
- Veri kaynakları değişebilir veya geçici olarak erişilemez olabilir.

**Mevcut sürümde HTTPS sertifika ve alan adı doğrulaması kapalıdır; uygulama sunucu kimliğini doğrulamaz.** Eski Android cihazlar için uygulama içinde Java TLS 1.2 desteği bulunur.

## Derleme

Gerekli araçlar ve ortam değişkenleri:

[Android derleme rehberi](docs/BUILD-v09.md)

Uygulama:

    python tools/build_android.py

Yerel katalogları yeniden oluşturma:

    python tools/build_catalog.py

## Kaynaklar ve teşekkürler

- [EGO Genel Müdürlüğü](https://www.ego.gov.tr/)
- [İETT](https://iett.istanbul/)
- [İzmir Açık Veri Portalı](https://acikveri.bizizmir.com/)
- [Konya ATUS](https://atus.konya.bel.tr/)
- [OpenStreetMap](https://www.openstreetmap.org/copyright)
- [byigitt/egomac](https://github.com/byigitt/egomac): EGO mobil servis keşifleri ve OSM durak adı kaynağı
- [caglarsarikaya/postman-collections](https://github.com/caglarsarikaya/postman-collections): İstanbul ve İzmir API başvuru koleksiyonları
- [Bouncy Castle](https://www.bouncycastle.org/): Eski Android için TLS desteği

Bu proje belediyelerin veya ulaşım işletmecilerinin resmî uygulaması değildir.

## Lisans

Proje kodu [Apache License 2.0](LICENSE) ile lisanslanmıştır.

Üçüncü taraf bileşenler ve veriler kendi lisanslarına tabidir. Ayrıntılar için [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md) dosyasına bakın.

© OpenStreetMap contributors. OSM kaynaklı veriler ODbL koşullarına tabidir.
