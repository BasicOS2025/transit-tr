# Transit TR v0.7.1 — bu PC'deki testler

4 Kasım 2026, Europe/Istanbul. Tüm işlemler bu Windows PC üzerinde yapıldı.

## Sonuçlar

| Test | Sonuç |
|---|---|
| JavaScript çekirdek | Geçti |
| SQLite / web katalog eşleşmesi | 6 test geçti |
| EGO Java form ve ayrıştırma | 12 kontrol geçti |
| Chrome EGO entegrasyonu (kontrollü veri) | 2 test geçti |
| Go backend | 11 test geçti |
| Tam web uçtan uca | Geçti; WEB_EXIT 0 |
| Gerçek EGO, Chrome | Geçti |
| Gerçek EGO, Go API → Chrome işçisi | HTTP 200 |
| Android API 36 x86_64 emülatörü | Açılış, veritabanı, M2 araması, yakın duraklar, tarife SQL, Konya ve 100 sonuç ayrıştırma çağrısı geçti |
| Android doğrudan EGO HTTPS | Başarısız: sonuç alanı bulunamadı |

Canlı API kanıtı: 10001 durağı, 591 hattı, KORU METRO İST.-TEMELLİ yönü; kaynak zamanı 04.11.2026 20:44:56. Gösterilen süre geçmiş test anına aittir, güncel varış bilgisi değildir. Plaka ve IP uygulama sonucuna aktarılmadı.

## RAM

Güncellenmiş APK'da örneklenmiş en yüksek toplam süreç PSS: **34.546 KiB = 35,38 MB**. Çevrimdışı bölümde 32.521 KiB; başarısız doğrudan EGO isteğinde 34.546 KiB. 200 MB testi bu senaryolarda geçti. Bu ölçüm başarılı Android canlı sorgusunu, PC Chrome süreçlerini veya emülatörün toplam RAM tüketimini kapsamaz. Emülatöre 2 GB RAM ayrıldı; bu uygulamanın RAM tüketimi değildir. Gerçek ARM telefon ve Android 2.2 bu oturumda test edilmedi. Ölçüm tüm cihazlarda kesin bellek kotası garantisi değildir.

## Yapılan düzeltmeler

EGO formunun gönderim adresi sabit /index yerine güncel formdan okunur. Java istemcisi yalnızca /otobusnerede/ altındaki sınırlı biçimdeki göreli yolları; Chrome işçisi aynı kaynak ve yol kurallarını kabul eder. Güncel /sorgula adresi ve değişen metin alanı adıyla Chrome canlı sorgusu doğrulandı. Android form gönderimi aşamasını geçti, ancak varış verisi yine alınamadı; tarayıcısız canlı bağlantı tamamlanmış sayılmaz.

Windows katalog testinde UTF-8 açıkça belirtildi. Web testinin ekran görüntüsü yolu taşınabilir hale getirildi. Android test raporu gerçek API sürümünü gösterir. Sürüm 0.7.1 olarak işaretlendi.

## Web kapsamı

360×760 Chrome görünümü; hat/durak araması, detay, favoriler, simüle GPS, elle girilen konumla çevrimdışı yakın durak, tarife paneli, çevrimdışı yeniden yükleme, şehir değiştirme ve asistan doğrulandı. Tarayıcı JavaScript hatası görülmedi. İBB hizmet duyuruları canlı kaynaktan alındı. TAKSİS için yetkili token bulunmadığından gerçek yetkili durak servisi yerine yetki gereksiniminin doğru gösterilmesi test edildi. Kart bakiyesi, tüm şehirlerde canlı konum ve gerçek Windows GPS testi yapılmış değildir.

## Dosyalar

Transit-TR-v0.7.1-modern-test.apk: Android 6+ için bu PC'de yeniden derlendi, geçici test anahtarıyla imzalandı ve Android 16/API 36 emülatöründe test edildi. Yayın anahtarı değildir; önceki imzalı APK üzerine güncelleme olarak yüklenmeyebilir. Legacy APK yeniden üretilmedi.

Transit-TR-v0.7.1-source.zip: değişiklikleri içeren kaynak kod. Node/Go/SDK ve özel test imza anahtarı dahil değildir. Windows testleri için kullanılan build-tools 36.1.0 ve JDK 25 ile modern APK üretildi; kaynakta bulunan eski Linux derleme betiği ayrıca korunur.

Ham test kanıtları bu raporla aynı klasördedir. EGO erişimindeki ara DNS/TLS sorunları nedeniyle bazı ilk denemeler başarısız oldu; son web ve backend sonuçları başarılı çalıştırmaları gösterir.
