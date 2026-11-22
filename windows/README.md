# Transit TR for Windows

Windows 7 SP1 ve daha yeni masaüstü Windows için yerel WinForms uygulaması. Paket x86 ve x64 Windows’ta çalışmak üzere AnyCPU olarak derlenir. Microsoft .NET Framework 4.8 veya uyumlu yeni 4.x sürümü gerekir. Windows 7 SP1 desteği hedeflenir. Windows 7 cihazında ayrıca doğrulama gereklidir.

## Açma

`setup.exe` dosyasını aç. Varsayılan kurulum, yönetici yetkisi istemeden kullanıcı hesabının programlar klasörüne kurulur ve Başlat menüsüne Transit TR kısayolunu ekler. Taşınabilir ZIP’i kullanırsan tamamını ayıkla ve `TransitTR.exe` ile aç. Windows 7’de .NET Framework 4.8’i önceden yükle.

- 41 şehir veya işletmenin paketlenmiş hat ve durakları, Türkçe arama ve sıralı duraklar çevrimdışı çalışır.
- Ankara hat ayrıntılarında arşivlenmiş EGO haftalık tarifeleri gösterilir.
- Canlı durak sorguları, Ankara’da EGO Cep’te dahil, Transit TR HTTPS API’sine gider. Windows 7’de canlı erişim TLS 1.2, işletim sisteminin sertifika deposu ve sunucunun desteklediği şifreleme seçeneklerine bağlıdır. Katalog çevrimdışı açılabilir.
- Harita sekmesi OpenStreetMap karolarını, seçili durağı, kaynak araç GPS kayıtlarını ve paketlenmiş güzergâhı çiziyor. Güzergâh geometrisi bulunmayan hatlarda durakların şematik bağlantısı etiketlenir. Harita karoları bağlantı gerektirir. Daha önce yüklenen karolar süreleri dolana kadar kullanılabilir.
- **5 dk kala bildir** seçeneği açıkken seçili durak ve varsa hat 30 saniyede bir, İstanbul’da 60 saniyede bir sorgulanır. Kaynak geçerli bir varış süresi verirse Windows bildirim alanında bir uyarı gösterilir. Bildirim için Transit TR açık kalmalı. Yeni bildirim almak için durak veya hat değiştir.
- Muğla ve Eskişehir mevcut pakette yerel hat veya durak verisi içermiyor.

## Derleme

.NET Framework 4.8 geliştirici paketi, .NET SDK ve Python 3 kurulu bir Windows bilgisayarda, depo kökünden:

```powershell
python windows/build_catalog.py windows/data
dotnet build windows/TransitTR.Windows.csproj -c Release
```

Çıktı `windows/bin/Release/net48` dizinindedir. GitHub Actions paketi oluşturmadan önce çalıştırılabilir dosyanın bütün şehir kataloglarını okuyabildiğini kontrol eder.

Kurulum dosyası NSIS 3 ile `windows/setup.nsi` üzerinden üretilir. GitHub Actions sessiz kurulum, katalog açılışı ve kaldırmayı Windows çalıştırıcısında doğrular.
