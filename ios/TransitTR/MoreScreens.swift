import UIKit

final class AssistantController: TransitScreen, UITextFieldDelegate {
    private let question = UITextField()
    private var answer: String?
    private let topics = ["Yakınımdaki duraklar", "Canlı varış", "Planlı seferler", "Kart bakiyesi"]

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "Mini Asistan"
        navigationItem.rightBarButtonItem = nil
        question.placeholder = "Ulaşım hakkında sor"
        question.accessibilityLabel = "Sorun"
        question.borderStyle = .roundedRect
        question.font = .preferredFont(forTextStyle: .body)
        question.adjustsFontForContentSizeCategory = true
        question.clearButtonMode = .whileEditing
        question.returnKeyType = .go
        question.delegate = self
        question.heightAnchor.constraint(greaterThanOrEqualToConstant: 44).isActive = true
        header([question, button("Sor", action: #selector(ask))])
    }

    override func cityChanged() {}

    @objc private func ask() {
        view.endEditing(true)
        let q = Catalog.normalize(question.text ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        if q.isEmpty {
            answer = "Bir soru yaz veya aşağıdan konu seç."
        } else if q.contains("kart") || q.contains("bakiye") {
            answer = "Kart bakiyesi ve ödeme desteği yok. İşletmecinin web sitesine Servisler’den ulaşabilirsin."
        } else if q.contains("yakin") || q.contains("etraf") {
            answer = "Katalog → Yakınımda → Konumumu kullan. Seçili şehrin cihazdaki kataloğunda 2 km içindeki duraklar aranır."
        } else if q.contains("canli") || q.contains("dakika") || q.contains("ne zaman") {
            answer = "Otobüs ekranında durak koduyla varışları sorgula. Kaynak GPS’i varsa haritada açabilirsin."
        } else if q.contains("tarife") || q.contains("sefer") || q.contains("planli") {
            answer = "Katalogda bir durak seçerek geçerli planlı saatlere bak. Ankara’da hat ayrıntısında EGO’nun yayımladığı haftalık çizelge bulunur."
        } else if q.contains("metro") || q.contains("tramvay") || q.contains("ankaray") || q.contains("baskentray") {
            answer = "Raylı Sistem ekranı seçili şehrin hatlarını ve istasyonlarını gösterir. Saat modelinden hesaplanan tren konumları ayrı gösterilir."
        } else if q.contains("favori") || q.contains("hat") || q.contains("durak") {
            answer = "Katalogda hat kodunu veya durak adını ara. Kayda dokunup yıldızla favorilerine ekleyebilirsin."
        } else {
            answer = "Yakın duraklar, canlı varış, planlı kalkış ve favoriler hakkında sorabilirsin."
        }
        tableView.reloadData()
    }

    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        ask()
        return false
    }

    override func numberOfSections(in tableView: UITableView) -> Int { 2 }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        section == 0 ? 1 : topics.count
    }

    override func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        section == 0 ? "Çevrimdışı" : "Konular"
    }

    override func tableView(_ tableView: UITableView, cellForRowAt path: IndexPath) -> UITableViewCell {
        path.section == 0 ? cell(answer ?? "Uygulama ve ulaşım bilgileri hakkında soru sor.")
            : cell(topics[path.row], link: true)
    }

    override func tableView(_ tableView: UITableView, didSelectRowAt path: IndexPath) {
        tableView.deselectRow(at: path, animated: true)
        guard path.section == 1 else { return }
        question.text = topics[path.row]
        ask()
    }
}

final class ServicesController: TransitScreen {
    private struct NoticeFeed: Decodable {
        struct Notice: Decodable {
            let LineName: String?
            let UpdateDate: String?
            let Description: String?
        }
        let Success: Bool
        let Data: [Notice]?
    }

    private var sources: [CatalogSource] = []
    private var counts = (routes: 0, stops: 0)
    private var notices: [(String, String?)] = []
    private var revision = UUID()
    private var request: URLSessionDataTask?
    private let links = [
        0: "https://www.ego.gov.tr/", 1: "https://iett.istanbul/", 2: "https://www.eshot.gov.tr/",
        3: "https://www.e-komobil.com/", 4: "https://ulasim.muttas.com.tr/", 5: "https://www.eskisehir.bel.tr/ulasim-hizmetleri-otobus-saatleri-dvm.php",
        6: "https://atus.konya.bel.tr/", 8: "https://www.antalya.bel.tr/", 9: "https://www.burulas.com.tr/"
    ]

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "Servisler"
        load()
    }

    override func cityChanged() {
        super.cityChanged()
        load()
    }

    private func load() {
        revision = UUID()
        let token = revision
        request?.cancel()
        request = nil
        sources = []
        counts = (0, 0)
        notices = []
        let cityID = city.id
        tableView.reloadData()
        state.catalog.read({ try $0.coverage(city: cityID) }) { [weak self] result in
            guard let self = self, self.revision == token else { return }
            if case .success(let info) = result {
                self.sources = info.sources
                self.counts = (info.routes, info.stops)
                self.tableView.reloadData()
            }
        }
    }

    private func fetchNotices() {
        guard request == nil else { return }
        let token = revision
        notices = [("Duyurular alınıyor…", nil)]
        tableView.reloadData()
        request = state.api.get("statuses", [:]) { [weak self] (result: Result<NoticeFeed, Error>) in
            guard let self = self, self.revision == token else { return }
            self.request = nil
            switch result {
            case .success(let feed) where feed.Success:
                self.notices = (feed.Data ?? []).map {
                    ($0.LineName ?? "Metro İstanbul",
                     [$0.UpdateDate, $0.Description].compactMap { $0 }.joined(separator: "\n"))
                }
                if self.notices.isEmpty { self.notices = [("Kaynak duyuru döndürmedi.", nil)] }
            case .success:
                self.notices = [("Duyurular alınamadı.", nil)]
            case .failure(let error):
                self.notices = [("Duyurular alınamadı.", error.localizedDescription)]
            }
            self.tableView.reloadData()
        }
    }

    override func numberOfSections(in tableView: UITableView) -> Int { 3 }

    override func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        ["Şehir kapsamı", "Veri kaynakları", "Duyurular"][section]
    }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        switch section {
        case 0: return links[city.id] == nil ? 1 : 2
        case 1: return max(1, sources.count)
        default: return notices.count + 1
        }
    }

    override func tableView(_ tableView: UITableView, cellForRowAt path: IndexPath) -> UITableViewCell {
        switch path.section {
        case 0:
            if path.row == 1 { return cell("Resmî işletmeci sitesini aç", link: true) }
            let bus = city.id == 0 ? "Ankara canlı varış kaynağı · EGO Cep’te" :
                city.hasLiveService ? "Canlı sorgu kapsamı işletmeciye bağlı." : "Canlı varış servisi bağlı değil."
            return cell("\(counts.routes) hat/yön · \(counts.stops) durak", bus)
        case 1:
            guard !sources.isEmpty else {
                return cell(city.provider == "asis" ? "Bölgesel ASİS kataloğu" : "Ek kaynak kaydı yok.")
            }
            let source = sources[path.row]
            return cell(source.name, source.license, link: source.url != nil)
        default:
            if path.row == 0 { return cell("Metro İstanbul duyurularını getir", link: true) }
            let notice = notices[path.row - 1]
            return cell(notice.0, notice.1)
        }
    }

    override func tableView(_ tableView: UITableView, didSelectRowAt path: IndexPath) {
        tableView.deselectRow(at: path, animated: true)
        let url: URL?
        switch (path.section, path.row) {
        case (0, 1): url = links[city.id].flatMap(URL.init(string:))
        case (1, _): url = sources.indices.contains(path.row) ? sources[path.row].url : nil
        case (2, 0): fetchNotices(); return
        default: return
        }
        if let url = url { UIApplication.shared.open(url) }
    }
}
