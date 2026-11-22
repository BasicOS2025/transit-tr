import UIKit
import UserNotifications

enum TransitReminders {
    static let prefix = "transittr.departure."
    static let center = UNUserNotificationCenter.current()

    struct TooLate: LocalizedError {
        var errorDescription: String? { "Seçilen hatırlatma saati geçti." }
    }

    static func nextWeeklyDeparture(_ time: String, dayType: Int, now: Date = Date()) -> Date? {
        guard time.range(of: #"^([01][0-9]|2[0-3]):[0-5][0-9]$"#, options: .regularExpression) != nil,
              let hour = Int(time.prefix(2)), let minute = Int(time.suffix(2)) else { return nil }
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = Catalog.timeZone
        for day in 0...7 {
            guard let date = calendar.date(byAdding: .day, value: day, to: now),
                  let departure = calendar.date(bySettingHour: hour, minute: minute, second: 0, of: date)
            else { continue }
            let weekday = calendar.component(.weekday, from: date)
            let index = weekday == 7 ? 1 : weekday == 1 ? 2 : 0
            if dayType == index && departure.timeIntervalSince(now) >= 121 { return departure }
        }
        return nil
    }

    static func schedule(_ departure: Departure, stop: Entry, minutes: Int,
                         completion: @escaping (Error?) -> Void) {
        let date = departure.instant.addingTimeInterval(-Double(minutes * 60))
        guard date.timeIntervalSinceNow >= 1 else { completion(TooLate()); return }

        center.requestAuthorization(options: [.alert, .sound]) { allowed, error in
            guard allowed else {
                DispatchQueue.main.async {
                    completion(error ?? TransitError.message("Ayarlar’dan Transit TR bildirimlerine izin ver."))
                }
                return
            }
            let interval = date.timeIntervalSinceNow
            guard interval >= 1 else {
                DispatchQueue.main.async { completion(TooLate()) }
                return
            }
            let content = UNMutableNotificationContent()
            content.title = departure.line + " · Planlı kalkış"
            content.body = stop.name + " · " + TransitScreen.date(departure.instant, includeDay: false) +
                " TSİ. Kaynak tarife saati."
            content.sound = .default
            let id = prefix + "\(stop.city).\(stop.kind.rawValue).\(stop.id ?? 0).\(departure.line)." +
                String(Int64(departure.instant.timeIntervalSince1970))
            let trigger = UNTimeIntervalNotificationTrigger(timeInterval: interval, repeats: false)
            center.add(UNNotificationRequest(identifier: id, content: content, trigger: trigger)) { error in
                DispatchQueue.main.async { completion(error) }
            }
        }
    }
}
