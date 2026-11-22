import UIKit
import CoreLocation

final class CatalogController: TransitScreen, UISearchBarDelegate, CLLocationManagerDelegate {
    private let selection = UISegmentedControl(items: ["Hatlar", "Duraklar", "Yakınımda", "Favoriler"])
    private let search = UISearchBar()
    private var entries: [Entry] = []
    private var distances: [Double?] = []
    private var status = ""
    private var revision = UUID()
    private var searchWork: DispatchWorkItem?
    private var request: URLSessionDataTask?
    private var location: CLLocationManager?
    private var canPage = false
    private var loading = false

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "Katalog"
        selection.selectedSegmentIndex = 0
        selection.addTarget(self, action: #selector(modeChanged), for: .valueChanged)
        search.delegate = self
        search.searchBarStyle = .minimal
        search.autocapitalizationType = .none
        search.placeholder = "Hat kodu veya adı"
        header([selection, search])
        reloadList()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        if selection.selectedSegmentIndex == 3 { reloadList() }
    }

    override func cityChanged() {
        super.cityChanged()
        reloadList()
    }

    @objc private func modeChanged() {
        search.resignFirstResponder()
        search.isHidden = selection.selectedSegmentIndex >= 2
        search.placeholder = selection.selectedSegmentIndex == 0 ? "Hat kodu veya adı" : "Durak kodu veya adı"
        view.setNeedsLayout()
        reloadList()
    }

    func searchBar(_ searchBar: UISearchBar, textDidChange searchText: String) {
        resetPending()
        let work = DispatchWorkItem { [weak self] in self?.reloadList() }
        searchWork = work
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.22, execute: work)
    }

    func searchBarSearchButtonClicked(_ searchBar: UISearchBar) { searchBar.resignFirstResponder() }

    private func resetPending() {
        revision = UUID()
        searchWork?.cancel()
        request?.cancel()
        location?.delegate = nil
        location = nil
        loading = false
        canPage = false
    }

    private func reloadList() {
        resetPending()
        entries = []
        distances = []
        status = ""
        switch selection.selectedSegmentIndex {
        case 2:
            status = "Konumunu kullanarak 2 km içindeki durakları ara."
            tableView.reloadData()
        case 3:
            entries = state.favorites.filter { $0.city == city.id }
            status = entries.isEmpty ? "Bu şehirde kayıtlı favori yok." : ""
            tableView.reloadData()
        default:
            loadPage()
        }
    }

    private func loadPage() {
        guard !loading else { return }
        loading = true
        let token = revision
        let city = self.city
        let kind: Kind = selection.selectedSegmentIndex == 0 ? .routes : .stops
        let query = String((search.text ?? "").trimmingCharacters(in: .whitespacesAndNewlines).prefix(100))
        let offset = entries.count
        if offset == 0 {
            status = "Katalog yükleniyor…"
            tableView.reloadData()
        }
        state.catalog.read({ try $0.search(city: city.id, kind: kind, query: query, offset: offset) }) {
            [weak self] result in
            guard let self = self, self.revision == token else { return }
            switch result {
            case .success(let records) where records.isEmpty && offset == 0 &&
                city.hasOnlineStopSearch && !query.isEmpty && (kind == .stops || city.id == 9):
                self.request = self.state.api.get("catalog", ["city": String(city.id), "kind": kind.rawValue,
                                                               "q": query]) {
                    [weak self] (result: Result<OnlineCatalog, Error>) in
                    guard let self = self, self.revision == token else { return }
                    switch result {
                    case .success(let value):
                        self.updateList(value.items.map { $0.entry(city: city.id, kind: kind) },
                                        status: value.items.isEmpty ? "Eşleşen kayıt yok." : "Çevrimiçi kaynak",
                                        canPage: false)
                    case .failure(let error):
                        self.updateList([], status: "Çevrimiçi arama alınamadı. \(error.localizedDescription)",
                                        canPage: false)
                    }
                }
            case .success(let records):
                let hint = city.hasOnlineStopSearch && kind == .stops && query.isEmpty
                    ? "Çevrimiçi durak araması için kod veya ad gir." : "Eşleşen yerel kayıt yok."
                self.updateList(records, status: records.isEmpty && offset == 0 ? hint : "", canPage: records.count == 50)
            case .failure(let error):
                self.updateList([], status: error.localizedDescription, canPage: false)
            }
        }
    }

    private func updateList(_ records: [Entry], status: String, canPage: Bool) {
        let firstPage = entries.isEmpty
        entries.append(contentsOf: records)
        self.status = status
        self.canPage = canPage
        loading = false
        tableView.reloadData()
        if firstPage && !records.isEmpty { tableView.setContentOffset(.zero, animated: false) }
    }

    private func startLocation() {
        resetPending()
        entries = []
        distances = []
        status = "Konum bekleniyor…"
        tableView.reloadData()
        let manager = CLLocationManager()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
        location = manager
        useAuthorization(CLLocationManager.authorizationStatus(), manager: manager)
    }

    private func useAuthorization(_ authorization: CLAuthorizationStatus, manager: CLLocationManager) {
        switch authorization {
        case .notDetermined: manager.requestWhenInUseAuthorization()
        case .authorizedWhenInUse, .authorizedAlways: manager.requestLocation()
        case .restricted, .denied: locationFailed("Konum için Ayarlar’dan Transit TR’ye izin ver.")
        @unknown default: locationFailed("Konum izni kullanılamıyor.")
        }
    }

    func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        guard manager === location, status != .notDetermined else { return }
        useAuthorization(status, manager: manager)
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard manager === location else { return }
        location = nil
        guard let coordinate = locations.last?.coordinate else { locationFailed("Konum alınamadı."); return }
        let point = Point(lat: coordinate.latitude, lon: coordinate.longitude)
        guard point.valid else { locationFailed("Bu konum için arama yapılamıyor."); return }
        let token = revision
        let city = self.city
        state.catalog.read({ try $0.nearby(city: city.id, point: point) }) { [weak self] result in
            guard let self = self, self.revision == token else { return }
            switch result {
            case .success(let rows) where !rows.isEmpty:
                self.distances = rows.map { $0.1 }
                self.updateList(rows.map { $0.0 }, status: "Yerel duraklar · kuş uçuşu mesafe", canPage: false)
            case .success(_): self.locationFailed("2 km içinde yerel durak kaydı yok.")
            case .failure(let error): self.locationFailed(error.localizedDescription)
            }
        }
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        guard manager === location else { return }
        locationFailed("Konum alınamadı. \(error.localizedDescription)")
    }

    private func locationFailed(_ text: String) {
        location = nil
        updateList([], status: text, canPage: false)
    }

    override func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        status.isEmpty ? nil : status
    }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        entries.count + ((selection.selectedSegmentIndex == 2 || canPage) ? 1 : 0)
    }

    override func tableView(_ tableView: UITableView, cellForRowAt path: IndexPath) -> UITableViewCell {
        if selection.selectedSegmentIndex == 2 && path.row == 0 {
            return cell("Konumumu kullan", "Konum cihazda işlenir.", link: true)
        }
        let index = path.row - (selection.selectedSegmentIndex == 2 ? 1 : 0)
        if index == entries.count { return cell(loading ? "Yükleniyor…" : "Daha fazla göster", link: !loading) }
        let entry = entries[index]
        let distance = distances.indices.contains(index) ? distances[index] : nil
        let detail = distance.flatMap { $0.isFinite && $0 >= 0 && $0 <= 2000 ? "\(Int($0.rounded())) m" : nil }
            ?? entry.type
        return entry.kind == .routes ? codedCell(entry.code, entry.name, detail) : cell(entry.title, detail, link: true)
    }

    override func tableView(_ tableView: UITableView, didSelectRowAt path: IndexPath) {
        tableView.deselectRow(at: path, animated: true)
        if selection.selectedSegmentIndex == 2 && path.row == 0 { startLocation(); return }
        let index = path.row - (selection.selectedSegmentIndex == 2 ? 1 : 0)
        if index == entries.count { loadPage(); return }
        guard entries.indices.contains(index) else { return }
        navigationController?.pushViewController(EntryController(state, entries[index]), animated: true)
    }
}

final class EntryController: TransitScreen {
    private struct Row {
        let title: String
        let detail: String?
        let action: (() -> Void)?

        init(_ title: String, _ detail: String? = nil, action: (() -> Void)? = nil) {
            self.title = title
            self.detail = detail
            self.action = action
        }
    }

    private let entry: Entry
    private var sections: [(String, [Row])] = []
    private var actions: [Row] = []
    private var routeTask: URLSessionDataTask?

    init(_ state: AppState, _ entry: Entry) {
        self.entry = entry
        super.init(state)
    }

    required init?(coder: NSCoder) { fatalError("Use init(_:_:)") }

    override func viewDidLoad() {
        super.viewDidLoad()
        title = entry.code.isEmpty ? "Ayrıntı" : entry.code
        refreshStar()
        navigationItem.largeTitleDisplayMode = .never
        if entry.kind == .routes {
            state.catalog.read({ [entry] in try $0.route(entry) }) { [weak self] result in
                guard let self = self else { return }
                switch result {
                case .success(let data): self.renderRoute(data)
                case .failure(let error): self.renderError(error)
                }
            }
        } else {
            renderStop()
        }
    }

    deinit { routeTask?.cancel() }

    override func cityChanged() {}

    private func refreshStar() {
        let star = state.isFavorite(entry) ? "★" : "☆"
        let button = UIBarButtonItem(title: star, style: .plain, target: self, action: #selector(toggleFavorite))
        button.accessibilityLabel = state.isFavorite(entry) ? "Favorilerden çıkar" : "Favorilere ekle"
        navigationItem.rightBarButtonItem = button
    }

    @objc private func toggleFavorite() {
        if state.toggleFavorite(entry) { refreshStar() }
        else { message("Favoriler", "En fazla 200 kayıt saklanabilir.") }
    }

    private func renderStop() {
        if city.hasLiveService && entry.code.range(of: #"^\d{1,8}$"#, options: .regularExpression) != nil {
            actions.append(Row("Varışları sorgula") { [weak self] in
                guard let self = self else { return }
                self.state.openLive?(self.entry)
            })
        }
        if let point = entry.point {
            actions.append(Row("Haritada aç") { [weak self] in
                guard let self = self else { return }
                self.showMap(self.entry.code, pins: [MapPin(point: point, title: self.entry.title,
                                                            subtitle: nil, vehicle: false)])
            })
        }
        sections = [("Planlı kalkışlar · Türkiye saati", [Row("Tarife okunuyor…")])]
        tableView.reloadData()
        state.catalog.read({ [entry] in try $0.departures(entry) }) { [weak self] result in
            guard let self = self else { return }
            switch result {
            case .success(let departures):
                let rows = departures.isEmpty ? [Row("Önümüzdeki 24 saat için geçerli yerel tarife yok.")] :
                    departures.map {
                        Row(Self.date($0.instant, includeDay: false) + " · " + $0.line, $0.name)
                    }
                self.sections = [("Planlı kalkışlar · canlı varış değildir", rows)]
                self.tableView.reloadData()
            case .failure(let error): self.renderError(error)
            }
        }
    }

    private func renderRoute(_ data: RouteDetails) {
        var sections: [(String, [Row])] = []
        if let schedule = data.schedule {
            let stamp = schedule.synced_at.map { Self.date(Date(timeIntervalSince1970: $0 / 1000), includeDay: true) }
            sections.append(("Paketlenmiş EGO hat tarifesi", [Row(stamp.map { "Kaynak kaydı · " + $0 }
                ?? "Kayıt tarihi bilinmiyor", "Türkiye saati. Özel günlerde çizelge değişebilir.")]))
            for (index, title) in ["Hafta içi", "Cumartesi", "Pazar"].enumerated()
                where schedule.days.indices.contains(index) {
                let times = schedule.days[index]
                sections.append((title, [Row(times.isEmpty ? "Kalkış kaydı yok." : times.joined(separator: "\n"))]))
            }
        }
        if !data.stops.isEmpty { sections.append(("Sıralı duraklar", stopRows(data.stops))) }
        if data.parts.contains(where: { $0.count > 1 }) {
            let caption = data.schematic ? "Duraklar arasındaki şematik bağlantı" : "Yerel güzergâh verisi"
            actions.append(routeMap(data.parts, stops: data.stops, caption: caption))
        } else if city.hasLiveService {
            actions.append(Row("Çevrimiçi güzergâhı al") { [weak self] in self?.onlineRoute() })
        }
        if data.stops.isEmpty { sections.append(("Sıralı duraklar", [Row("Bu hat için paketlenmiş durak dizisi yok.")])) }
        self.sections = sections
        tableView.reloadData()
    }

    private func stopRows(_ stops: [Entry]) -> [Row] {
        stops.map { stop in
            Row(stop.title) { [weak self] in
                guard let self = self else { return }
                self.navigationController?.pushViewController(EntryController(self.state, stop), animated: true)
            }
        }
    }

    private func routeMap(_ parts: [[Point]], stops: [Entry], caption: String) -> Row {
        Row("Güzergâhı haritada aç", caption) { [weak self] in
            guard let self = self else { return }
            let pins = stops.compactMap { stop -> MapPin? in
                guard let point = stop.point else { return nil }
                return MapPin(point: point, title: stop.title, subtitle: nil, vehicle: false)
            }
            self.showMap(self.entry.code, pins: pins, parts: parts, caption: caption)
        }
    }

    private func onlineRoute() {
        guard routeTask == nil else { return }
        let code = entry.code
        routeTask = state.api.get("route", ["city": String(entry.city), "line": code]) {
            [weak self] (result: Result<OnlineRoute, Error>) in
            guard let self = self else { return }
            self.routeTask = nil
            switch result {
            case .success(let route):
                guard route.parts.contains(where: { $0.filter { $0.valid }.count > 1 }) else {
                    self.message("Güzergâh", "Çizim için yeterli konum yok."); return
                }
                let stops = route.stops.map {
                    Entry(id: nil, city: self.entry.city, kind: .stops, code: $0.code ?? "",
                          name: $0.name ?? "Durak", type: nil, lat: $0.lat, lon: $0.lon)
                }
                let caption = [route.schematic ? "Şematik güzergâh" : route.source, route.notice]
                    .filter { !$0.isEmpty }.joined(separator: "\n")
                let action = self.routeMap(route.parts, stops: stops, caption: caption)
                self.actions = [action]
                if !stops.isEmpty {
                    self.sections.removeAll { $0.0 == "Sıralı duraklar" }
                    self.sections.append(("Sıralı duraklar", self.stopRows(stops)))
                }
                self.tableView.reloadData()
                action.action?()
            case .failure(let error): self.message("Güzergâh alınamadı", error.localizedDescription)
            }
        }
    }

    private func renderError(_ error: Error) {
        sections = [("Veri", [Row(error.localizedDescription)])]
        tableView.reloadData()
    }

    private var content: [(String, [Row])] { [(" ", [Row(entry.name)] + actions)] + sections }

    override func numberOfSections(in tableView: UITableView) -> Int { content.count }
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        content[section].1.count
    }
    override func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        content[section].0
    }
    override func tableView(_ tableView: UITableView, cellForRowAt path: IndexPath) -> UITableViewCell {
        let row = content[path.section].1[path.row]
        return cell(row.title, row.detail, link: row.action != nil)
    }
    override func tableView(_ tableView: UITableView, didSelectRowAt path: IndexPath) {
        tableView.deselectRow(at: path, animated: true)
        content[path.section].1[path.row].action?()
    }
}
