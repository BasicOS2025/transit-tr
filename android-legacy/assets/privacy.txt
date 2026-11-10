# Transit TR — gizlilik ve kişisel veri aydınlatması

Son güncelleme: 10 Kasım 2026. Yayıncı ve kendi işlemleri bakımından veri sorumlusu: Ata Köse. Başvuru/iletişim: atakose2012@gmail.com. Belediyeler ve diğer servis sağlayıcıları kendi işlemleri bakımından ayrıca veri sorumlusu olabilir.

Bu metin bilgilendirme içindir; bir açık rıza beyanı veya sağlayıcı kullanım izni değildir.

## Hangi bilgiler, hangi amaçla kullanılır?

- Seçtiğiniz şehir, durak, hat ve yön, ilgili varış/rota verisini getirmek için kullanılır. Favoriler ve tema tercihi Android'de uygulama alanında, web'de bu tarayıcının yerel depolamasında saklanır.
- “Yakınımdaki duraklar” özelliğini kullanırsanız cihazın konum izni istenir. Konum yakın durakları hesaplamak için kullanılır; kaynağa göre sorgu koordinatı sunucuya veya ulaşım sağlayıcısına iletilebilir. Arka planda sürekli kişisel konum takibi amaçlanmaz. Konum iznini vermeden durak numarasıyla sorgu yapabilirsiniz.
- Android sürümü ulaşım sağlayıcılarına doğrudan bağlanır. Web sürümünde sorgular Transit TR web sunucusundan geçer. Ağ alıcıları IP adresi, istemci türü, sorgu ve bağlantı zamanı gibi teknik bilgileri görebilir. Yakın durak web sorgusu koordinat içerir.
- Harita karoları OpenStreetMap sunucusundan alınır. Harita sağlayıcısı IP adresini ve istenen karo/bölge bilgisini görebilir; harita kişisel konumunuz etrafında açıldıysa bu bilgi yaklaşık bulunduğunuz bölgeyi gösterebilir.
- Ulaşım araçlarının kaynak GPS'i kişisel cihazınızın GPS'i değildir. Uygulama araç konumunu, hat bilgisini, kaynak zamanını ve varsa durak varış tahminini gösterir. Kaynakta bulunan sürücü bilgileri gibi gereksiz kişisel alanlar uygulamanın amaçları için gerekli değildir ve toplanmamalıdır.
- Bu kaynak sürümünde kullanıcı hesabı, kart bakiyesi veya ödeme özelliği yoktur. Kart numarası, banka parolası veya SMS doğrulama kodu göndermeyin. Kaynakta ayrıca reklam/analitik SDK'sı eklenmemiştir; bağımsız forklar bu davranışı değiştirebilir.

## Hukuki sebep, alıcılar ve aktarım

Kullanıcının istediği sorgunun sağlanması için gerekli işlemler, somut hizmet ilişkisine göre KVKK m.5/2-c; sınırlı güvenlik/işletim işlemleri, hak ve menfaat dengesi sağlanarak m.5/2-f kapsamında değerlendirilir. Başvuru ve yasal yükümlülükler için ilgili diğer kanuni şartlar uygulanabilir. Zorunlu olmayan bir işlem için açık rıza gerekiyorsa ayrıca ve özgür iradeyle alınmalıdır; cihazın izin penceresi bunun yerine geçmez.

Alıcılar seçilen ulaşım işletmesi/servis sağlayıcısı, web sürümünde site işletmecisinin barındırma sağlayıcısı, harita sağlayıcısı ve yasal yükümlülük hâlinde yetkili mercilerdir. Bağımsız bir web kurulumunu veya forku başka kişi işletiyorsa kendi kimliği, alıcıları ve gerçek veri işleme şartlarıyla ayrı aydınlatma yapmalıdır; bu metinde Ata Köse'nin adını kullanarak kendi işlemlerini Ata Köse'ye yükleyemez.

OSM ve barındırma gibi hizmetlerde alıcı/altyapı yurt dışında olabilir. Kamuya sunulacak gerçek barındırma, alt işleyen ve aktarım düzeni henüz belirlenmediğinden KVKK m.9 kapsamındaki somut aktarım dayanağı bu geliştirme sürümü için doğrulanmış değildir. Bu metin tek başına yurt dışı aktarım yetkisi sağlamaz. Kamuya hizmet verilmeden önce bu düzen tamamlanmalı; gerekli sözleşme/güvence veya diğer kanuni şart sağlanmalıdır.

## Saklama ve silme

Favori ve tema kayıtları, siz kaldırana, tarayıcı/site verilerini veya Android uygulama verisini temizleyene ya da uygulamayı silene kadar cihazda kalabilir. Favori satırını kaldırarak ilgili kaydı silebilirsiniz. Harita ve rota önbelleği sınırlıdır ve işletim sistemi tarafından da temizlenebilir. Web servisinin sorgu önbelleği bellektedir; kayıtlar sorgu türünün süresi dolduktan sonra periyodik temizlikte çıkarılır ve süreç kapanınca silinir. Sunucu erişim günlükleri varsa bunların kapsamı ve saklama süresi gerçek barındırma kurulumunda ayrıca açıklanmalıdır; bu kaynak sürümüne kalıcı bir erişim günlüğü eklenmemiştir.

Sağlayıcıların kendi kayıtları yayıncının cihaz önbelleği değildir. Bu tarafların saklama/başvuru kuralları kendi aydınlatmalarında yer alır. Kişisel veri içeren destek başvuruları yalnız talebi ele almak ve hukuki yükümlülükleri yerine getirmek için gerekli süre boyunca tutulmalı, amaç ortadan kalkınca silinmelidir. Gereksiz kart, parola, konum geçmişi veya üçüncü kişi verisi göndermeyin.

## Haklarınız ve güvenlik

KVKK m.11 kapsamında işleme hakkında bilgi edinme, amaç ve alıcıları öğrenme, koşulları varsa düzeltme/silme, üçüncü kişilere bildirim, otomatik analiz sonucu aleyhinize sonuca itiraz ve hukuka aykırı işleme nedeniyle zararın giderilmesini talep haklarınız vardır. Taleplerinizi kimliğinizin güvenli biçimde doğrulanmasına elverişli bir yöntemle yukarıdaki iletişim kanalına iletebilirsiniz. Kanunun m.13 ve ilgili Tebliğ'de öngördüğü usuller, süreler ve zorunlu bilgiler saklıdır. E-posta üzerinden gereksiz kimlik belgesi paylaşmayın.

Bağlantılarda sertifika zinciri ve sunucu adı doğrulaması etkinleştirilmiştir. Doğrulanamayan sunucuya bağlantı kurulmaz; doğrulama kapatılarak yeniden deneme yapılmaz. Eski Android sürümleri ayrıca güvenlik ve sertifika uyumluluğu sınırlamalarına sahiptir. Bu açıklama hatasız veya mutlak güvenlik garantisi değildir.

Kaynaklar: KVKK m.4–5, 7, 9–13 ve [Kurum açıklamaları](https://www.kvkk.gov.tr/); [OSMF gizlilik politikası](https://osmfoundation.org/wiki/Privacy_Policy). API/veri lisansları ve kamuya yayın incelemesi ayrıca değerlendirilir.

İlk açılışta gizlilik ve sorumluluk metni birlikte gösterilir. “Okudum, devam et” yalnız bildirimin gösterildiğine ilişkin cihaz/tarayıcı kaydını saklar; açık rıza veya kanuni haklardan feragat anlamına gelmez. Uygulama/site verileri temizlenirse bildirim tekrar görünür. Ayarlar üzerinden metne daha sonra da erişilebilir.
