# Ankara EGO TAKSİS adaptörü

Kullanıcının verdiği resmî Swagger: https://taksisapi.ego.gov.tr/swagger/index.html
Tanım: https://taksisapi.ego.gov.tr/swagger/v1/swagger.json

04.11.2026 tarihinde tanım indirildi. OpenID Connect güvenlik şeması var. Durak, istasyon ve araç GET uçlarına kimlik bilgisi olmadan yapılan sorguların üçü de HTTP 401 döndürdü. Araç DTO'su GPS koordinatı, varış süresi veya kart bakiyesi alanı sağlamıyor; servis iş takibi/envanter alanları içeriyor. Swagger'ın açık olması verilerin anonim erişime açık olduğu anlamına gelmiyor.

## Eklenen entegrasyon

Sunucu yalnızca GET /api/app/istasyon ve GET /api/app/durak okuma uçlarını kullanır:

- GET /api/v1/ankara/stops?type=station&q=Kızılay&page=0
- GET /api/v1/ankara/stops?type=stop&q=12345&page=0

İstasyon araması Ad, durak araması No parametresine eşlenir. Sayfalar en çok 50 kayıttır. Arama uzunluğu ve sayfa sınırları doğrulanır. Çıktıya yalnızca kimlik, ad, kod, adres ve kaynak konum metni alınır. Personel, oluşturan kişi ve denetim alanları istemciye aktarılmaz. Konum metninin biçimi doğrulanmadığı için bundan harita koordinatı üretilmez.

EGO tarafından verilmiş yetkili OAuth erişimi mevcutsa, belirteci yalnızca kendi sunucunun TAKSIS_BEARER_TOKEN ortam değişkeninde yapılandır. APK'ya, web kaynaklarına veya ZIP'e belirteç koyma. Kullanıcı adı/şifreyle otomatik giriş, yetki alma veya token yenileme akışı uygulanmadı. Belirteç süresi biterse sunucu erişim gerekli durumuna döner.

Web: Servisler → Ankara EGO TAKSİS. Android: Resmî → Ankara EGO TAKSİS; kendi HTTPS Transit TR sunucu adresini ve istasyon adını gir.

Yetkili belirteç sağlanmadığından gerçek Ankara kayıtları alınamadı. Yetkisiz yanıt, alan eşleme ve güvenli çıktı testleri yerel sahte servisle geçti; gerçek yetkili veri sorgusu doğrulanmadı. Bu adaptör canlı otobüs takibi/ETA/kart bakiyesi entegrasyonu değildir.
