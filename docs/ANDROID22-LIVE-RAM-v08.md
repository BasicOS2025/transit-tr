# Transit TR v0.8 — Android 2.2 başarılı canlı sorgu RAM testi

4 Kasım 2026. Test bu Windows PC'de Android 2.2/API 8 ARM emülatöründe yapıldı. Android 16 ölçümü değildir.

## Sonuç

**Canlı EGO sorgusu Android 2.2'de başarılı.** Cihaz EGO mobil JSON servisine doğrudan bağlandı. Chrome, WebView, bilgisayardaki geçit veya haricî uygulama sunucusu kullanılmadı.

| Ölçüm | İlk başarılı sürüm | Teslim edilen son sürüm |
|---|---:|---:|
| Android | 2.2 / API 8 | 2.2 / API 8 |
| Durak | 10001 | 10001 |
| Canlı hat | 591 | 591 |
| En yüksek örneklenmiş süreç PSS | 6339 KiB / 6,491 MB | 6196 KiB / 6,345 MB |
| Bellek örneği | 5 | 22 |
| Toplam test süresi | 2231 ms | 2399 ms |
| Gerçek canlı ETA | 1 dk 41 sn | 0 dk 38 sn |
| Sonuç | LIVE_SUCCESS=true | LIVE_SUCCESS=true |

Son testte alınma zamanı: 04.11.2026 21:05:09, Türkiye saati. Bu süreler test anına aittir; güncel varış sonucu değildir. İlk denemedeki eski saat dilimi verisi nedeniyle alınma saati bir saat geri görünüyordu; son sürümde GMT+03:00 kullanılarak düzeltildi.

Son testte örnekleme uygulama açılmadan başlar, açılışı ve gerçek ağ isteğini kapsar, 100 ms aralıklarla Android Debug.getMemoryInfo().getTotalPss() değerini toplar. LIVE_SUCCESS yalnızca gerçek JSON sorgusundan canlı varış alanı elde edildiğinde true olur. Hata sayfası veya yalnızca tarife verisi başarılı canlı ETA sayılmaz. PSS süreç ölçümüdür; emülatöre ayrılan 128 MB RAM ile aynı şey değildir. Bu senaryoda 200 MB hedefinin altında kaldı; örnekleme, tüm anlık zirveleri ve bütün fiziksel telefonlardaki tüketimi garanti etmez.

## egomac uyarlaması

Kaynak: https://github.com/byigitt/egomac (MIT). EGOClient.swift ve API keşif notlarındaki mobil servis çağrısı Java 6/API 8'e uyarlandı. macOS arayüzü ve macOS bildirim altyapısı aktarılmadı. Android'in yerel JSON ayrıştırıcısı kullanılır; yeni üçüncü taraf çalışma zamanı eklenmez.

Uç nokta: https://egocptsrvand.ego.gov.tr/mblSrv14/service.asp?FNC=Otobusler&VER=3.1.0&LAN=tr&DURAK=10001

Canlı satırlar ile tarife satırları ayrılır; saniye=999999 geçmiş araç olarak ele alınır. Eksik süre sıfır dakika yapılmaz. Türkçe yanıtlar için UTF-8 / Windows-1254 desteği vardır. Kullanıcıya hat, yön, varış ve zaman bilgisi gösterilir; plaka, IP ve GPS koordinatları ekrana aktarılmaz. Yanıt 256 KiB ve 100 satırla, istekler zaman aşımıyla sınırlıdır. Sorgu yalnızca kullanıcı başlatınca yapılır.

Kullanıcının açık isteği üzerine uygulama HTTPS sertifika zinciri ve alan adı doğrulaması kapalıdır. Bu sürüm sunucu kimliğini doğrulamaz. Bu ayar TLS protokolünün şifreleme sürümünü yükseltmez.

## Derleme ve test

Resmi Android 2.2 r03 android.jar, ECJ 3.33.0 Java 6 hedefi, Android dx 30.0.3 ile DEX üretildi. SDK 36.1.0 aapt kısa Windows dosya yollarıyla kullanıldı; APK minSdk=8 ile test anahtarıyla imzalandı. API 8 emülatörüne kuruldu. Manifest minSdk=8 / targetSdk=8. Kaynak sürümü 0.8.0.

APK: Transit-TR-v0.8-android22-test.apk. Deneme imzası önceki indirilen APK'dan farklı olabilir; eski imzalı uygulamanın üzerine güncelleme olarak kurulamayabilir. Fiziksel ARMv6/ARMv7 cihaz testi yapılmadı. Son kaynak değişikliklerinden sonra web/Go testleri yeniden çalıştırılmadı; bu sürümün doğrulaması Android 2.2 canlı sorgusudur.

Ham kanıt: Transit-TR-Android22-Live-RAM.txt. İlk başarılı test: Transit-TR-Android22-Live-RAM-first.txt. Test kaynak kodu android-tests/src/org/transittr/tests/LiveRam.java. MIT atfı THIRD-PARTY-NOTICES.md dosyasındadır.
