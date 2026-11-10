# Kamuya dağıtım durumu: onaylanmadı

10 Kasım 2026. Geliştirici, GitHub/APK dağıtımını herkese açık planladığını ve sağlayıcılardan yazılı API izni bulunmadığını belirtti. Bu kayıt bir suç tespiti değildir. Açık lisansın kapsamına giren kullanımlar için ayrıca yazılı izin her durumda zorunlu değildir; ayrı özel servis erişimi veya lisans dışı kullanım için bu sonuç çıkarılamaz.

## Yayın kontrolü

Proje kökünden `python tools/legal_release_check.py` çalıştırılır. Beklenen mevcut sonuç `public_release: blocked` ve çıkış kodu 2'dir. Araç hiçbir servise bağlanmaz, sır değerlerini yazdırmaz ve dosya silmez. Bu denetim hukuki uygunluk sertifikası değildir; elle dosya kopyalanmasını veya diğer paketleme araçlarını engellemez.

Android derlemesinde `$env:TRANSIT_DISTRIBUTION='public'` ayarlandığında aynı kontrol, derleme araçları ve APK üretimi başlamadan çalışır. Değişken ayarlanmamış geliştirme/test derlemeleri bu yeni kamuya dağıtım kontrolüne tabi değildir; bunların başarılı olması yayın onayı anlamına gelmez. Web hizmetini çalıştırmak veya eski ZIP/APK'yı paylaşmak da bu kontrolü kendiliğinden çalıştırmaz.

`public-release.json` alanlarını yalnız gerçek inceleme ve kapsamı karşılayan delille doldurun. Açık lisans kaydı; tam veri kümesi, sürüm, atıf, değişiklik bildirimi ve ilgili veri tabanı yükümlülüklerini kapsamalıdır. Yazılı izin; istemci kimliği/anahtar kullanımı, çağrı sınırları, önbellek, yeniden dağıtım ve ticari kullanım kapsamını belirtmelidir. Bir kaynağın dosyadan kaldırıldığına ilişkin beyan tek başına yeterli değildir; incelenen derleme gerçekten bu entegrasyonu/veriyi içermemelidir.

İnceleme `python tools/legal_release_check.py --fingerprint` ile elde edilen kaynak/veri özetiyle bağlanır. Bu özeti doldurmak veya boolean alanları `true` yapmak izin oluşturmaz. Kimlik doğrulamasını kapatan bağlantılar ayrıca teknik yayın engelidir. Kontrol bu göstergeleri ihtiyatlı olarak işaretler; özel TLS uygulamasının güvenliği ayrıca doğrulanmalıdır.

## Henüz çözülmeyenler

- APK'dan çıkarılmış uygulama oturumu/imza/anahtarlarının üçüncü taraf istemcide kullanım yetkisi.
- Açık lisansı doğrulanmamış katalogların ve çizelgelerin yeniden dağıtımı.
- Veri sorumlusu/yayıncı kimliği ve başvuru kanalı; uygulama içinde erişilebilen, gerçek veri akışına uygun aydınlatma metni.
- TLS sertifika ve alan adı doğrulamasının yeniden etkinleştirilmesi ve Android 2.2 uyumluluk testi.
- ODbL ve çok kaynaklı veri tabanlarının somut paket üzerinde lisans değerlendirmesi.

Kodda bir açıklama eklenmesi bunları çözmez. Çözüm, ilgili entegrasyonu/veriyi kamuya dağıtılacak sürümden çıkarmak veya kapsamı uygun hak/izin ve teknik düzeltme sağlamaktır. Ayrıntılar: [hukuki inceleme](../docs/LEGAL-REVIEW-2026-11-10.md).

## 10 Kasım düzeltme durumu

Yayıncı Ata Köse ve iletişim atakose2012@gmail.com eklendi. Gizlilik/sorumluluk metinleri uygulama ve web arayüzüne yerleştirildi. Kaynakta TLS doğrulaması yeniden açıldı; API 8 derlemesi ve yerel sertifika fixture testleri geçti. Gerçek Android ağ testi henüz yapılmadı. Özellikler ve 1.4.0 / 16 sürümü korundu. Yukarıdaki izin/dağıtım ve aktarım inceleme kayıtları hâlâ tamamlanmadı; sağlayıcı izni varmış gibi onaylanmadı.
