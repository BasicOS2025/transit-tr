import Foundation
#if canImport(FoundationNetworking)
import FoundationNetworking
#endif
import SQLite3

struct City {
    let id: Int
    let name: String
    let provider: String

    var hasOnlineStopSearch: Bool { [3, 8, 9].contains(id) }
    var hasLiveService: Bool { !provider.isEmpty && provider != "static" }
}

enum Kind: String, Codable {
    case routes, stops
}

struct Point: Decodable {
    let lat: Double
    let lon: Double

    var valid: Bool {
        lat.isFinite && lon.isFinite && abs(lat) < 85 && abs(lon) < 180 && lat != 0 && lon != 0
    }

    func distance(to other: Point) -> Double {
        let radians = Double.pi / 180
        let a = sin((lat - other.lat) * radians / 2)
        let b = sin((lon - other.lon) * radians / 2)
        let h = a * a + cos(lat * radians) * cos(other.lat * radians) * b * b
        return 12_742_000 * asin(sqrt(min(1, max(0, h))))
    }
}

struct Entry: Codable, Equatable {
    let id: Int64?
    let city: Int
    let kind: Kind
    let code: String
    let name: String
    let type: String?
    let lat: Double?
    let lon: Double?

    var point: Point? {
        guard let lat = lat, let lon = lon else { return nil }
        let point = Point(lat: lat, lon: lon)
        return point.valid ? point : nil
    }

    var title: String { code.isEmpty ? name : "\(code) · \(name)" }

    func matches(_ other: Entry) -> Bool {
        guard city == other.city && kind == other.kind else { return false }
        if let id = id, let otherID = other.id { return id == otherID }
        return code == other.code && name == other.name
    }
}

struct CatalogSource {
    let name: String
    let url: URL?
    let license: String
}

struct EGOSchedule: Decodable {
    let days: [[String]]
    let synced_at: Double?
    let source: String?
    let origin: String?
    let destination: String?
}

struct Departure {
    let line: String
    let name: String
    let instant: Date
}

struct RouteDetails {
    let stops: [Entry]
    let parts: [[Point]]
    let schematic: Bool
    let schedule: EGOSchedule?
}

struct RailNetwork: Decodable {
    let lines: [RailLine]
}

struct RailLine: Decodable {
    struct Station: Decodable {
        let name: String
        let lat: Double
        let lon: Double
        var point: Point { Point(lat: lat, lon: lon) }
    }

    let city: Int
    let id: String
    let name: String
    let stations: [Station]
    let color: String?
}

enum TransitError: LocalizedError {
    case catalog
    case response
    case message(String)

    var errorDescription: String? {
        switch self {
        case .catalog: return "Çevrimdışı katalog açılamadı."
        case .response: return "Transit TR sunucusu geçerli bir yanıt vermedi."
        case .message(let value): return value
        }
    }
}

private enum SQLValue {
    case int(Int64), text(String), double(Double)
}

final class Catalog {
    private var database: OpaquePointer?
    private let queue = DispatchQueue(label: "org.transittr.catalog", qos: .userInitiated)
    private static let transient = unsafeBitCast(-1, to: sqlite3_destructor_type.self)
    static let timeZone = TimeZone(identifier: "Europe/Istanbul") ?? TimeZone(secondsFromGMT: 10800)!

    init(path: String?) {
        guard let path = path else { return }
        if sqlite3_open_v2(path, &database, SQLITE_OPEN_READONLY | SQLITE_OPEN_NOMUTEX, nil) != SQLITE_OK {
            sqlite3_close(database)
            database = nil
        }
    }

    deinit { sqlite3_close(database) }

    func read<T>(_ work: @escaping (Catalog) throws -> T,
                 completion: @escaping (Result<T, Error>) -> Void) {
        queue.async {
            let result = Result { try work(self) }
            DispatchQueue.main.async { completion(result) }
        }
    }

    private func rows<T>(_ sql: String, _ values: [SQLValue] = [],
                         _ row: (OpaquePointer) -> T) throws -> [T] {
        guard let db = database else { throw TransitError.catalog }
        var statement: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &statement, nil) == SQLITE_OK,
              let stmt = statement else { throw TransitError.catalog }
        defer { sqlite3_finalize(stmt) }
        for (offset, value) in values.enumerated() {
            let index = Int32(offset + 1)
            let status: Int32
            switch value {
            case .int(let value): status = sqlite3_bind_int64(stmt, index, value)
            case .text(let value):
                status = sqlite3_bind_text(stmt, index, value, -1, Self.transient)
            case .double(let value): status = sqlite3_bind_double(stmt, index, value)
            }
            guard status == SQLITE_OK else { throw TransitError.catalog }
        }
        var result: [T] = []
        var status = sqlite3_step(stmt)
        while status == SQLITE_ROW {
            result.append(row(stmt))
            status = sqlite3_step(stmt)
        }
        guard status == SQLITE_DONE else { throw TransitError.catalog }
        return result
    }

    private func text(_ stmt: OpaquePointer, _ column: Int32) -> String {
        sqlite3_column_text(stmt, column).map { String(cString: $0) } ?? ""
    }

    private func number(_ stmt: OpaquePointer, _ column: Int32) -> Double? {
        sqlite3_column_type(stmt, column) == SQLITE_NULL ? nil : sqlite3_column_double(stmt, column)
    }

    static func normalize(_ text: String) -> String {
        text.lowercased().replacingOccurrences(of: "ı", with: "i")
            .decomposedStringWithCanonicalMapping
            .unicodeScalars.filter { !CharacterSet.nonBaseCharacters.contains($0) }
            .map(String.init).joined()
    }

    func cities() throws -> [City] {
        try rows("SELECT id,name,provider FROM ios_cities ORDER BY id") { stmt in
            City(id: Int(sqlite3_column_int64(stmt, 0)), name: text(stmt, 1), provider: text(stmt, 2))
        }
    }

    func coverage(city: Int) throws -> (routes: Int, stops: Int, sources: [CatalogSource]) {
        let values: [SQLValue] = [.int(Int64(city)), .int(Int64(city))]
        let counts = try rows("""
            SELECT (SELECT count(*) FROM routes WHERE city=?),(SELECT count(*) FROM stops WHERE city=?)
            """, values) { (Int(sqlite3_column_int64($0, 0)), Int(sqlite3_column_int64($0, 1))) }.first ?? (0, 0)
        let sources = try rows("""
            SELECT name,url,license FROM sources WHERE id IN
              (SELECT DISTINCT source FROM routes WHERE city=? UNION SELECT DISTINCT source FROM stops WHERE city=?)
            ORDER BY id
            """, values) {
                let url = URL(string: text($0, 1))
                return CatalogSource(name: text($0, 0), url: url?.scheme == "https" ? url : nil, license: text($0, 2))
            }
        return (counts.0, counts.1, sources)
    }

    private func entry(_ stmt: OpaquePointer, _ city: Int, _ kind: Kind) -> Entry {
        Entry(id: sqlite3_column_int64(stmt, 0), city: city, kind: kind,
              code: text(stmt, 1), name: text(stmt, 2), type: kind == .routes ? text(stmt, 3) : nil,
              lat: kind == .stops ? number(stmt, 3) : nil,
              lon: kind == .stops ? number(stmt, 4) : nil)
    }

    func search(city: Int, kind: Kind, query: String, offset: Int) throws -> [Entry] {
        let columns = kind == .routes ? "id,code,name,type" : "id,code,name,lat,lon"
        let query = Self.normalize(query.trimmingCharacters(in: .whitespacesAndNewlines))
        if query.isEmpty {
            return try rows("""
                SELECT \(columns) FROM \(kind.rawValue) WHERE city=? ORDER BY code,id LIMIT 50 OFFSET ?
                """, [.int(Int64(city)), .int(Int64(offset))]) { entry($0, city, kind) }
        }
        return try rows("""
            SELECT \(columns) FROM \(kind.rawValue) WHERE city=? AND instr(search,?)>0
            ORDER BY (lower(code)=?) DESC,code,id LIMIT 50 OFFSET ?
            """, [.int(Int64(city)), .text(query), .text(query), .int(Int64(offset))]) {
                entry($0, city, kind)
            }
    }

    func regionalRail(city: Int) throws -> [Entry] {
        guard city == 2 || city == 6 else { return [] }
        return try rows("""
            SELECT id,code,name,type FROM routes WHERE city=? AND type IN ('Metro','Tramvay')
            ORDER BY type,code,id
            """, [.int(Int64(city))]) { entry($0, city, .routes) }
    }

    func nearby(city: Int, point: Point) throws -> [(Entry, Double)] {
        let lat = 0.019
        let lon = lat / cos(point.lat * Double.pi / 180)
        let candidates = try rows("""
            SELECT id,code,name,lat,lon FROM stops
            WHERE city=? AND lat BETWEEN ? AND ? AND lon BETWEEN ? AND ?
            """, [.int(Int64(city)), .double(point.lat - lat), .double(point.lat + lat),
                  .double(point.lon - lon), .double(point.lon + lon)]) { entry($0, city, .stops) }
        return Array(candidates.compactMap { candidate -> (Entry, Double)? in
            guard let location = candidate.point else { return nil }
            let distance = point.distance(to: location)
            return distance <= 2000 ? (candidate, distance) : nil
        }.sorted { $0.1 < $1.1 }.prefix(50))
    }

    func route(_ item: Entry) throws -> RouteDetails {
        let schedule = item.city == 0 ? try rows("SELECT json FROM ego_schedules WHERE code=?",
                                                [.text(item.code)]) { text($0, 0) }.first : nil
        let ego = try schedule.map { try JSONDecoder().decode(EGOSchedule.self, from: Data($0.utf8)) }
        guard let id = item.id else {
            return RouteDetails(stops: [], parts: [], schematic: false, schedule: ego)
        }
        let stops = try rows("""
            SELECT s.id,s.code,s.name,s.lat,s.lon FROM route_stops r JOIN stops s ON s.id=r.stop
            WHERE r.route=? ORDER BY r.seq
            """, [.int(id)]) { entry($0, item.city, .stops) }
        let geometry = try rows("SELECT points FROM ios_shapes WHERE route=?", [.int(id)]) { stmt -> Data in
            guard let bytes = sqlite3_column_blob(stmt, 0) else { return Data() }
            return Data(bytes: bytes, count: Int(sqlite3_column_bytes(stmt, 0)))
        }.first
        if let geometry = geometry, geometry.count >= 16, geometry.count % 8 == 0 {
            let points = geometry.withUnsafeBytes { (bytes: UnsafeRawBufferPointer) -> [Point] in
                func coordinate(_ i: Int) -> Double {
                    let word = UInt32(bytes[i]) << 24 | UInt32(bytes[i + 1]) << 16
                        | UInt32(bytes[i + 2]) << 8 | UInt32(bytes[i + 3])
                    return Double(Int32(bitPattern: word)) / 1_000_000
                }
                return stride(from: 0, to: bytes.count, by: 8).map {
                    Point(lat: coordinate($0), lon: coordinate($0 + 4))
                }
            }
            return RouteDetails(stops: stops, parts: [points], schematic: false, schedule: ego)
        }
        return RouteDetails(stops: stops, parts: [stops.compactMap { $0.point }],
                            schematic: true, schedule: ego)
    }

    func departures(_ item: Entry, now: Date = Date()) throws -> [Departure] {
        guard let id = item.id else { return [] }
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = Self.timeZone
        let day = calendar.startOfDay(for: now)
        let dateKey = DateFormatter()
        dateKey.calendar = calendar
        dateKey.locale = Locale(identifier: "en_US_POSIX")
        dateKey.timeZone = calendar.timeZone
        dateKey.dateFormat = "yyyyMMdd"
        var result: [Departure] = []
        for offset in -1...1 {
            guard let date = calendar.date(byAdding: .day, value: offset, to: day) else { continue }
            let weekday = (calendar.component(.weekday, from: date) + 5) % 7
            let key = dateKey.string(from: date)
            let begin = Int64(ceil(now.timeIntervalSince(date)))
            let end = Int64(ceil(now.addingTimeInterval(86400).timeIntervalSince(date)))
            result += try rows("""
                SELECT r.code,r.name,d.time FROM departures d
                JOIN routes r ON r.id=d.route JOIN services s ON s.id=d.service
                LEFT JOIN exceptions e ON e.service=s.id AND e.date=?
                WHERE d.stop=? AND d.time>=? AND d.time<? AND
                  COALESCE(e.type=1,(s.start<=? AND s.end>=? AND (s.mask & ?) != 0))
                ORDER BY d.time LIMIT 12
                """, [.text(key), .int(id), .int(begin), .int(end), .text(key),
                      .text(key), .int(1 << weekday)]) { stmt in
                Departure(line: text(stmt, 0), name: text(stmt, 1),
                          instant: date.addingTimeInterval(sqlite3_column_double(stmt, 2)))
            }
        }
        return Array(result.sorted { $0.instant < $1.instant }.prefix(12))
    }
}

struct SourceLocation: Decodable {
    let code: String?
    let name: String?
    let lat: Double?
    let lon: Double?

    private enum CodingKeys: String, CodingKey { case code, name, lat, lon }

    init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        code = (try? values.decode(String.self, forKey: .code))
            ?? (try? values.decode(Int.self, forKey: .code)).map(String.init)
        name = try? values.decode(String.self, forKey: .name)
        lat = try? values.decode(Double.self, forKey: .lat)
        lon = try? values.decode(Double.self, forKey: .lon)
    }

    var point: Point? {
        guard let lat = lat, let lon = lon else { return nil }
        let point = Point(lat: lat, lon: lon)
        return point.valid ? point : nil
    }
}

struct Vehicle: Decodable {
    let line: String
    let name: String
    let eta: String
    let sourceTime: String?
    let lat: Double?
    let lon: Double?

    var point: Point? {
        guard let lat = lat, let lon = lon else { return nil }
        let point = Point(lat: lat, lon: lon)
        return point.valid ? point : nil
    }
}

struct LiveResult: Decodable {
    let source: String
    let rows: [Vehicle]
    let mapRows: [Vehicle]
    let separateMapRows: Bool
    let receivedAt: Double
    let refreshMillis: Double
    let message: String
    let stop: SourceLocation?
}

struct OnlineCatalog: Decodable {
    struct Row: Decodable {
        let code: String
        let name: String
        let lat: Double?
        let lon: Double?

        private enum CodingKeys: String, CodingKey { case code, name, lat, lon }

        init(from decoder: Decoder) throws {
            let values = try decoder.container(keyedBy: CodingKeys.self)
            if let text = try? values.decode(String.self, forKey: .code) {
                code = text
            } else {
                code = String(try values.decode(Int.self, forKey: .code))
            }
            name = try values.decode(String.self, forKey: .name)
            lat = try? values.decode(Double.self, forKey: .lat)
            lon = try? values.decode(Double.self, forKey: .lon)
        }

        func entry(city: Int, kind: Kind) -> Entry {
            Entry(id: nil, city: city, kind: kind, code: code, name: name, type: nil, lat: lat, lon: lon)
        }
    }

    let items: [Row]
}

struct OnlineRoute: Decodable {
    let parts: [[Point]]
    let stops: [SourceLocation]
    let schematic: Bool
    let source: String
    let notice: String
}

final class TransitAPI {
    private let base: URL
    private let session: URLSession

    init(base: URL) {
        self.base = base
        let config = URLSessionConfiguration.ephemeral
        config.timeoutIntervalForRequest = 18
        config.timeoutIntervalForResource = 30
        config.requestCachePolicy = .reloadIgnoringLocalCacheData
        self.session = URLSession(configuration: config)
    }

    @discardableResult
    func get<T: Decodable>(_ path: String, _ params: [String: String],
                           _ completion: @escaping (Result<T, Error>) -> Void) -> URLSessionDataTask? {
        var components = URLComponents(url: base.appendingPathComponent("api").appendingPathComponent("v2")
            .appendingPathComponent(path), resolvingAgainstBaseURL: false)
        components?.queryItems = params.sorted { $0.key < $1.key }.map { URLQueryItem(name: $0.key, value: $0.value) }
        guard let url = components?.url else {
            completion(.failure(TransitError.response))
            return nil
        }
        let task = session.dataTask(with: url) { data, response, error in
            let result = Result<T, Error> {
                if let error = error { throw error }
                guard let data = data, let response = response as? HTTPURLResponse,
                      response.url?.scheme == "https" else { throw TransitError.response }
                let decoder = JSONDecoder()
                if !(200..<300).contains(response.statusCode) {
                    throw TransitError.message(responseMessage(data, decoder: decoder, status: response.statusCode))
                }
                return try decoder.decode(T.self, from: data)
            }
            DispatchQueue.main.async { completion(result) }
        }
        task.resume()
        return task
    }
}

private struct APIErrorBody: Decodable { let message: String? }

private func responseMessage(_ data: Data, decoder: JSONDecoder, status: Int) -> String {
    (try? decoder.decode(APIErrorBody.self, from: data))?.message
        ?? "Servis hatası (HTTP \(status))."
}
