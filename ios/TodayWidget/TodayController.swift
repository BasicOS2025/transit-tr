import UIKit
import NotificationCenter

final class TodayController: UIViewController, NCWidgetProviding {
    private let titleLabel = UILabel()
    private let details = UIButton(type: .system)
    private let updatedLabel = UILabel()
    private var previous: WidgetSnapshot?
    private var previousText: String?

    override func viewDidLoad() {
        super.viewDidLoad()
        let stack = UIStackView(arrangedSubviews: [titleLabel, details, updatedLabel])
        stack.axis = .vertical
        stack.spacing = 2
        stack.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(stack)
        NSLayoutConstraint.activate([
            stack.leadingAnchor.constraint(equalTo: view.layoutMarginsGuide.leadingAnchor),
            stack.trailingAnchor.constraint(equalTo: view.layoutMarginsGuide.trailingAnchor),
            stack.centerYAnchor.constraint(equalTo: view.centerYAnchor)
        ])
        titleLabel.font = .preferredFont(forTextStyle: .headline)
        titleLabel.numberOfLines = 1
        updatedLabel.font = .preferredFont(forTextStyle: .caption1)
        details.titleLabel?.numberOfLines = 2
        details.titleLabel?.font = .preferredFont(forTextStyle: .subheadline)
        details.contentHorizontalAlignment = .leading
        details.heightAnchor.constraint(greaterThanOrEqualToConstant: 44).isActive = true
        details.addTarget(self, action: #selector(openApp), for: .touchUpInside)
        render(WidgetSnapshot.read())
    }

    private func render(_ snapshot: WidgetSnapshot?) {
        titleLabel.text = snapshot.map { $0.stop + " · " + $0.name } ?? "Transit TR"
        let text = snapshot?.display(at: Date()) ?? "Uygulamadan bir durak sorgusu sabitle."
        details.setTitle(text, for: .normal)
        updatedLabel.text = snapshot?.timeLabel
        previous = snapshot
        previousText = text
    }

    func widgetPerformUpdate(completionHandler: @escaping (NCUpdateResult) -> Void) {
        let snapshot = WidgetSnapshot.read()
        let text = snapshot?.display(at: Date()) ?? "Uygulamadan bir durak sorgusu sabitle."
        let changed = snapshot != previous || text != previousText
        render(snapshot)
        completionHandler(changed ? .newData : .noData)
    }

    @objc private func openApp() {
        extensionContext?.open(WidgetSnapshot.url, completionHandler: nil)
    }
}
