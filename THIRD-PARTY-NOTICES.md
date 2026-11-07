EGO mobile API integration adapted from byigitt/egomac
https://github.com/byigitt/egomac

MIT License

Copyright (c) 2026 Barış Cem Bayburtlu (@byigitt)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

---

This project is an unofficial third-party tool. It is not affiliated with,
endorsed by, or connected to Ankara Büyükşehir Belediyesi or EGO Genel
Müdürlüğü. The "EGO" name and the EGO Cep'te logo are trademarks of their
respective owners and are used here only for descriptive purposes (nominative
fair use) to identify the public transit data the tool consumes.

Bus stop coordinate / name data ships from OpenStreetMap (© OpenStreetMap
contributors), licensed under the Open Database License (ODbL).


TLS compatibility: Bouncy Castle 1.86 (bcprov/bctls/bcutil jdk15to18), relocated to org.transittr.bc. Full license: android-legacy/libs/BOUNCY-CASTLE-LICENSE.txt.
Ankara stop-name snapshot: OpenStreetMap contributors, ODbL, distributed by egomac under its attribution. Coordinates are not present in that snapshot.

Google Material Icons: map icon, Copyright Google, Apache License 2.0 (see LICENSE). Source: https://github.com/google/material-design-icons/blob/master/src/maps/map/materialicons/24px.svg . The SVG path is rendered with Android Canvas for API 8 compatibility.

Rail station-network snapshot: Ankara M1–M4/Ankaray and Istanbul M11 use OpenStreetMap-derived factual records published by kacdurak.com, © OpenStreetMap contributors, ODbL 1.0 (https://opendatacommons.org/licenses/odbl/1-0/). Metadata and source data accompany the app; see data/raw/rail-networks-v1.json and docs/SOURCES-v1.md. Other Istanbul rail records retain the IBB Open Data License referenced in docs/SOURCES-v05.md. Code licensing does not relicense database rights.


## Otobüsüm Anlık API integration

Native adaptation of public API contracts and direction/vehicle matching from the user-supplied otobusum-anlik-main.zip. No React Native runtime, copied map styles, bundled fonts, OAuth keys or embedded service passwords are used. The following license applies to adapted code, separately from service data rights:

MIT License

Copyright (c) 2025 metkm

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.


## 1.1.0 durak ve rota verileri
Ankara koordinatları: OpenStreetMap contributors, ODbL; 7 Kasım 2026 Ankara il sınırı içindeki `highway=bus_stop` + `ref` sorgusu. Ham yanıt `data/raw/ankara-osm-20261107.json` içindedir. EGO saatleri, adlar ve sıralar ego.gov.tr sitesindeki yayımlanmış hizmet bilgileridir; ayrı bir açık veri lisansı belirtilmemiştir.
İzmir ESHOT: `eshot-otobus-duraklari.csv`, `eshot-otobus-hat-listesi.csv`, `otobus-hat-guzergahlari.csv`; 7 Kasım 2026 indirmesi. Kaynak: https://acikveri.bizizmir.com/tr/dataset/otobus-hat-guzergahlari . İzmir Büyükşehir Belediyesi Açık Veri Lisansı: https://acikveri.bizizmir.com/tr/license . Veriler uygulamaya dönüştürülmüş, koordinat hassasiyeti artırılmamıştır. Lisans kodun Apache-2.0 lisansından ayrıdır.
