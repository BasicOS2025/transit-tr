# Kaynaklar — 04.11.2026

- İBB / İETT: https://data.ibb.gov.tr/dataset/iett-gtfs-verisi — routes.csv ve stops.csv; kaynak adresleri data/raw/iett-metadata.json içinde. Lisans: https://data.ibb.gov.tr/license
- İBB / Metro İstanbul: istasyon ve hat API'leri İBB Açık Veri Portalı'nın veri setlerinde açıkça listelenmiştir. Katalog kaydı data/raw/metro-meta.json içinde korunmuştur. https://api.ibb.gov.tr/MetroIstanbul/api/MetroMobile/V2/GetStations ve GetLines. Lisans: https://data.ibb.gov.tr/license
- Duyurular: https://api.ibb.gov.tr/MetroIstanbul/api/MetroMobile/V2/GetServiceStatuses — canlı araç konumu değildir; servis yanıtındaki güncelleme tarihleri aynen gösterilir.
- İzmir Büyükşehir Belediyesi: https://acikveri.bizizmir.com/tr/dataset/toplu-ulasim-gtfs-verileri
- Metro: https://www.izmirmetro.com.tr/gtfs/rail-metro-gtfs.zip — takvim bitişi 20270709.
- Tramvay: https://www.tramizmir.com/gtfs/rail-tramizmir-gtfs.zip — takvim bitişi 20270716.
- İZBAN: https://www.izban.com.tr/gtfs/rail-izban-gtfs.zip — takvim bitişi 20250101; eski katalog olduğu arayüzde belirtilir, güncel sefer gibi kullanılmaz.
- İzmir lisansı: https://acikveri.bizizmir.com/tr/license — CC BY 4.0 https://creativecommons.org/licenses/by/4.0/
- OpenStreetMap harita bağlantıları: © OpenStreetMap contributors, ODbL https://www.openstreetmap.org/copyright

Değişiklikler: kaynaklar kompakt SQLite/JSON'a dönüştürüldü, gerekli alanlar seçildi, İETT metinlerindeki mojibake ve binlik ayıraca dönüşmüş koordinatlar onarıldı. GTFS seferleri takvim ve istisnalara göre planlı kalkış sorgusuna dönüştürüldü. Statik katalog güncellik garantisi taşımaz. Kaynak dosyaların SHA256 değerleri data/manifest.json içinde.

- Konya Büyükşehir Belediyesi: https://acikveri.konya.bel.tr/dataset/toplu-tasima-gtfs-verileri — resmî portalın yönlendirdiği https://acikveri-konya.digitalcosmonot.com alanından GTFS 11/2025 indirildi. Kaynak takvim beyanı 20301231; kaynak dosyanın yeni indirilmesi tarifenin güncel olduğunu kanıtlamaz. CC BY 4.0. Bu pakette yalnızca katalog ve her hat için ilk seferin temsilî durak dizisi kullanıldı; otobüs kalkış saatleri aktarılmadı.
