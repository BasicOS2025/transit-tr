# Şehir ve işletmeci testi · 22 Kasım 2026

Yerel Android kaynaklarından üretilen iOS SQLite kataloğunda 41 şehir/işletme seçeneği, 9.900 hat/yön ve 72.226 durak bulundu. Aşağıdaki canlı sonuçlar, iOS uygulamasının kullandığı HTTPS API’sinde örnek durak başına bir istekle ölçüldü. Araç sayısı saat ve durakla değişir. GPS sütunu, filo verisi ayrı olduğunda o listedeki geçerli koordinatları sayar. Sonuçlar iPhone üzerinde uygulama testi yerine geçmez.

İzmir’de `21050` ve `20034` için ESHOT bağlantısı zaman aşımına uğradı. Aydın `4480`, Hatay `20001`, Trabzon `2400` tekrar sorgularında kaynak yanıt verdi, ancak kayıt dönmedi.

| Şehir / işletme | Yerel hat/yön | Yerel durak | Örnek durak | Varış süresi | GPS kaydı | API sonucu |
|---|---:|---:|---|---:|---:|---|
| Ankara | 665 | 10866 | 10940 | 7 | 7 | EGO Cep’te |
| İstanbul | 5413 | 15631 | 205471 | 5 | 5 | İETT / Otobüsüm Anlık |
| İzmir | 907 | 12101 | 21050 | — | — | HTTP 502 · kaynak bağlantısı zaman aşımı |
| Kocaeli | 415 | 0 | 80163 | 1 | 1 | Kocaeli / Kentkart |
| Muğla | 0 | 0 | — | — | — | bağlı değil |
| Eskişehir | 0 | 0 | — | — | — | bağlı değil |
| Konya | 165 | 4463 | 27 | 18 | 8 | Konya ATUS Mobil |
| Aydın | 41 | 2345 | 4454 | 0 | 0 | Yanıt var, yaklaşan araç kaydı yok |
| Antalya | 168 | 0 | 12406 | 4 | 4 | Antalya / Kentkart |
| Bursa | 23 | 5 | 3368 | 12 | 5 | BursaKart / BURULAŞ |
| Adıyaman | 11 | 0 | — | — | — | test edilmedi: örnek durak yok |
| Ağrı | 12 | 0 | — | — | — | test edilmedi: örnek durak yok |
| Balıkesir | 403 | 3084 | 3074 | 1 | 1 | Balıkesir / ASİS |
| Çorum | 25 | 0 | — | — | — | test edilmedi: örnek durak yok |
| Denizli | 68 | 1477 | 901 | 1 | 2 | Denizli / ASİS |
| Diyarbakır | 104 | 1284 | 399 | 1 | 11 | Diyarbakır / ASİS |
| Elazığ | 49 | 0 | — | — | — | test edilmedi: örnek durak yok |
| Erzincan | 40 | 0 | — | — | — | test edilmedi: örnek durak yok |
| Hakkari | 12 | 0 | — | — | — | test edilmedi: örnek durak yok |
| Hatay | 197 | 1406 | 20055 | 0 | 0 | Yanıt var, yaklaşan araç kaydı yok |
| Kastamonu | 11 | 309 | 20 | 3 | 5 | Kastamonu / ASİS |
| Kütahya | 26 | 845 | 1003 | 1 | 3 | Kütahya / ASİS |
| Malatya | 119 | 4125 | 11282 | 1 | 0 | Malatya / ASİS |
| Sakarya | 115 | 2754 | 6711 | 1 | 2 | Sakarya / ASİS |
| Samsun | 119 | 1828 | 5610 | 3 | 5 | Samsun / ASİS |
| Trabzon | 155 | 5625 | 2437 | 0 | 0 | Yanıt var, yaklaşan araç kaydı yok |
| Şanlıurfa | 103 | 0 | — | — | — | test edilmedi: örnek durak yok |
| Van | 224 | 2657 | 1584 | 1 | 1 | Van / ASİS |
| Kırıkkale | 14 | 0 | — | — | — | test edilmedi: örnek durak yok |
| Batman | 32 | 0 | — | — | — | test edilmedi: örnek durak yok |
| Şırnak | 1 | 0 | — | — | — | test edilmedi: örnek durak yok |
| Yalova | 21 | 0 | — | — | — | test edilmedi: örnek durak yok |
| Karabük | 45 | 0 | — | — | — | test edilmedi: örnek durak yok |
| Zile | 4 | 0 | — | — | — | test edilmedi: örnek durak yok |
| Tekirdağ | 174 | 1421 | 3309 | 20 | 29 | Tekirdağ / ASİS |
| Ankara - Teiaş | 1 | 0 | — | — | — | test edilmedi: örnek durak yok |
| Yalova - Subaşı | 2 | 0 | — | — | — | test edilmedi: örnek durak yok |
| Yalova - Tavşanlı | 3 | 0 | — | — | — | test edilmedi: örnek durak yok |
| Muş - S.S.51 Koop. | 1 | 0 | — | — | — | test edilmedi: örnek durak yok |
| Yalova - Taşköprü | 2 | 0 | — | — | — | test edilmedi: örnek durak yok |
| Bayburt Belediyesi | 10 | 0 | — | — | — | test edilmedi: örnek durak yok |
