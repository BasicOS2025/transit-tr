import UIKit
import WidgetKit

final class LiveController: TransitScreen, UITextFieldDelegate {
    private let stop = UITextField()
    private let line = UITextField()
    private var selectedStop: Entry?
    private var pendingWidget: WidgetSnapshot?
    private var pin: UIButton?
    private var result: LiveResult?
    private var status = ""
    private var task: URLSessionDataTask?
    private var timer: Timer?
    private var revision = UUID()
    private var tracking = false
    private var showing = false

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "Otobüs"
        configure(stop, hint: "Durak numarası", keyboard: .numberPad)
        configure(line, hint: "Hat kodu (isteğe bağlı)", keyboard: .asciiCapable)
        let pin = button("Widget’a sabitle", action: #selector(pinWidget))
        pin.isEnabled = false
        self.pin = pin
        header([stop, line, button("Varışları sorgula", action: #selector(search)), pin])
        navigationItem.leftBarButtonItem = UIBarButtonItem(title: "Bilgi", style: .plain, target: self,
                                                            action: #selector(about))
        let notifications = NotificationCenter.default
        notifications.addObserver(self, selector: #selector(pause), name: UIApplication.didEnterBackgroundNotification,
                                  object: nil)
        notifications.addObserver(self, selector: #selector(resume), name: UIApplication.didBecomeActiveNotification,
                                  object: nil)
        resetStatus()
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        showing = true
        if let item = pendingWidget { selectWidget(item) } else { resume() }
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        showing = false
        pause()
    }

    override func cityChanged() {
        super.cityChanged()
        clear()
        if let item = pendingWidget, item.city == city.id, showing {
            selectWidget(item)
            return
        }
        stop.text = nil
        line.text = nil
        selectedStop = nil
        resetStatus()
    }

    func selectStop(_ entry: Entry) {
        loadViewIfNeeded()
        if entry.city != city.id, let target = state.cities.first(where: { $0.id == entry.city }) {
            state.setCity(target)
        }
        selectedStop = entry
        stop.text = entry.code
        line.text = nil
        search()
    }

    func selectWidget() {
        if let item = WidgetSnapshot.read() { selectWidget(item) }
    }

    private func selectWidget(_ item: WidgetSnapshot) {
        loadViewIfNeeded()
        pendingWidget = item
        guard showing, let target = state.cities.first(where: { $0.id == item.city }) else { return }
        pendingWidget = nil
        if city.id != item.city { state.setCity(target) }
        selectedStop = nil
        stop.text = item.stop
        line.text = item.line
        search()
    }

    @objc private func pinWidget() {
        guard let result = result else { return }
        if updateWidget(result, code: stop.text?.trimmingCharacters(in: .whitespacesAndNewlines) ?? "",
                        line: line.text?.trimmingCharacters(in: .whitespacesAndNewlines) ?? "") {
            message("Widget", "Durak sabitlendi. Transit TR widget’ını ekleyerek son sorguyu görebilirsin.")
        } else {
            message("Widget", "Uygulama ve widget aynı App Group ile imzalanmalı.")
        }
    }

    @discardableResult private func updateWidget(_ value: LiveResult, code: String, line: String) -> Bool {
        let stamp = Date(timeIntervalSince1970: value.receivedAt / 1000)
        let snapshot = WidgetSnapshot(city: city.id, stop: code, name: value.stop?.name ?? selectedStop?.name ?? "Durak",
                                      line: line, arrivals: value.rows.prefix(2).map { $0.line + " · " + $0.eta },
                                      updated: stamp)
        guard snapshot.save() else { return false }
        if #available(iOS 14.0, *) { WidgetCenter.shared.reloadTimelines(ofKind: "TransitTR.Arrivals") }
        return true
    }

    private func configure(_ field: UITextField, hint: String, keyboard: UIKeyboardType) {
        field.placeholder = hint
        field.accessibilityLabel = hint
        field.borderStyle = .roundedRect
        field.keyboardType = keyboard
        field.autocapitalizationType = .allCharacters
        field.autocorrectionType = .no
        field.clearButtonMode = .whileEditing
        field.returnKeyType = .search
        field.font = .preferredFont(forTextStyle: .body)
        field.adjustsFontForContentSizeCategory = true
        field.addTarget(self, action: #selector(fieldsEdited), for: .editingChanged)
        field.delegate = self
        field.heightAnchor.constraint(greaterThanOrEqualToConstant: 44).isActive = true
    }

    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        search()
        return false
    }

    @objc private func about() {
        message("Transit TR · iOS", """
            Katalog ve paketlenmiş tarifeler cihazda çalışır. Canlı sorgular Transit TR sunucusuna şehir, durak ve varsa hat kodu gönderir.

            Yakın durak araması cihazda yapılır. Haritayı açtığında görünür alanın harita karoları OpenStreetMap’ten istenir.
            """)
    }

    @objc private func fieldsEdited() {
        clear()
        if selectedStop?.code != stop.text { selectedStop = nil }
        resetStatus()
    }

    private func resetStatus() {
        status = city.hasLiveService ? "Durak numarasını gir veya katalogdan bir durak seç."
            : "Bu şehir için canlı varış kaynağı bağlı değil."
        tableView.reloadData()
    }

    @objc private func search() {
        view.endEditing(true)
        guard city.hasLiveService else { resetStatus(); return }
        let stop = stop.text?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let line = line.text?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let pattern = city.id == 0 ? #"^[0-9]{5}$"# : #"^[0-9]{1,8}$"#
        guard stop.range(of: pattern, options: .regularExpression) != nil else {
            message("Durak numarası", city.id == 0 ? "Ankara için 5 haneli durak numarası gir." :
                    "1 ile 8 hane arasında durak numarası gir.")
            return
        }
        guard line.range(of: #"^[A-Za-z0-9ÇĞİÖŞÜçğıöşü_ -]{0,80}$"#, options: .regularExpression) != nil else {
            message("Hat kodu", "Geçerli bir hat kodu gir.")
            return
        }
        tracking = true
        query(stop: stop, line: line, manual: true)
    }

    private func query(stop code: String, line: String, manual: Bool) {
        pause()
        let token = revision
        if manual {
            result = nil
            pin?.isEnabled = false
            status = "Varış bilgisi alınıyor…"
            tableView.reloadData()
        }
        let city = self.city
        task = state.api.get("live", ["city": String(city.id), "stop": code, "line": line]) {
            [weak self] (response: Result<LiveResult, Error>) in
            guard let self = self, self.revision == token else { return }
            self.task = nil
            var refresh = city.id == 1 ? 60.0 : 30.0
            switch response {
            case .success(let value):
                self.result = value
                self.status = value.message
                if let saved = WidgetSnapshot.read(), saved.city == city.id && saved.stop == code && saved.line == line {
                    self.updateWidget(value, code: code, line: line)
                }
                if value.refreshMillis.isFinite {
                    refresh = max(refresh, value.refreshMillis / 1000)
                }
            case .failure(let error):
                self.result = nil
                self.status = "Canlı varış alınamadı. \(error.localizedDescription)"
            }
            self.pin?.isEnabled = self.result != nil
            self.tableView.reloadData()
            guard self.showing, self.tracking, UIApplication.shared.applicationState == .active else { return }
            self.timer = Timer.scheduledTimer(withTimeInterval: refresh, repeats: false) { [weak self] _ in
                self?.query(stop: code, line: line, manual: false)
            }
        }
    }

    @objc private func pause() {
        timer?.invalidate()
        timer = nil
        revision = UUID()
        task?.cancel()
        task = nil
    }

    @objc private func resume() {
        guard showing, tracking, task == nil, timer == nil, let code = stop.text?.trimmingCharacters(in: .whitespacesAndNewlines) else { return }
        query(stop: code, line: line.text?.trimmingCharacters(in: .whitespacesAndNewlines) ?? "", manual: false)
    }

    private func clear() {
        pause()
        tracking = false
        result = nil
        pin?.isEnabled = false
    }

    private var pins: [MapPin] {
        guard let result = result else { return [] }
        var pins: [MapPin] = []
        if let point = result.stop?.point ?? selectedStop?.point {
            let name = result.stop?.name ?? selectedStop?.name ?? "Seçilen durak"
            pins.append(MapPin(point: point, title: name, subtitle: result.stop?.code ?? stop.text, vehicle: false))
        }
        pins += result.mapRows.prefix(300).compactMap { vehicle in
            guard let point = vehicle.point else { return nil }
            let detail = result.separateMapRows ? vehicle.sourceTime :
                [vehicle.eta, vehicle.sourceTime].compactMap { $0 }.joined(separator: " · ")
            return MapPin(point: point, title: vehicle.line, subtitle: detail, vehicle: true)
        }
        return pins
    }

    override func numberOfSections(in tableView: UITableView) -> Int { result == nil ? 1 : 2 }

    override func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        if section == 1 { return "Durak varışları" }
        return result?.source
    }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        guard let result = result else { return 1 }
        if section == 1 { return max(1, result.rows.count) }
        return pins.isEmpty ? 1 : 2
    }

    override func tableView(_ tableView: UITableView, cellForRowAt path: IndexPath) -> UITableViewCell {
        guard let result = result else { return cell(status) }
        if path.section == 0 {
            if path.row == 1 {
                let description = result.separateMapRows ? "Hat filosu GPS konumları" : "Kaynak GPS konumları"
                return cell("Haritada aç", description, link: true)
            }
            let title = result.stop?.name ?? selectedStop?.name ?? "Durak " + (stop.text ?? "")
            let stamp = Self.date(Date(timeIntervalSince1970: result.receivedAt / 1000), includeDay: true)
            return cell(title, (["Servisin veri alma zamanı · " + stamp, status].filter { !$0.isEmpty })
                .joined(separator: "\n"))
        }
        if result.rows.isEmpty { return cell("Kaynak bu sorguda yaklaşan araç göstermedi.") }
        let row = result.rows[path.row]
        return cell(row.line + " · " + row.eta, row.name)
    }

    override func tableView(_ tableView: UITableView, didSelectRowAt path: IndexPath) {
        tableView.deselectRow(at: path, animated: true)
        guard path.section == 0, path.row == 1, let value = result else { return }
        let stamp = Self.date(Date(timeIntervalSince1970: value.receivedAt / 1000), includeDay: true)
        let label = value.separateMapRows ? "Hat filosu GPS · " : "Kaynak GPS · "
        showMap("Araçlar ve durak", pins: pins, caption: label + stamp)
    }
}
