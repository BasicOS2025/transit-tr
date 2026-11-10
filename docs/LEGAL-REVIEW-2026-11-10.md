# Transit TR — Türkiye hukuku açısından yayın öncesi inceleme

**Sonuç: mevcut sürüm için kamuya dağıtım uygunluğu doğrulanmadı.** İnceleme tarihi 10 Kasım 2026. Kullanıcının beyanı: herkese açık GitHub/APK dağıtımı; yazılı servis izni yok. Kaynakta Android manifesti 1.4.0, README başlığı 1.3.0 test sürümü gösteriyordu. İnceleme kaynak kod ve yerel dosyalara dayanır; mevcut APK'ların aynı kaynakla üretildiği doğrulanmadı. Canlı sağlayıcılara test isteği gönderilmedi. Bu belge teknik risk değerlendirmesidir; somut olay hakkında bağlayıcı hukuki mütalaa veya suç tespiti değildir.

## Sonraki düzeltmeler — 10 Kasım 2026

Kullanıcı özelliklerin korunmasını istedi. Hiçbir sağlayıcı, şehir veya özellik çıkarılmadı. Manifest versionName **1.4.0**, versionCode **16** korundu. Yayıncı **Ata Köse**, iletişim **atakose2012@gmail.com** olarak dolduruldu. `docs/PRIVACY-TR.md` ve `docs/TERMS-TR.md` Android varlıklarına ve web arayüzüne eklendi; Android başlığından/Ayarlar’dan, web alt bilgisi/Ayarlar’dan açılır. Fork/kötüye kullanım için hukuken izin verilen en geniş sorumluluk sınırlaması; kasıt, ağır kusur ve emredici hükümler istisnasıyla yazıldı. Apache lisansının hakları daraltılmadı.

Android’de trust-all sertifika/alan adı atlaması kaldırıldı. `TlsSecurity` sistem güven deposu ve yalnız açıkça paketlenmiş ISRG kökleriyle zincir doğrular. API 8 özel TLS callback’i aynı kontrolü ve DNS SAN eşleşmesini uygular. Web HTTPS istemcisinde `rejectUnauthorized:true` kullanılır. Web sorgu belleğine süre sonu temizliği eklendi. Aşağıdaki TLS-kapalı bulgusu ilk inceleme anını anlatır; mevcut kaynakta bu davranış düzeltildi. Eski APK’lar bu kaynak değişiklikleriyle otomatik güncellenmez.

Doğrulama: bütün ortak Android Java kaynakları API 8’e karşı hatasız derlendi (13 uyarı); web sunucusu/taşıma dosyaları sözdizimi denetiminden geçti. Ağsız, üretilmiş sertifikalarla yerel JVM testinde güvenilir zincir kabulü; yanlış alan adı, çok düzeyli wildcard, süresi geçmiş, güvenilmeyen ve yalnız CN içeren sertifikaların reddi geçti. İki ek CA kökünün öz imzası kontrol edildi. Bu kontroller gerçek Android TLS yığını/emülatör ağı testinin yerine geçmez. Bu yan çalışmada yeni APK üretilmedi.

**Açık kalan dış koşullar:** sağlayıcı erişim/yeniden dağıtım yetkileri, gerçek kamuya açık barındırmanın yurt dışı aktarım düzeni, karışık veri tabanı/ODbL yükümlülükleri ve OSM önbellek politikasının somut kullanımda doğrulanması. Kullanıcı onayı sağlayıcı izni değildir. Özellikler korunduğu için özel entegrasyonlar çıkarılmadı; bunların hukuki durumu kodla çözülmüş sayılmadı. Yayın öncesi kontrol bu nedenle engel vermeye devam eder.

## 1. APK incelemesi ile servis erişim yetkisi ayrı konular

FSEK m.38, belirli koşullarda programı gözlemleme ve bağımsız programla ara işlerlik için sınırlı çözümleme imkânları tanır. Meşru kullanım, gereklilik ve elde edilen bilginin kullanım sınırları somut olarak incelenmelidir. Bir APK'nın indirilebilmesi veya açık kaynakta API örneği bulunması, bütün kodunu/varlıklarını kopyalama ya da sunucusuna başka istemci kimliğiyle erişme izni değildir. Koşullar bakımından FSEK m.71–72 de ayrıca değerlendirilmelidir; her tersine mühendislik işlemi kendiliğinden suç değildir. [FSEK güncel metin kaynağı](https://mevzuat.adalet.gov.tr/mevzuat/104458?query=Madde+21), [m.38 düzenlemesini içeren 4110 sayılı Kanun](https://www5.tbmm.gov.tr/tutanaklar/KANUNLAR_KARARLAR/kanuntbmmc078/kanuntbmmc078/kanuntbmmc07804110.pdf).

Kaynak göstergeleri: `AsisAuth.java`, `KentWebSignature.java` uygulama imza biçimlerini yeniden üretir; `EshotClient.java` uygulama giriş akışı kullanır; `bursa-bootstrap.json` ve web hizmetindeki bootstrap dosyaları sağlayıcıya ilişkin kimlik bilgisi içerir. Bunların bazısı teknik olarak “public key” olabilir; her anahtar parola veya ticari sır değildir. Ancak üçüncü taraf istemcide kullanım/dağıtım yetkisi için kayıt bulunmadı. Sır değerleri rapora alınmadı.

TCK m.243 için belirleyici unsur hukuka aykırı erişimdir; teknik olarak 200 yanıtı alınması bu unsuru ortadan kaldırmaz. TCK m.244 bakımından hizmeti engelleme/bozma ve veri müdahalesi ayrı fiillerdir; normal bir sorguyu sırf otomatik olduğu için bu suçla eşitlemek doğru değildir. Kota, yetki veya erişim reddini aşmaya yönelik kullanımlar yapılmamalıdır. [TCK m.243–244](https://mevzuat.adalet.gov.tr/mevzuat/103228).

**Gerekli çözüm:** her özel servis için kullanım koşulu/sözleşme veya açık izin kapsamını doğrulamak; aksi durumda bu istemciyi ve kimlik bilgilerini kamuya dağıtılacak sürümden çıkarmak. İmzayı sunucuya taşımak, gizlemek veya obfuscation uygulamak yetki eksikliğini gidermez. Kişisel kullanım da otomatik yetki oluşturmaz.

## 2. Kod lisansı veri tabanı lisansı değildir

`LICENSE` Apache-2.0'dır; bu, başka hak sahibine ait veri veya kodu yeniden lisanslamaz. `THIRD-PARTY-NOTICES.md` EGO çizelgelerinde ayrı açık lisans belirtilmediğini kaydeder. `catalog-stream-*`, `city-catalog-*`, `ego-schedules.dbz` ve web `data/` paketleri yalnız API isteği değil, yeniden dağıtılan veri içerir. FSEK m.6 ve Ek m.8 kapsamında eser niteliğindeki derlemeler ile esaslı yatırım içeren veri tabanlarının korunması ayrıca değerlendirilir. Her tekil otobüs saati otomatik olarak telif eseri değildir; buna karşılık tüm veri tabanının serbestçe yeniden dağıtılabileceği de çıkarılamaz. [FSEK](https://mevzuat.adalet.gov.tr/mevzuat/104458?query=Madde+21), [veri tabanı düzenlemesinin resmî metni](https://cdn.tbmm.gov.tr/KKBSPublicFile/D22/Y2/T1/KanunMetni/04a01fb6-e7ab-4fcc-94b1-16b4b102e8df.html).

İzmir Açık Veri Portalı, aksi belirtilmeyen materyaller için CC BY 4.0 bildiriyor. Atıf ve değişiklik bilgisiyle ilgili yükümlülükler korunmalı; bu lisansın ayrı ESHOT uygulama API'sine veya bütün belediye sistemlerine uygulanacağı varsayılmamalıdır. İBB lisans sayfası bu incelemede yeniden alınamadı; mevcut atıf tek başına güncel sözleşme doğrulaması sayılmadı. [İzmir lisansı](https://acikveri.bizizmir.com/tr/license).

OSM verisinde atıf ve ODbL yükümlülükleri, türetilmiş/karma veri tabanının yapısına göre yerine getirilmelidir; her proje dosyasına yalnız Apache yazmak yeterli değildir. Diğer kaynakların ODbL ile uyumu ve kaynak veri teklifi ayrıca incelenmelidir. [ODbL 1.0](https://opendatacommons.org/licenses/odbl/1-0/).

**Gerekli çözüm:** veri kümesi bazında kaynak/sürüm/lisans/dağıtım kapsamı kaydı; uygun olmayan veriyi paketten çıkarma veya izin alma. Açık lisans koşulları sağlanıyorsa sırf yazılı izin yok diye kullanım otomatik hukuka aykırı sayılmaz.

## 3. Konum ve sorgular için KVKK değerlendirmesi eksik

Android konum izni ve tarayıcı izin penceresi, KVKK aydınlatmasının veya gerekli hukuki sebebin yerine geçmez. KVKK m.4–5, 10–12 kapsamında amaç, asgarilik, hukuki sebep, veri sorumlusu kimliği, alıcılar, saklama ve hak kullanım kanalı somutlaştırılmalıdır. Gerçek kişiyle ilişkilendirilebilen konum/IP/sorgu kayıtları bakımından yükümlülük doğabilir. Araç GPS'i veya plakasının kişisel veri olup olmadığı gerçek kişiyle ilişkilendirilebilirliğe göre değerlendirilir. [Kanun metni](https://www.aile.gov.tr/kvkk/mevzuat), [KVKK aydınlatma rehberi](https://www.kvkk.gov.tr/SharedFolderServer/CMSFiles/a569a068-c079-4189-b134-f57bc727af7d.pdf).

Görülen akışlar: favoriler cihaz/tarayıcıda saklanır; web canlı sorguları Transit TR sunucusundan geçer; yakın durak işlemi cihaz konumunu kullanır; OSM karo istekleri IP ve görüntülenen bölge bilgisini alıcıya gösterebilir. Web sunucusunun sınırlı sorgu belleği vardır. “Veri hiç toplanmıyor” gibi genel bir ifade doğru değildir. Üçüncü taraf servis kaydının açık görünmesi de ilgili kişinin veriyi her amaçla kullanıma açtığı anlamına gelmez. [KVKK alenileştirme duyurusu](https://www.kvkk.gov.tr/Icerik/6843/-ALENILESTIRME-HAKKINDA-KAMUOYU-DUYURUSU).

Yurt dışı alıcı/altyapı varsa güncel KVKK m.9 koşulları ayrıca belirlenmelidir; Türkiye'de sunucu kullanmak veya genel bir rıza kutusu koymak tek başına yeterli çözüm değildir. VERBİS yükümlülüğü ve istisnaları işletmecinin somut durumuna göre değerlendirilir. [KVKK güncel aktarım açıklaması](https://www.kvkk.gov.tr/Icerik/2053/Yurtdisina-Aktarim).

**Gerekli çözüm:** yayıncı/veri sorumlusu ve iletişim kanalı belirtilerek gerçek akışa uygun metnin uygulama içinde sunulması; gereksiz alanların/logların kaldırılması, saklama/silme düzeni ve gerekiyorsa aktarım dayanağı. Bu bilgiler kullanıcıdan henüz alınmadığı için sahte bir kimlikle tamamlanmış aydınlatma metni üretilmedi.

## 4. TLS doğrulaması kapalı: somut teknik yayın engeli

`ApiHttp.java` ve `MapHttp.java`, `EgoLocal.disableCertificateChecks()` çağırıyor. `EgoLocal.java` genel sertifika/alan adı kontrollerini devre dışı bırakıyor; eski Android için `LegacyTls.java` özel doğrulama yolu içeriyor. `web-service/transport.mjs` içinde `rejectUnauthorized:false` var. Aradaki saldırganın yanıtları veya taşıma sırasında kimlik bilgilerini ele geçirmesi/değiştirmesi riskini artırır. Bu durum tek başına TCK suçu işlendiği anlamına gelmez; kişisel veri işleniyorsa KVKK m.12 tedbir değerlendirmesinde önemlidir. [KVKK veri güvenliği yükümlülükleri](https://www.aile.gov.tr/kvkk/mevzuat).

**Gerekli çözüm:** güvenilir sertifika zinciri ve sunucu adı kontrolünü etkinleştirmek; başarısız sertifikada doğrulamayı kapatarak tekrar denememek; API 8 için doğrulayan TLS uygulamasını ve güncel güven köklerini test etmek veya güvenli bağlantı sağlanamayan özelliği kapatmak. Bu yan çalışmada mevcut ağ davranışı değiştirilmedi ve yeni APK üretilmedi. Önceki TLS kapatma tercihi kamuya dağıtım uygunluğu için onay sayılmadı; yayın kontrolü bu durumu engelliyor.

## 5. Marka, iddialar ve OSM servis koşulları

Resmî belediye uygulaması izlenimi verilmemeli; kurum adları yalnız kaynağı açıklamak için kullanılmalı, logolar/markalar için ayrıca hak değerlendirmesi yapılmalıdır. ABD hukukuna ait “nominative fair use” ifadesi Türk hukukunda otomatik koruma gibi sunulamaz. SMK m.7 ve TTK m.54–55 somut kullanım bakımından önemlidir. [SMK resmî metni](https://cdn.tbmm.gov.tr/KKBSPublicFile/D26/Y1/T1/KanunMetni/bb586e8c-efca-4ed5-ac77-47f34dc5d8ee.html), [TTK resmî metni](https://resmigazete.gov.tr/eskiler/2011/02/20110214-1-1.htm).

“GPS”, dakika, test kapsamı ve metro tahminleri kaynağın sağladığından fazla kesinlik iddia etmemeli. Mevcut metroda tahmin/GPS ayrımı olumlu; README sürüm/test kapsamı ile manifest arasında fark vardı, dolayısıyla tüm sürümler için tek doğrulama beyanı kullanılamaz. Satış/ücretli hizmet varsa ayrıca tüketici/sözleşme yükümlülükleri incelenmelidir.

OSM veri lisansı, ücretsiz karo sunucusuna sınırsız erişim hakkı vermez. Görünür atıf, doğru istemci kimliği, uygun önbellek ve toplu indirmeden kaçınma gerekir. Mevcut 1 MiB küresel önbelleğin sık tahliye ile aynı karoları yeniden indirmesi ihtimali yalnız “7 gün” açıklamasıyla çözülmüş sayılmaz; ayrı doğrulama gerekir. [OSMF karo kullanım koşulları](https://operations.osmfoundation.org/policies/tiles/).

## Bu incelemede yapılan düzeltmeler ve doğrulama

- Türk hukukunda otomatik marka kullanım izni varmış izlenimi veren ifade kaldırıldı; kod ve veri hakları ayrıldı.
- Kamuya yayın durumunun onaylanmadığı README'ye eklendi. Mevcut APK/ZIP'lar silinmedi veya uyumlu diye yeniden etiketlenmedi.
- `legal/public-release.json` ile kanıt, yayıncı, gizlilik ve lisans inceleme kaydı eklendi; mevcut kayıtlar bilerek `pending` bırakıldı.
- Ağ çağrısı yapmayan `tools/legal_release_check.py` eklendi. `TRANSIT_DISTRIBUTION=public` Android derlemesi kontrol geçmeden araç çalıştırmaz. Bu seçenek kullanılmayan geliştirme derlemeleri veya elle dağıtım engellenmez.
- Dört bağımsız fixture testi: eksik hak/kimlik kaydı, kaynak değişikliğinde incelemenin geçersizleşmesi, sır yazdırmadan TLS engeli ve yalnız lisans URL'sinin onay sayılmaması. Uygulama, emulator veya mevcut proje testleri çalıştırılmadı.

Bu düzeltmeler mevcut API erişimlerini izinli hâle getirmedi ve tüm hukuki riskleri kapatmadı. İzinleri edinmek veya sorunlu entegrasyonları/verileri yayından çıkarmak; gerçek yayıncı bilgileri ve teknik TLS düzeltmesini tamamlamak gerekiyor. Açık dağıtım öncesi özellikle bu somut kayıtların Türkiye'de bilişim/fikrî mülkiyet alanında çalışan bir avukat tarafından değerlendirilmesi uygundur.
