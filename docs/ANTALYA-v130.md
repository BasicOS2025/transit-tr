# Antalya — 1.3.0 test derlemesi

Antalyakart resmî herkese açık web uygulamasının ulaşım protokolü kullanılır. Canlı araçlar `/rl1//web/nearest/bus`, rota geometrisi ve sıralı duraklar `/rl1//web/pathInfo` üzerinden alınır. Bölge kodu 026. İstek imzası cihazda HMAC-SHA256 ile üretilir; kullanıcı hesabı gerekmez. İmza ve zaman damgası her istekte yenilenir.

Dakika, seçilen durak için servisin `timeDiff` alanından; konum, gerçek `lat`/`lng` alanlarından gelir. GPS zamanı kaynakta belirtilmediğinde veri alınma saati gösterilir. Durak 12406 ve VL13 güzergâhı, Android 6 ve Android 2.2 üzerinde AntalyaCheck canlı testi ile denetlenir. Servis hatasında tahmini araç veya dakika üretilmez.

Bu test APK’sı diğer şehirlerde devam eden 1.3.0 çalışmalarını da içerir; Antalya testinin geçmesi diğer yeni şehirlerin tamamının doğrulandığı anlamına gelmez.
