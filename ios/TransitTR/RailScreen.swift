import UIKit

final class RailController: TransitScreen {
    private var lines: [RailLine] = []
    private var regional: [Entry] = []
    private var status = "Raylı sistem ağı yükleniyor…"
    private var revision = UUID()

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "Raylı Sistem"
        load()
    }

    override func cityChanged() {
        super.cityChanged()
        load()
    }

    private func load() {
        let token = UUID()
        revision = token
        let city = self.city.id
        let url = Bundle.main.url(forResource: "rail-networks", withExtension: "json")
        lines = []
        regional = []
        status = "Raylı sistem ağı yükleniyor…"
        tableView.reloadData()
        state.catalog.read({ try $0.regionalRail(city: city) }) { [weak self] result in
            guard let self = self, self.revision == token, case .success(let routes) = result else { return }
            self.regional = routes
            self.tableView.reloadData()
        }
        DispatchQueue.global(qos: .userInitiated).async {
            let loaded = url.flatMap { try? Data(contentsOf: $0) }
                .flatMap { try? JSONDecoder().decode(RailNetwork.self, from: $0) }
            let selected = loaded?.lines.filter { $0.city == city } ?? []
            DispatchQueue.main.async { [weak self] in
                guard let self = self, self.revision == token else { return }
                self.lines = selected
                self.status = loaded == nil ? "Raylı sistem ağı açılamadı." :
                    "Bu şehir için paketlenmiş raylı sistem ağı yok."
                self.tableView.reloadData()
            }
        }
    }

    override func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        lines.isEmpty && regional.isEmpty ? nil : "İstasyon ağı"
    }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        max(1, lines.count + regional.count)
    }

    override func tableView(_ tableView: UITableView, cellForRowAt path: IndexPath) -> UITableViewCell {
        if lines.indices.contains(path.row) {
            let line = lines[path.row]
            return codedCell(line.id, line.name, "\(line.stations.count) istasyon", color: railColor(line.color))
        }
        let index = path.row - lines.count
        guard regional.indices.contains(index) else { return cell(status) }
        let entry = regional[index]
        return codedCell(entry.code, entry.name, entry.type)
    }

    override func tableView(_ tableView: UITableView, didSelectRowAt path: IndexPath) {
        tableView.deselectRow(at: path, animated: true)
        if lines.indices.contains(path.row) {
            navigationController?.pushViewController(RailLineController(state, lines[path.row]), animated: true)
            return
        }
        let index = path.row - lines.count
        if regional.indices.contains(index) {
            navigationController?.pushViewController(EntryController(state, regional[index]), animated: true)
        }
    }

}

private final class RailLineController: TransitScreen {
    private struct EstimateFeed: Decodable {
        struct Train: Decodable {
            let name: String
            let lat: Double
            let lon: Double
            let estimated: Bool
        }
        let trains: [Train]
        let explanation: String
    }

    private let line: RailLine
    private var task: URLSessionDataTask?
    private var supportsEstimate: Bool { [0, 1].contains(line.city) }


    init(_ state: AppState, _ line: RailLine) {
        self.line = line
        super.init(state)
    }

    required init?(coder: NSCoder) { fatalError("Use init(_:_:)") }

    override func viewDidLoad() {
        super.viewDidLoad()
        title = line.id
        navigationItem.largeTitleDisplayMode = .never
        navigationItem.rightBarButtonItem = nil
    }

    override func cityChanged() {}

    override func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        line.name
    }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        line.stations.count + (supportsEstimate ? 2 : 1)
    }

    override func tableView(_ tableView: UITableView, cellForRowAt path: IndexPath) -> UITableViewCell {
        if path.row == 0 {
            return cell("Hattı haritada aç", "İstasyonların şematik bağlantısı", link: !line.stations.isEmpty)
        }
        if supportsEstimate && path.row == 1 {
            return cell(task == nil ? "Tren konumu tahminlerini göster" : "Tahminler alınıyor…",
                        "Saat modeli · GPS değildir", link: task == nil)
        }
        return cell(line.stations[path.row - (supportsEstimate ? 2 : 1)].name)
    }

    override func tableView(_ tableView: UITableView, didSelectRowAt path: IndexPath) {
        tableView.deselectRow(at: path, animated: true)
        if path.row == 0 {
            showRailMap(extra: [], caption: "İstasyonların şematik bağlantısı")
        } else if path.row == 1 && supportsEstimate {
            fetchEstimates()
        }
    }

    private func fetchEstimates() {
        guard task == nil else { return }
        task = state.api.get("rail", ["city": String(line.city), "line": line.id]) {
            [weak self] (result: Result<EstimateFeed, Error>) in
            guard let self = self else { return }
            self.task = nil
            self.tableView.reloadData()
            guard self.navigationController?.topViewController === self, self.view.window != nil else { return }
            switch result {
            case .success(let value):
                let pins = value.trains.filter { $0.estimated }.map { item in
                    MapPin(point: Point(lat: item.lat, lon: item.lon), title: "Tahmini tren",
                           subtitle: item.name, vehicle: true)
                }
                if pins.isEmpty { self.message("Tren tahmini", value.explanation); return }
                self.showRailMap(extra: pins, caption: "Saat modeli · GPS değildir")
            case .failure(let error): self.message("Tahmin alınamadı", error.localizedDescription)
            }
        }
        tableView.reloadData()
    }

    private func showRailMap(extra: [MapPin], caption: String) {
        let pins = line.stations.map { MapPin(point: $0.point, title: $0.name, subtitle: nil, vehicle: false) }
        showMap(line.id, pins: pins + extra, parts: [line.stations.map { $0.point }],
                caption: caption, color: railColor(line.color))
    }

    deinit { task?.cancel() }
}

private func railColor(_ text: String?) -> UIColor? {
    guard let text = text, text.count == 7, text.first == "#",
          let hex = UInt32(text.dropFirst(), radix: 16) else { return nil }
    return UIColor(red: CGFloat((hex >> 16) & 255) / 255, green: CGFloat((hex >> 8) & 255) / 255,
                   blue: CGFloat(hex & 255) / 255, alpha: 1)
}
