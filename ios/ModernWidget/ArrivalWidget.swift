import SwiftUI
import WidgetKit

private struct Entry: TimelineEntry {
    let date: Date
    let snapshot: WidgetSnapshot?
}

private struct Provider: TimelineProvider {
    func placeholder(in context: Context) -> Entry { Entry(date: Date(), snapshot: nil) }

    func getSnapshot(in context: Context, completion: @escaping (Entry) -> Void) {
        completion(Entry(date: Date(), snapshot: WidgetSnapshot.read()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<Entry>) -> Void) {
        let now = Date()
        let snapshot = WidgetSnapshot.read()
        var entries = [Entry(date: now, snapshot: snapshot)]
        if let expiry = snapshot?.expiry, expiry > now {
            entries.append(Entry(date: expiry, snapshot: snapshot))
        }
        completion(Timeline(entries: entries, policy: .never))
    }
}

private struct ArrivalView: View {
    let entry: Entry

    private var content: some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(entry.snapshot.map { $0.stop + " · " + $0.name } ?? "Transit TR")
                .font(.headline).lineLimit(2)
            Text(entry.snapshot?.display(at: entry.date) ?? "Uygulamadan bir durak sorgusu sabitle.")
                .font(.subheadline)
            if let time = entry.snapshot?.timeLabel {
                Text(time).font(.caption2).foregroundColor(.secondary)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .widgetURL(WidgetSnapshot.url)
    }

    var body: some View {
        #if compiler(>=5.9)
        if #available(iOSApplicationExtension 17.0, *) {
            content.containerBackground(for: .widget) { Color(.systemBackground) }
        } else {
            content.padding()
        }
        #else
        content.padding()
        #endif
    }
}

@main
struct ArrivalWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "TransitTR.Arrivals", provider: Provider()) { entry in
            ArrivalView(entry: entry)
        }
        .configurationDisplayName("Transit TR Varışlar")
        .description("Sabitlediğin durak için son varış sorgusu.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}
