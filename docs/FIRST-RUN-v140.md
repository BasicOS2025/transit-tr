# İlk açılış bildirimi — Transit TR 1.4.0

10 Kasım 2026. versionName 1.4.0 ve versionCode 16 korundu. Android 2.2/API 8 ve Android 6/API 23 emülatörlerinde gerçek düğme ve yaşam döngüsü testleri geçti.

İlk açılışta gizlilik ve sorumluluk metinleri kaydırılabilir pencerede birlikte gösterilir. “Okudum, devam et” cihazda bir defalık kayıt oluşturur; “Çıkış” kayıt oluşturmaz. Sonraki açılışta bildirim atlanır. Harita/canlı ekran bildirimden sonra açılır. Widget yapılandırması da aynı kaydı kullanır. Kullanıcı favorileri test öncesi ve sonrası karşılaştırılarak korundu. Test, okundu kaydını önceki durumuna geri getirdi; ilk kullanıcı açılışı yapay olarak tamamlanmış sayılmadı.

Web'de iki metnin yüklenmesi, devam kaydı, sonraki açılışta atlama, Escape ile atlanamama, çıkış/hata durumunda kayıt yazılmaması ve depolama hatası yerel taklitli testle kontrol edildi. Bildirim dosyaları uygulama kabuğu önbelleğine eklendi.

Okundu kaydı açık rıza veya kanuni haklardan feragat değildir. Ayarlar'dan metin tekrar açılabilir. Uygulama/site verileri temizlenirse tekrar gösterilir.

Bu dosyalar yerel test APK'larıdır; bu test sağlayıcı erişim/veri dağıtım izinlerini onaylamaz. Kamuya dağıtım incelemesi ayrı ve henüz tamamlanmamıştır.
