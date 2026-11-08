# Android 1.2 derleme

Windows üzerinde JDK 25, ECJ 3.33.0, Android API 8 android.jar ve Android build-tools 36.1.0 kullanıldı. Kod Java 6/API 8’e karşı derlenir; R8 ile DEX üretilir. Modern varyantın manifesti minSdk 23 / targetSdk 28 olur. Legacy minSdk 8 / targetSdk 8’dir.

Aşağıdaki ortam değişkenlerini yerel dosyalarınıza göre ayarlayın:

```powershell
$env:ANDROID_JAR='C:\Android\platforms\android-8\android.jar'
$env:ECJ_JAR='C:\Tools\ecj-3.33.0.jar'
$env:ANDROID_MODERN_JAR='C:\Android\platforms\android-34\android.jar'
$env:BUILD_TOOLS='C:\Android\build-tools\36.1.0'
$env:JAVA_EXE='C:\Program Files\Java\jdk-25\bin\java.exe'
$env:KEYSTORE='C:\Private\release.jks'
$env:KEY_ALIAS='transittr'
$env:KS_PASS='kendi-anahtar-parolaniz'
$env:ANDROID_VARIANT='modern'
python tools/build_android.py
$env:ANDROID_VARIANT='legacy'
python tools/build_android.py
$env:ANDROID_TESTS='1'
python tools/build_android.py
```

Son komut test APK’sı üretir; normal uygulamayı yeniden derlemek için ANDROID_TESTS değişkenini kaldırın. Çıktılar `dist/transit-tr-v1.2.0-{modern,legacy,tests}.apk`. ZIP dağıtımındaki kullanıcı dosya adları daha açıklayıcıdır. Yeni derleme klasörleri her seferinde oluşturulur. aapt, zipalign ve apksigner işlemleri betik tarafından yürütülür. Windows Unicode yol sorunu için 8.3 kısa yollar kullanılır; sistemde kısa yol üretimi kapalıysa ASCII bir klasörden derleyin.

Katalog ve rota verileri: `python tools/build_route_data.py` (ham kaynaklar veri paketindedir). APK derlemesi SQLite varlığını GZIP akışı olarak `.dbz` dosyasına paketler; Android 2.2’nin büyük sıkıştırılmış Asset sınırını aşmadan disk dosyasına açar. Verinin tamamı Java belleğine alınmaz. Metro ağı: `python tools/build_rail.py`. Test paketini kurduktan sonra `python tools/run_map_tests.py emulator-5556 --output test-results`; eski katalog/canlı sorgular için `.V09Check`. Test kodu son kullanıcı APK’sına paketlenmez.

API 8 android.jar SHA256: `a1d34a01ddb9ab7222445b79893d4af584ed4dd7cd180224b8f9037fb94395aa`.

TLS kitaplığının lisansı ve yeniden üretimi için [önceki derleme rehberi](BUILD-v09.md). Özel anahtar ve SDK araçları kaynak paketine dahil değildir.

MapCheck arka plan kontrolü sırasında `RESUME_FROM_HOME` durumunu üretir; Python çalıştırıcı ADB ile uygulamayı öne getirir. Bu nedenle yaşam döngüsü kontrolü için çalıştırıcıyı kullanın.


Modern paket, `android-modern/src` içindeki API 26+ widget `JobService` kodunu API 26+ platform jar ile ayrıca derler. `ANDROID_MODERN_JAR` belirtilmezse build-tools yanındaki `platforms/android-34/android.jar` aranır. Legacy paket bu sınıfı içermez. Modern kaynak derlemesinde R8 ve aapt modern platform jar'ı kullanır; ortak Java kodu API 8'e karşı kontrol edilir.

`build_route_data.py` sonunda `compact_android_data.py` çalışır. Rota şekilleri ve durak tarifeleri, kimlik başına `.dbz` varlıklara kayıpsız aktarılır. SQLite'daki yinelenmiş normalleştirilmiş arama metni saklanmaz; Türkçe arama sorguda normalleştirilir. `.dbz` dosyaları APK içinde tekrar sıkıştırılmaz ve cihazda topluca açılmaz. Yerel katalog dosyası `transit-v120.sqlite` adını kullanır; eski türetilmiş katalog dosyaları uygulama başlangıcında kaldırılır.

Widget entegrasyon testi gerçek bir `AppWidgetHost` kullanır. API 23 emülatöründe teste geçici bağlama izni için `adb shell appwidget grantbind --package org.transittr --user 0`, test sonunda `adb shell appwidget revokebind --package org.transittr --user 0` kullanılır. Android 2.2'de sistem widget seçicisinden Transit TR seçilir. Normal kullanıcı kurulumunda launcher bu bağlamayı kendisi yapar. Testin dört şehir sorguları ayrı tercihlerde çalışır; kullanıcı favorilerine örnek hat eklenmez.
