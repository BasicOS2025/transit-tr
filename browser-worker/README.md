# EGO arka plan tarayıcısı

Bu servis EGO'nun herkese açık Otobüs Nerede ekranını gerçek Chromium tarayıcısında açar, durak formuna numarayı yazar, düğmeye basar ve yalnızca `.bus-card` sonuçlarındaki hat/yön/tahmini süreyi okur. TAKSİS OAuth API'sini kullanmaz. Dinamik form adları veya form güvenlik alanları elle üretilmez; normal tarayıcı gönderimi kullanılır.

## Çalıştırma

Node 22+ ve Chromium gerekir. Projenin kökünden:

```sh
npm ci
npx playwright install chromium
npm run ego:browser
```

Varsayılan dinleme adresi `127.0.0.1:8091`'dir. Go sunucusunu ikinci terminalde başlat:

```sh
cd backend
EGO_BROWSER_URL=http://127.0.0.1:8091 go run .
```

Web'de Servisler → Ankara Otobüs nerede → 5 haneli durak numarası → Kontrol et. Android'de Resmî → Ankara canlı durak; kendi HTTPS Transit TR sunucu adresini ve durak numarasını gir. APK'ya çalışan ortak bir sunucu adresi gömülmedi; bu paket kendi başına barındırılan bir hizmet değildir.

Tarayıcı telefonda değil sunucuda çalışır. Telefon küçük JSON yanıtını gösterir. Sunucuda Chromium'un ek RAM/CPU tüketimi vardır. Mevcut Chrome kullanılacaksa CHROME_PATH, kurum ağı proxy gerektiriyorsa BROWSER_PROXY_URL yapılandırılabilir. TLS sertifika doğrulaması açıktır; ignoreHTTPSErrors veya sertifika denetimini kapatan seçenekler kullanılmaz. Bu çalışma ortamında proxy sertifikası nedeniyle yerel Chromium gerçek EGO sayfasına bağlanamadı; kullanıcının kendi sunucusunda güven zinciri doğru olmalıdır.

## İşleyiş ve sınırlar

- `GET /api/v1/ankara/arrivals?stop=10001` Go geçidi üzerinden worker'a gider.
- Aynı anda tek tarayıcı sorgusu; dış sorgular arasında en az 5 saniye; aynı durak için 30 saniyelik bellek önbelleği, en çok 100 durak.
- Önbellek yanıtı özgün `checkedAt` zamanını korur ve `cache:true` döner; eski tahmin güncelmiş gibi etiketlenmez.
- Her sorguda yeni tarayıcı bağlamı açılır; sayfa işinden sonra kapanır. Tarayıcı süreci yeniden kullanılır. Bağlam için toplam süre sınırı 25 saniyedir.
- Yalnızca hat kodu, yön adı, tahmini süre, kaynak zamanı ve kontrol zamanı aktarılır. Sayfanın IP adresi ve plaka alanları aktarılmaz.
- Kaynak işaretlemesi değişirse, CAPTCHA/giriş istenirse veya veri okunamazsa hata gösterilir. CAPTCHA çözme, kimlik doğrulama aşma veya sayfanın güvenlik denetimini etkisizleştirme yoktur.
- Bu tahminler araç GPS koordinatı değildir; garantili varış saati veya kart bakiyesi sağlamaz.
- Otomatik aralıksız tarama yapılmaz. Sorgu kullanıcı Kontrol et düğmesine bastığında başlar. Kamuya açık bir kurulumda kullanıcı başına trafik sınırı ayrıca uygulanmalıdır.

## Test

```sh
node --test tests/ego-browser.test.mjs
```

Testte yerel bir sayfa gerçek tarayıcıyla açılır; değişken adlı form alanı, normal form güvenlik alanı, gönderim, tahmini süre ayrıştırması ve IP/plaka alanlarının dışarı aktarılmaması doğrulanır. Bu fixture testi gerçek EGO ağı testi değildir. EGO'nun bulut tarayıcısında 10001 numaralı durak için gerçek sonuç döndürdüğü ayrıca doğrulandı; yerel worker → EGO ağı ise bu ortamda sertifika/ağ engeli nedeniyle doğrulanamadı.
