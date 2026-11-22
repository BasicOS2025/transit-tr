# Transit TR iOS

Transit TR’nin iOS 11 ve üzerini hedefleyen UIKit uygulaması. iPhone ve iPad için tek hedef bulunur. Uygulama, paketlenmiş ulaşım verisini salt okunur SQLite veritabanında kullanır. Swift paket bağımlılığı, hesap veya kullanıcıya ait bir API anahtarı gerekmez.

## Kapsam

- Türkçe hat ve durak araması, sıralı duraklar, arşiv güzergâhları, yerel favoriler ve GPS ile yakın durak araması.
- Katalog, Ankara EGO haftalık hat tarifeleri, geçerli paketlenmiş GTFS kalkışları, raylı sistem istasyon listeleri ve kural tabanlı Mini Asistan çevrimdışı çalışır.
- Ankara canlı otobüs verisi mevcut Transit TR API’si üzerinden EGO Cep’te’den gelir. Diğer işletmecilerin sonuçları canlı kapsam ve servis durumuna bağlıdır.
- GPS bilgisi varsa araç konumları haritada açılır. Durak varışları ve ayrı filo GPS’i kendi kaynak alanlarından okunur. Ankara ve İstanbul’da hat bazında sunulan tren konumu tahminleri saat modeline göre ayrı haritada gösterilir.
- Harita altlığı OpenStreetMap’ten gelir. Güzergâh ve istasyon ağı cihazdaki veriden okunabilir. Muğla ve Eskişehir şehir seçicisinde yer alır, mevcut depoda yerel hat/durak veri paketi bulunmadığından resmî işletmeciye yönlendirir.
- Servisler ekranında yerel veri kapsamı, kaynak/lisans bilgileri ve Metro İstanbul duyuru sorgusu bulunur.

Veri üretimi mevcut Android varlıklarını kullanır. `../../android-legacy` verilerini ayrıca indirmek gerekmez. Seçilen şehir ve favoriler cihazda saklanır. Yakın durak konumu sunucuya gönderilmez. Canlı sorguda yalnızca sorgu parametreleri Transit TR API’sine gider. İstenirse HTTPS servisi `TransitTR.xcconfig` dosyasındaki `TRANSIT_API_URL` ile değiştirilebilir.

## Derleme

iOS 11 hedefi için doğrulanacak araç zinciri Xcode 14.3.1 ve macOS Ventura 13.x’tir. Python 3 gereklidir. Deponun tamamını klonla ve `ios/TransitTR.xcodeproj` dosyasını Xcode’da aç. `TransitTR` şemasını seç. iOS 11 cihazında doğrudan kurulum için Signing & Capabilities altında Apple Developer takımını ve kullanabildiğin bundle kimliğini ayarla.

İmzasız paketi oluşturmak için depo kökünden:

```sh
ios/build_ipa.sh --unsigned TransitTR-v1.0-ios-unsigned.ipa
```

Bu paket sonradan imzalanmak üzere hazırlanır. Cihaza kurulum için geçerli imza ve cihaza uygun provisioning profili gerekir.

Takım ve profil yapılandırıldıktan sonra, Xcode 14’ün uygun dağıtım yöntemi için dışa aktarım seçeneklerini kullan:

```sh
ios/build_ipa.sh --signed-export ExportOptions.plist TransitTR-v1.0-ios.ipa
```

Apple’ın Eylül 2026’dan beri uyguladığı App Store Connect alt hedefi iOS 13’tür. iOS 11 derlemesi cihaz veya izinli dağıtım amacıyla ayrı tutulur.

## Cihazda doğrulama

Ankara `10940`, İstanbul `205471`, Kocaeli `80163` ve Konya `27` için Otobüs ekranını aç. Ardından uçak modunda şehir değiştir, katalogda ara, favori ekle ve tarife aç. Konumu reddetme ve izin verme akışlarını, küçük bir iPhone’da Türkçe etiketleri ve iPad’de yatay görünümü kontrol et. Kaynak servisin anlık sonuçları saat, bölge ve bağlantıya bağlı olarak değişir.

Veri atıfları, kaynak tarihleri ve üçüncü taraf bildirimleri deponun [THIRD-PARTY-NOTICES.md](../THIRD-PARTY-NOTICES.md) dosyasındadır.

## Bildirimler ve widget

- Durak ayrıntısındaki **Planlı kalkışlar** veya Ankara EGO hat çizelgesindeki saatlerden birini seç. Kalkıştan 2, 5 veya 10 dakika önce yerel bildirim kurulabilir. Bildirim cihazdan planlanır ve canlı gecikme verisine göre yeniden zamanlanmaz.
- Canlı sorgudan sonra **Widget’a sabitle** seçeneğiyle durak ve isteğe bağlı hattı seç. Widget, uygulamanın son sorgusunu gösterir ve iki dakikadan eski süreleri gizler. Widget’a dokununca uygulama sabitlenmiş sorguyu yeniler.
- iOS 11–13 için Bugün görünümü ve iOS 14+ için WidgetKit hedefi bulunur. iOS 14–17’de eski Bugün bileşeni de sistem tarafından sunulabilir.
- Uygulama ve iki widget hedefi geliştirici hesabında aynı App Group ile imzalanmalı. Varsayılan grup `group.org.transittr.ios` değeridir. Kendi geliştirici hesabında farklı kimlik gerekiyorsa `TransitTR.xcconfig` dosyasında `TRANSIT_BUNDLE_ID` ve `TRANSIT_APP_GROUP` değerlerini birlikte güncelle. Her hedef için uygun provisioning profili gerekir.
