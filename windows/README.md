# Transit TR for Windows

Windows 7 SP1 ve daha yeni masaüstü Windows için yerel WinForms uygulaması. Paket x86 ve x64 Windows’ta çalışmak üzere AnyCPU olarak derlenir. Microsoft .NET Framework 4.8 veya uyumlu yeni 4.x sürümü gerekir. Windows 7 SP1 desteği hedeflenir. Windows 7 cihazında ayrıca doğrulama gereklidir.

## Açma

ZIP’in tamamını aynı klasöre ayıkla ve `TransitTR.exe` dosyasını aç. `data` klasörünü yanından ayırma. Hata ayıklayıcı, IDE, tarayıcı motoru veya yönetici yetkisi gerekmez. OpenStreetMap bağlantıları varsayılan tarayıcıda açılır.

- 41 şehir veya işletmenin paketlenmiş hat ve durakları, Türkçe arama ve sıralı duraklar çevrimdışı çalışır.
- Ankara hat ayrıntılarında arşivlenmiş EGO haftalık tarifeleri gösterilir.
- Canlı durak sorguları, Ankara’da EGO Cep’te dahil, Transit TR HTTPS API’sine gider. Windows 7’de canlı erişim TLS 1.2, işletim sisteminin sertifika deposu ve sunucunun desteklediği şifreleme seçeneklerine bağlıdır. Katalog çevrimdışı açılabilir.
- Muğla ve Eskişehir mevcut pakette yerel hat veya durak verisi içermiyor.

## Derleme

.NET Framework 4.8 geliştirici paketi, .NET SDK ve Python 3 kurulu bir Windows bilgisayarda, depo kökünden:

```powershell
python windows/build_catalog.py windows/data
dotnet build windows/TransitTR.Windows.csproj -c Release
```

Çıktı `windows/bin/Release/net48` dizinindedir. GitHub Actions paketi oluşturmadan önce çalıştırılabilir dosyanın bütün şehir kataloglarını okuyabildiğini kontrol eder.
