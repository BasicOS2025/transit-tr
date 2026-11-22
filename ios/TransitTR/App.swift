import UIKit
import MapKit

@UIApplicationMain
final class AppDelegate: UIResponder, UIApplicationDelegate {
    var window: UIWindow?

    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions options: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        let state = AppState()
        let live = LiveController(state)
        let catalog = CatalogController(state)
        let rail = RailController(state)
        let tabs = UITabBarController()
        tabs.viewControllers = [
            tab(live, "Otobüs", "bus"),
            tab(catalog, "Katalog", "catalog"),
            tab(rail, "Raylı Sistem", "rail"),
            tab(AssistantController(state), "Asistan", "assistant"),
            tab(ServicesController(state), "Servisler", "services")
        ]
        state.openLive = { [weak tabs, weak live] entry in
            tabs?.selectedIndex = 0
            live?.navigationController?.popToRootViewController(animated: false)
            live?.selectStop(entry)
        }
        window = UIWindow(frame: UIScreen.main.bounds)
        window?.tintColor = UIColor(red: 0.08, green: 0.36, blue: 0.75, alpha: 1)
        window?.rootViewController = tabs
        window?.makeKeyAndVisible()
        return true
    }

    private func tab(_ controller: UIViewController, _ title: String, _ icon: String) -> UINavigationController {
        let navigation = UINavigationController(rootViewController: controller)
        navigation.navigationBar.prefersLargeTitles = true
        navigation.tabBarItem = UITabBarItem(title: title, image: tabIcon(icon), selectedImage: nil)
        return navigation
    }

    // Small template images work on iOS versions before SF Symbols.
    private func tabIcon(_ icon: String) -> UIImage {
        UIGraphicsImageRenderer(size: CGSize(width: 24, height: 24)).image { _ in
            UIColor.black.setStroke()
            UIColor.black.setFill()
            if icon == "catalog" {
                for y in [5, 11, 17] {
                    UIBezierPath(ovalIn: CGRect(x: 2, y: y, width: 3, height: 3)).fill()
                    UIBezierPath(roundedRect: CGRect(x: 8, y: y, width: 14, height: 2), cornerRadius: 1).fill()
                }
                return
            }
            if icon == "assistant" || icon == "services" {
                let path = UIBezierPath(roundedRect: CGRect(x: 3, y: 3, width: 18, height: 18), cornerRadius: 4)
                path.lineWidth = 2
                path.stroke()
                let mark = icon == "assistant" ? "?" : "i"
                let style: [NSAttributedString.Key: Any] = [.font: UIFont.boldSystemFont(ofSize: 16),
                                                            .foregroundColor: UIColor.black]
                let size = mark.size(withAttributes: style)
                mark.draw(at: CGPoint(x: (24 - size.width) / 2, y: (24 - size.height) / 2),
                          withAttributes: style)
                return
            }
            let body = UIBezierPath(roundedRect: CGRect(x: 4, y: 3, width: 16, height: 16), cornerRadius: 3)
            body.lineWidth = 2
            body.stroke()
            UIBezierPath(rect: CGRect(x: 5, y: 10, width: 14, height: 2)).fill()
            if icon == "rail" {
                let track = UIBezierPath()
                track.move(to: CGPoint(x: 7, y: 22))
                track.addLine(to: CGPoint(x: 10, y: 18))
                track.move(to: CGPoint(x: 17, y: 22))
                track.addLine(to: CGPoint(x: 14, y: 18))
                track.lineWidth = 2
                track.stroke()
            } else {
                for x in [5, 16] {
                    UIBezierPath(roundedRect: CGRect(x: x, y: 18, width: 3, height: 4), cornerRadius: 1).fill()
                }
            }
        }.withRenderingMode(.alwaysTemplate)
    }
}

final class AppState {
    static let cityChanged = Notification.Name("TransitTR.cityChanged")
    let catalog = Catalog(path: Bundle.main.path(forResource: "TransitCatalog", ofType: "sqlite"))
    let api: TransitAPI
    var openLive: ((Entry) -> Void)?
    private let defaults = UserDefaults.standard
    private(set) var city: City
    private(set) var cities: [City] = []
    private(set) var favorites: [Entry]

    init() {
        city = City(id: 0, name: "Ankara", provider: "ego")
        let base = (Bundle.main.object(forInfoDictionaryKey: "TransitAPIBaseURL") as? String)
            .flatMap(URL.init(string:))
            .flatMap { $0.scheme == "https" && $0.host != nil ? $0 : nil }
        api = TransitAPI(base: base ?? URL(string: "https://transit-tr.vercel.app")!)
        favorites = defaults.data(forKey: "catalogFavorites")
            .flatMap { try? JSONDecoder().decode([Entry].self, from: $0) } ?? []
        let savedCity = defaults.integer(forKey: "city")
        catalog.read({ try $0.cities() }) { [weak self] result in
            guard let self = self, case .success(let cities) = result else { return }
            self.cities = cities
            self.setCity(cities.first { $0.id == savedCity } ?? self.city)
        }
    }

    func setCity(_ city: City) {
        self.city = city
        defaults.set(city.id, forKey: "city")
        NotificationCenter.default.post(name: Self.cityChanged, object: self)
    }

    func isFavorite(_ entry: Entry) -> Bool {
        favorites.contains { $0.matches(entry) }
    }

    func toggleFavorite(_ entry: Entry) -> Bool {
        if let index = favorites.firstIndex(where: { $0.matches(entry) }) {
            favorites.remove(at: index)
        } else {
            guard favorites.count < 200 else { return false }
            favorites.insert(entry, at: 0)
        }
        defaults.set(try? JSONEncoder().encode(favorites), forKey: "catalogFavorites")
        return true
    }
}

class TransitScreen: UITableViewController {
    let state: AppState
    var city: City { state.city }

    init(_ state: AppState) {
        self.state = state
        super.init(style: .grouped)
    }

    required init?(coder: NSCoder) { fatalError("Use init(_:)") }

    override func viewDidLoad() {
        super.viewDidLoad()
        tableView.rowHeight = UITableView.automaticDimension
        tableView.estimatedRowHeight = 58
        tableView.keyboardDismissMode = .onDrag
        navigationItem.rightBarButtonItem = UIBarButtonItem(title: city.name, style: .plain,
                                                             target: self, action: #selector(chooseCity))
        NotificationCenter.default.addObserver(self, selector: #selector(cityChanged),
                                               name: AppState.cityChanged, object: state)
    }

    @objc func cityChanged() { navigationItem.rightBarButtonItem?.title = city.name }

    @objc private func chooseCity() {
        let list = CityController(state)
        let navigation = UINavigationController(rootViewController: list)
        navigation.modalPresentationStyle = .formSheet
        present(navigation, animated: true)
    }

    func cell(_ title: String, _ subtitle: String? = nil, link: Bool = false) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "text")
            ?? UITableViewCell(style: .subtitle, reuseIdentifier: "text")
        cell.textLabel?.text = title
        cell.textLabel?.numberOfLines = 0
        cell.textLabel?.font = .preferredFont(forTextStyle: .body)
        cell.textLabel?.adjustsFontForContentSizeCategory = true
        cell.detailTextLabel?.text = subtitle
        cell.detailTextLabel?.numberOfLines = 0
        cell.detailTextLabel?.font = .preferredFont(forTextStyle: .subheadline)
        cell.detailTextLabel?.adjustsFontForContentSizeCategory = true
        cell.accessoryType = link ? .disclosureIndicator : .none
        cell.selectionStyle = link ? .default : .none
        return cell
    }

    func codedCell(_ code: String, _ title: String, _ detail: String? = nil, color: UIColor? = nil) -> UITableViewCell {
        let cell = cell(title, detail, link: true)
        guard !code.isEmpty else { return cell }
        let font = UIFont.preferredFont(forTextStyle: .body)
        let text = NSMutableAttributedString(string: code + "   ", attributes: [
            .font: UIFont.monospacedDigitSystemFont(ofSize: font.pointSize, weight: .semibold),
            .foregroundColor: color ?? view.tintColor ?? UIColor.blue
        ])
        text.append(NSAttributedString(string: title))
        cell.textLabel?.attributedText = text
        return cell
    }

    func message(_ title: String, _ text: String) {
        let alert = UIAlertController(title: title, message: text, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "Tamam", style: .default))
        present(alert, animated: true)
    }

    func showMap(_ title: String, pins: [MapPin], parts: [[Point]] = [], caption: String? = nil, color: UIColor? = nil) {
        guard !pins.isEmpty || parts.contains(where: { $0.count > 1 }) else { return }
        navigationController?.pushViewController(MapController(title, pins: pins, parts: parts, caption: caption, color: color),
                                                 animated: true)
    }

    static func date(_ date: Date, includeDay: Bool) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "tr_TR")
        formatter.timeZone = Catalog.timeZone
        formatter.dateFormat = includeDay ? "d MMM yyyy HH:mm" : "HH:mm"
        return formatter.string(from: date)
    }

    func button(_ title: String, action: Selector) -> UIButton {
        let button = UIButton(type: .system)
        button.setTitle(title, for: .normal)
        button.titleLabel?.font = .preferredFont(forTextStyle: .headline)
        button.titleLabel?.adjustsFontForContentSizeCategory = true
        button.titleLabel?.numberOfLines = 0
        button.addTarget(self, action: action, for: .touchUpInside)
        button.heightAnchor.constraint(greaterThanOrEqualToConstant: 44).isActive = true
        return button
    }

    func header(_ elements: [UIView]) {
        let container = UIView()
        let stack = UIStackView(arrangedSubviews: elements)
        stack.axis = .vertical
        stack.spacing = 8
        stack.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(stack)
        NSLayoutConstraint.activate([
            stack.leadingAnchor.constraint(equalTo: container.layoutMarginsGuide.leadingAnchor),
            stack.trailingAnchor.constraint(equalTo: container.layoutMarginsGuide.trailingAnchor),
            stack.topAnchor.constraint(equalTo: container.topAnchor, constant: 8),
            stack.bottomAnchor.constraint(equalTo: container.bottomAnchor, constant: -8)
        ])
        tableView.tableHeaderView = container
        fitHeader()
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        fitHeader()
    }

    private func fitHeader() {
        guard let header = tableView.tableHeaderView, tableView.bounds.width > 0 else { return }
        let width = tableView.bounds.width
        header.frame.size.width = width
        let target = CGSize(width: width, height: UIView.layoutFittingCompressedSize.height)
        let height = ceil(header.systemLayoutSizeFitting(target, withHorizontalFittingPriority: .required,
                                                         verticalFittingPriority: .fittingSizeLevel).height)
        if abs(header.frame.height - height) >= 1 {
            header.frame.size.height = height
            tableView.tableHeaderView = header
        }
    }
}

private final class CityController: TransitScreen, UISearchResultsUpdating {
    private var query = ""
    private var matches: [City] {
        query.isEmpty ? state.cities : state.cities.filter { Catalog.normalize($0.name).contains(query) }
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "Şehir seç"
        navigationItem.rightBarButtonItem = UIBarButtonItem(barButtonSystemItem: .cancel, target: self,
                                                             action: #selector(close))
        let search = UISearchController(searchResultsController: nil)
        search.searchResultsUpdater = self
        search.obscuresBackgroundDuringPresentation = false
        search.searchBar.placeholder = "Şehir veya işletme"
        navigationItem.searchController = search
        navigationItem.hidesSearchBarWhenScrolling = false
        definesPresentationContext = true
    }

    override func cityChanged() { tableView.reloadData() }

    func updateSearchResults(for searchController: UISearchController) {
        query = Catalog.normalize(searchController.searchBar.text ?? "")
        tableView.reloadData()
    }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int { matches.count }

    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let item = matches[indexPath.row]
        let row = cell(item.name, link: true)
        row.accessoryType = item.id == city.id ? .checkmark : .none
        return row
    }

    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        state.setCity(matches[indexPath.row])
        close()
    }

    @objc private func close() {
        navigationItem.searchController?.isActive = false
        dismiss(animated: true)
    }
}

struct MapPin {
    let point: Point
    let title: String
    let subtitle: String?
    let vehicle: Bool
}

private final class MapController: UIViewController, MKMapViewDelegate {
    private let pins: [MapPin]
    private let parts: [[Point]]
    private let caption: String?
    private let color: UIColor?

    init(_ title: String, pins: [MapPin], parts: [[Point]], caption: String?, color: UIColor?) {
        self.pins = pins
        self.parts = parts
        self.caption = caption
        self.color = color
        super.init(nibName: nil, bundle: nil)
        self.title = title
        navigationItem.largeTitleDisplayMode = .never
    }

    required init?(coder: NSCoder) { fatalError("Use init(_:pins:parts:caption:)") }

    override func viewDidLoad() {
        super.viewDidLoad()
        let map = MKMapView()
        map.delegate = self
        map.showsPointsOfInterest = false
        map.addOverlay(StreetTiles(), level: .aboveLabels)
        let stack = UIStackView()
        stack.axis = .vertical
        stack.translatesAutoresizingMaskIntoConstraints = false
        if #available(iOS 13.0, *) { view.backgroundColor = .systemBackground }
        else { view.backgroundColor = .white }
        view.addSubview(stack)
        let guide = view.safeAreaLayoutGuide
        NSLayoutConstraint.activate([
            stack.leadingAnchor.constraint(equalTo: guide.leadingAnchor),
            stack.trailingAnchor.constraint(equalTo: guide.trailingAnchor),
            stack.topAnchor.constraint(equalTo: guide.topAnchor),
            stack.bottomAnchor.constraint(equalTo: guide.bottomAnchor)
        ])
        if let caption = caption {
            let label = UILabel()
            label.text = caption
            label.textAlignment = .center
            label.numberOfLines = 0
            label.font = .preferredFont(forTextStyle: .footnote)
            label.adjustsFontForContentSizeCategory = true
            stack.addArrangedSubview(label)
        }
        stack.addArrangedSubview(map)
        let attribution = UIButton(type: .system)
        attribution.setTitle("© OpenStreetMap contributors", for: .normal)
        attribution.titleLabel?.font = .preferredFont(forTextStyle: .footnote)
        attribution.addTarget(self, action: #selector(openAttribution), for: .touchUpInside)
        attribution.heightAnchor.constraint(greaterThanOrEqualToConstant: 44).isActive = true
        stack.addArrangedSubview(attribution)

        var bounds = MKMapRect.null
        for part in parts {
            let coordinates = part.filter { $0.valid }
                .map { CLLocationCoordinate2D(latitude: $0.lat, longitude: $0.lon) }
            guard coordinates.count > 1 else { continue }
            let path = MKPolyline(coordinates: coordinates, count: coordinates.count)
            bounds = bounds.union(path.boundingMapRect)
            map.addOverlay(path, level: .aboveLabels)
        }
        for item in pins where item.point.valid {
            let pin = PinAnnotation(vehicle: item.vehicle)
            pin.coordinate = CLLocationCoordinate2D(latitude: item.point.lat, longitude: item.point.lon)
            pin.title = item.title
            pin.subtitle = item.subtitle
            map.addAnnotation(pin)
            let point = MKMapPoint(pin.coordinate)
            bounds = bounds.union(MKMapRect(x: point.x - 50, y: point.y - 50, width: 100, height: 100))
        }
        if !bounds.isNull {
            view.layoutIfNeeded()
            map.setVisibleMapRect(bounds.insetBy(dx: -1000, dy: -1000),
                                  edgePadding: UIEdgeInsets(top: 24, left: 24, bottom: 36, right: 24),
                                  animated: false)
        }
    }

    @objc private func openAttribution() {
        if let url = URL(string: "https://www.openstreetmap.org/copyright") {
            UIApplication.shared.open(url)
        }
    }

    func mapView(_ mapView: MKMapView, rendererFor overlay: MKOverlay) -> MKOverlayRenderer {
        if let tiles = overlay as? MKTileOverlay { return MKTileOverlayRenderer(tileOverlay: tiles) }
        guard let path = overlay as? MKPolyline else { return MKOverlayRenderer(overlay: overlay) }
        let renderer = MKPolylineRenderer(polyline: path)
        renderer.strokeColor = color ?? view.tintColor
        renderer.lineWidth = 4
        return renderer
    }

    func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
        guard let pin = annotation as? PinAnnotation else { return nil }
        let identifier = pin.vehicle ? "vehicle" : "stop"
        let marker = mapView.dequeueReusableAnnotationView(withIdentifier: identifier) as? MKMarkerAnnotationView
            ?? MKMarkerAnnotationView(annotation: pin, reuseIdentifier: identifier)
        marker.annotation = pin
        marker.markerTintColor = pin.vehicle ? view.tintColor : .darkGray
        marker.canShowCallout = true
        return marker
    }
}

private final class PinAnnotation: MKPointAnnotation {
    let vehicle: Bool

    init(vehicle: Bool) {
        self.vehicle = vehicle
        super.init()
    }
}

private final class StreetTiles: MKTileOverlay {
    private static let session: URLSession = {
        let config = URLSessionConfiguration.default
        config.httpAdditionalHeaders = [
            "User-Agent": "TransitTR-iOS/1.0 (+https://github.com/BasicOS2025/transit-tr)"
        ]
        config.urlCache = URLCache(memoryCapacity: 1_048_576, diskCapacity: 16_777_216,
                                   diskPath: "TransitOpenStreetMap")
        config.requestCachePolicy = .useProtocolCachePolicy
        config.timeoutIntervalForRequest = 12
        return URLSession(configuration: config)
    }()

    init() {
        super.init(urlTemplate: "https://tile.openstreetmap.org/{z}/{x}/{y}.png")
        canReplaceMapContent = true
        maximumZ = 19
    }

    override func loadTile(at path: MKTileOverlayPath, result: @escaping (Data?, Error?) -> Void) {
        Self.session.dataTask(with: url(forTilePath: path)) { data, response, error in
            guard let response = response as? HTTPURLResponse, response.statusCode == 200 else {
                result(nil, error ?? TransitError.response)
                return
            }
            result(data, error)
        }.resume()
    }
}
