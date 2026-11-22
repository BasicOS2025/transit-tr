import Foundation

func check(_ test: @autoclosure () -> Bool, _ message: String) {
    guard test() else { fatalError(message) }
}

let catalog = Catalog(path: CommandLine.arguments[1])
let cities = try catalog.cities()
check(cities.count == 41, "city count")
check([0,1,2,3,4,5,6].allSatisfy { id in cities.contains { $0.id == id } }, "core cities missing")
let counts = try cities.map { try catalog.coverage(city: $0.id) }
check(counts.reduce(0) { $0 + $1.routes } == 9900, "route counts")
check(counts.reduce(0) { $0 + $1.stops } == 72226, "stop counts")
check(Catalog.normalize("İŞIK ÖĞÜT ÇAĞRI") == "isik ogut cagri", "Turkish normalization")
let query = try catalog.search(city: 0, kind: .stops, query: "anayasa mahkemesi", offset: 0)
check(query.contains { $0.code == "10738" }, "Turkish search")
let stops = try catalog.search(city: 0, kind: .stops, query: "10940", offset: 0)
guard let stop = stops.first(where: { $0.code == "10940" }), let point = stop.point else { fatalError("stop") }
let local = try catalog.nearby(city: 0, point: point)
check(local.contains { $0.0 == stop && $0.1 < 1 }, "nearby")
check(local.count <= 50 && local.allSatisfy { $0.1 <= 2000 }, "nearby bounds")
let buses = try catalog.search(city: 0, kind: .routes, query: "481", offset: 0)
let bus = try catalog.route(buses.first { $0.code == "481" }!)
check(!(bus.schedule?.days[0].isEmpty ?? true), "bundled EGO schedule")
check(bus.stops.count > 1 && bus.schematic, "EGO route")
let rail = try catalog.regionalRail(city: 2)
check(rail.count == 7, "Izmir rail")
let ist = try catalog.coverage(city: 1)
check(ist.stops == 15631 && !ist.sources.isEmpty, "Istanbul coverage and sources")
let izmir = try catalog.route(rail.first!)
check(izmir.parts.first?.count ?? 0 > 1, "route geometry")
let format = ISO8601DateFormatter()
let days = ["2026-11-22T10:00:00Z", "2030-11-22T10:00:00Z"]
let testStop = try catalog.search(city: 2, kind: .stops, query: "Konak", offset: 0)
let relevant = try testStop.flatMap { try catalog.departures($0, now: format.date(from: days[0])!) }
let expired = try testStop.flatMap { try catalog.departures($0, now: format.date(from: days[1])!) }
check(!relevant.isEmpty, "active GTFS")
check(expired.isEmpty, "expired GTFS not shown")
if CommandLine.arguments.count > 2 {
    let result = try JSONDecoder().decode(LiveResult.self, from: Data(contentsOf: URL(fileURLWithPath: CommandLine.arguments[2])))
    check(!result.source.isEmpty && result.refreshMillis >= 30000, "live contract")
}
print("PASS: 41 city records, Turkish search, regional coverage, nearby radius, EGO timetable and route, GTFS dates, live payload")
