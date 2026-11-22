import Foundation

struct WidgetSnapshot: Codable, Equatable {
    let city: Int
    let stop: String
    let name: String
    let line: String
    let arrivals: [String]
    let updated: Date

    static var group: String? { Bundle.main.object(forInfoDictionaryKey: "TransitAppGroup") as? String }
    static let url = URL(string: "transittr://widget")!

    private static var defaults: UserDefaults? {
        guard let group = group,
              FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: group) != nil else { return nil }
        return UserDefaults(suiteName: group)
    }

    static func read() -> WidgetSnapshot? {
        defaults?.data(forKey: "arrivalSnapshot").flatMap { try? JSONDecoder().decode(Self.self, from: $0) }
    }

    func save() -> Bool {
        guard let defaults = Self.defaults, let data = try? JSONEncoder().encode(self) else { return false }
        defaults.set(data, forKey: "arrivalSnapshot")
        return true
    }

    var expiry: Date { updated.addingTimeInterval(120) }

    func display(at now: Date) -> String {
        if now >= expiry || updated > now.addingTimeInterval(30) { return "Güncellemek için Transit TR’yi aç." }
        return arrivals.isEmpty ? "Kaynak yaklaşan araç göstermedi." : arrivals.joined(separator: "  /  ")
    }

    var timeLabel: String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "tr_TR")
        formatter.timeZone = TimeZone(secondsFromGMT: 10800)
        formatter.dateFormat = "HH:mm"
        return "Son sorgu " + formatter.string(from: updated) + " TSİ"
    }
}
