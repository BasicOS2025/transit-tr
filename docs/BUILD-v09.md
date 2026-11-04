# Android v0.9 derleme

Gereken ortam değişkenleri: ANDROID_JAR (API 8 android.jar), ECJ_JAR (ECJ 3.33.0), BUILD_TOOLS (36.1.0 klasörü), KEYSTORE, KS_PASS, KEY_ALIAS. İsteğe bağlı JAVA_EXE: JDK java yürütülebilir dosyası.

Windows veya Linux: `python tools/build_android.py`. Varsayılan çıktı dist/transit-tr-v0.9-legacy.apk. Test APK'sı için ANDROID_TESTS=1 ortam değişkenini ayarlayın; çıktı dist/transit-tr-v0.9-tests.apk. Modern manifest için ANDROID_VARIANT=modern seçeneği vardır; v0.9 modern cihazda test edilmedi.

Test: `adb install -r dist/transit-tr-v0.9-legacy.apk`, ardından test APK'sını kurun ve `adb shell am instrument -w org.transittr.tests/.V09Check` çalıştırın. V09Check gerçek ağ kullanır; örnek durağın o an araç göstermemesi veya servisin kapalı olması testin başarısız olmasına yol açabilir.

Katalog: `python tools/build_catalog.py`. Ağ kullanmaz. Ham Ankara girdileri data/raw/ankara-lines.json ve ankara-stops-osm.json içinde; diğer şehirlere ait ham girdiler korunmuştur. Derleme öncesi güncellenmiş katalog üretilmelidir.

Hazır android-legacy/libs/transit-tls.jar, Bouncy Castle 1.86 bcprov/bctls/bcutil jdk15to18 JAR'larının yeniden adlandırılmış halidir. Lisans aynı klasördedir. Yeniden üretmek için resmi orijinal JAR'ları proje kökündeki vendor-inputs klasörüne bcprov.jar, bctls.jar, bcutil.jar adlarıyla koyup `python tools/shade_tls.py` çalıştırın. Bu özgün JAR'ların SHA256 değerleri:

bcprov: fc50334d4d87b4272e72fa95ca748ead05bdef02ef760a5b13f64ffee317cd81
bctls: c0dbcd24ceeb78a263661a38d243a3c82620f002a41f49c5399a9635764a963a
bcutil: e72ef8a0fcad295207529b268ee02eb72acc7c83ec2e3a9df90cbed9380e3990

Dağıtım: https://repo.maven.apache.org/maven2/org/bouncycastle/ . İmza özel anahtarı kaynakta yoktur. tools/build_android.py her seferinde yeni bir derleme klasörü oluşturarak eski .class dosyalarını tekrar paketlemez.
