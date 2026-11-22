using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.Globalization;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;
using System.Web.Script.Serialization;
using System.Windows.Forms;

namespace TransitTR.Windows
{
    internal static class Program
    {
        [STAThread]
        private static int Main(string[] args)
        {
            if (args.SequenceEqual(new[] { "--verify-catalog" })) return VerifyCatalogue();
            bool testWindow = args.SequenceEqual(new[] { "--verify-window" });
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            if (Environment.OSVersion.Version.Major == 6 && Environment.OSVersion.Version.Minor == 1)
                ServicePointManager.SecurityProtocol = SecurityProtocolType.Tls12;
            try
            {
                using (var form = new TransitForm())
                {
                    if (testWindow)
                    {
                        form.CreateControl();
                        form.DrawToBitmapForTest();
                    }
                    else Application.Run(form);
                }
                return 0;
            }
            catch (IOException ex)
            {
                MessageBox.Show(ex.Message, "Transit TR", MessageBoxButtons.OK, MessageBoxIcon.Error);
                return 1;
            }
        }

        private static int VerifyCatalogue()
        {
            var cities = TransitData.Read<List<City>>("cities.json.gz");
            int routes = 0, stops = 0;
            foreach (var city in cities)
            {
                var catalogue = TransitData.Read<Catalogue>(city.id + ".json.gz");
                routes += catalogue.routes.Count;
                stops += catalogue.stops.Count;
                if (city.id == 0 && (!catalogue.stops.Any(stop => stop.code == "10940") ||
                    !catalogue.schedules.ContainsKey("481"))) return 2;
            }
            return cities.Count == 41 && routes > 9500 && stops > 70000 &&
                TransitData.Normalize("İŞIK ÇAĞRI") == "isik cagri" ? 0 : 2;
        }
    }

    public sealed class City
    {
        public int id { get; set; }
        public string name { get; set; }
        public string provider { get; set; }
        public override string ToString() { return name; }
    }

    public sealed class Entry : MapPoint
    {
        public long id { get; set; }
        public string code { get; set; }
        public string name { get; set; }
        public string type { get; set; }
        public override string ToString() { return code + "  ·  " + name; }
    }

    public sealed class Schedule
    {
        public string[][] days { get; set; }
    }

    public sealed class Catalogue
    {
        public List<Entry> routes { get; set; }
        public List<Entry> stops { get; set; }
        public Dictionary<string, List<long>> links { get; set; }
        public Dictionary<string, Schedule> schedules { get; set; }
    }

    public sealed class Arrival
    {
        public string line { get; set; }
        public string name { get; set; }
        public string eta { get; set; }
        public int etaSeconds { get; set; } = -1;
        public bool passed { get; set; }
    }

    public sealed class ArrivalResult
    {
        public string source { get; set; }
        public long receivedAt { get; set; }
        public string message { get; set; }
        public List<Arrival> rows { get; set; }
        public List<VehiclePoint> mapRows { get; set; }
        public MapPoint stop { get; set; }
    }

    internal static class TransitData
    {
        internal static T Read<T>(string file)
        {
            var path = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "data", file);
            using (var input = File.OpenRead(path))
            using (var gzip = new GZipStream(input, CompressionMode.Decompress))
            using (var reader = new StreamReader(gzip, Encoding.UTF8))
                return Parse<T>(reader.ReadToEnd());
        }

        internal static T Parse<T>(string json)
        {
            return new JavaScriptSerializer { MaxJsonLength = 12 * 1024 * 1024 }.Deserialize<T>(json);
        }

        internal static string Normalize(string value)
        {
            return new string(value.ToLowerInvariant().Replace('ı', 'i').Replace('İ', 'i')
                .Normalize(NormalizationForm.FormD)
                .Where(x => CharUnicodeInfo.GetUnicodeCategory(x) != UnicodeCategory.NonSpacingMark).ToArray());
        }
    }

    internal sealed class TransitForm : Form
    {
        private static readonly HttpClient Client = new HttpClient(new HttpClientHandler {
            AllowAutoRedirect = false
        }) { Timeout = TimeSpan.FromSeconds(25) };
        private readonly ComboBox city = new ComboBox { DropDownStyle = ComboBoxStyle.DropDownList, Width = 220 };
        private readonly TabControl tabs = new TabControl { Dock = DockStyle.Fill };
        private readonly ComboBox kind = new ComboBox { DropDownStyle = ComboBoxStyle.DropDownList, Width = 135 };
        private readonly TextBox search = new TextBox { Width = 270, AccessibleName = "Hat veya durak ara" };
        private readonly ListBox entries = new ListBox { Dock = DockStyle.Fill, IntegralHeight = false };
        private readonly TextBox detail = new TextBox { Dock = DockStyle.Fill, Multiline = true, ReadOnly = true, ScrollBars = ScrollBars.Vertical };
        private readonly Button next = new Button { Text = "Daha fazla", AutoSize = true };
        private readonly Button track = new Button { Text = "Varışları sorgula", AutoSize = true };
        private readonly Button maps = new Button { Text = "Haritada göster", AutoSize = true };
        private readonly Button goLive = new Button { Text = "Durak varışlarına git", AutoSize = true };
        private readonly Label summary = new Label { AutoSize = true };
        private readonly Label liveStatus = new Label { AutoSize = true, MaximumSize = new Size(740, 0) };
        private readonly TextBox stopCode = new TextBox { Width = 110, AccessibleName = "Durak kodu", MaxLength = 8 };
        private readonly TextBox lineCode = new TextBox { Width = 130, AccessibleName = "Hat kodu", MaxLength = 80 };
        private readonly ListView arrivals = new ListView { Dock = DockStyle.Fill, View = View.Details, FullRowSelect = true };
        private readonly System.Windows.Forms.Timer searchDelay = new System.Windows.Forms.Timer { Interval = 180 };
        private static readonly Color Ink = Color.FromArgb(18, 42, 67);
        private static readonly Color Accent = Color.FromArgb(0, 101, 173);
        private static readonly Color Surface = Color.FromArgb(245, 248, 251);
        private readonly TransitMap map = new TransitMap();
        private readonly TabPage mapPage = new TabPage("Harita");
        private readonly Label mapCaption = new Label { AutoSize = true };
        private readonly Button liveMap = new Button { Text = "Araçları haritada göster", AutoSize = true, Enabled = false };
        private readonly CheckBox alerts = new CheckBox { Text = "5 dk kala bildir", AutoSize = true };
        private readonly NotifyIcon tray = new NotifyIcon { Icon = SystemIcons.Information, Text = "Transit TR" };
        private readonly System.Windows.Forms.Timer liveTimer = new System.Windows.Forms.Timer();
        private bool notified;
        private long lastSnapshot;
        private string trackedQuery;
        private ArrivalResult latest;
        private Catalogue data;
        private List<Entry> matches = new List<Entry>();
        private CancellationTokenSource pending;
        private int revision;

        internal TransitForm()
        {
            Text = "Transit TR";
            MinimumSize = new Size(670, 440);
            ClientSize = new Size(960, 650);
            StartPosition = FormStartPosition.CenterScreen;
            Font = new Font("Segoe UI", 10);
            AutoScaleMode = AutoScaleMode.Dpi;
            BackColor = Surface;
            ForeColor = Ink;
            entries.BorderStyle = BorderStyle.None;
            entries.DrawMode = DrawMode.OwnerDrawFixed;
            entries.ItemHeight = 32;
            entries.DrawItem += DrawEntry;
            detail.BorderStyle = BorderStyle.None;
            detail.BackColor = Color.White;
            arrivals.BorderStyle = BorderStyle.None;
            tray.BalloonTipClicked += (sender, args) => { Show(); WindowState = FormWindowState.Normal; Activate(); };
            liveTimer.Tick += async (sender, args) => { liveTimer.Stop(); await QueryAsync(false); };
            alerts.CheckedChanged += (sender, args) =>
            {
                tray.Visible = alerts.Checked;
                if (!alerts.Checked) liveTimer.Stop();
                else if (latest != null && trackedQuery != null)
                {
                    MaybeNotify(latest);
                    ScheduleRefresh();
                }
            };
            stopCode.TextChanged += (sender, args) => ResetTracking();
            lineCode.TextChanged += (sender, args) => ResetTracking();

            var title = new Label { Text = "Transit TR", AutoSize = true, Font = new Font(Font.FontFamily, 20, FontStyle.Bold) };
            var header = Bar(title, new Label { Text = "Şehir", AutoSize = true, Margin = new Padding(22, 10, 6, 0) }, city);
            header.BackColor = Surface;
            Controls.Add(tabs);
            Controls.Add(header);
            MakeCatalogueTab();
            MakeLiveTab();
            MakeMapTab();
            StyleAction(track, true);
            StyleAction(maps);
            StyleAction(goLive);
            StyleAction(next);
            StyleAction(liveMap);

            city.DataSource = TransitData.Read<List<City>>("cities.json.gz");
            city.SelectedIndexChanged += async (sender, args) => await ChangeCityAsync();
            search.TextChanged += (sender, args) => { searchDelay.Stop(); searchDelay.Start(); };
            searchDelay.Tick += (sender, args) => { searchDelay.Stop(); Filter(); };
            Shown += async (sender, args) => await ChangeCityAsync();
            FormClosed += (sender, args) => { pending?.Cancel(); searchDelay.Dispose(); liveTimer.Dispose(); tray.Dispose(); };
        }

        internal void DrawToBitmapForTest()
        {
            using (var bitmap = new Bitmap(Width, Height))
            {
                DrawToBitmap(bitmap, new Rectangle(Point.Empty, Size));
            }
        }

        private static FlowLayoutPanel Bar(params Control[] controls)
        {
            var bar = new FlowLayoutPanel { Dock = DockStyle.Top, AutoSize = true, WrapContents = true,
                Padding = new Padding(12, 8, 12, 8) };
            bar.Controls.AddRange(controls);
            return bar;
        }

        private void MakeCatalogueTab()
        {
            var page = new TabPage("Katalog") { BackColor = SystemColors.Window };
            var split = new SplitContainer { Dock = DockStyle.Fill, Size = new Size(900, 500), SplitterDistance = 346, FixedPanel = FixedPanel.Panel1 };
            var actions = Bar(maps, goLive);
            split.Panel1.Controls.Add(entries);
            split.Panel2.Controls.Add(detail);
            split.Panel2.Controls.Add(actions);
            kind.Items.AddRange(new object[] { "Hatlar", "Duraklar" });
            kind.SelectedIndex = 0;
            kind.SelectedIndexChanged += (sender, args) => Filter();
            entries.SelectedIndexChanged += (sender, args) => ShowDetails();
            next.Click += (sender, args) => AppendPage();
            maps.Click += (sender, args) => ShowEntryMap();
            goLive.Click += async (sender, args) =>
            {
                var item = entries.SelectedItem as Entry;
                if (item == null) return;
                stopCode.Text = item.code;
                lineCode.Clear();
                tabs.SelectedIndex = 1;
                await QueryAsync();
            };
            page.Controls.Add(split);
            page.Controls.Add(Bar(kind, new Label { Text = "Ara", AutoSize = true }, search, next, summary));
            tabs.TabPages.Add(page);
        }

        private void MakeLiveTab()
        {
            var page = new TabPage("Canlı varış") { BackColor = SystemColors.Window };
            arrivals.Columns.Add("Hat", 100);
            arrivals.Columns.Add("Varış", 120, HorizontalAlignment.Right);
            arrivals.Columns.Add("Güzergâh", 490);
            var statusBar = Bar(liveStatus);
            statusBar.Dock = DockStyle.Bottom;
            track.Click += async (sender, args) => await QueryAsync();
            liveMap.Click += (sender, args) =>
            {
                if (latest != null) OpenMap(latest.stop, latest.mapRows, null, "Durak ve kaynak GPS kayıtları");
            };
            page.Controls.Add(arrivals);
            page.Controls.Add(statusBar);
            page.Controls.Add(Bar(new Label { Text = "Durak kodu", AutoSize = true }, stopCode,
                new Label { Text = "Hat (isteğe bağlı)", AutoSize = true }, lineCode, track, alerts, liveMap));
            tabs.TabPages.Add(page);
        }

        private async Task ChangeCityAsync()
        {
            ResetTracking();
            map.ClearData();
            arrivals.Items.Clear();
            liveStatus.Text = "";
            var current = city.SelectedItem as City;
            track.Enabled = current != null && current.provider != "static";
            if (current == null) return;
            var ticket = ++revision;
            data = null;
            Filter();
            summary.Text = "Katalog yükleniyor…";
            try
            {
                var loaded = await Task.Run(() => TransitData.Read<Catalogue>(current.id + ".json.gz"));
                if (ticket != revision || IsDisposed) return;
                data = loaded;
                Filter();
            }
            catch (Exception ex) when (ex is IOException || ex is InvalidOperationException || ex is ArgumentException)
            {
                if (ticket == revision && !IsDisposed) summary.Text = "Katalog okunamadı. Paket klasörünü kontrol et.";
            }
        }

        private void Filter()
        {
            var source = data == null ? Enumerable.Empty<Entry>() : (IEnumerable<Entry>)(kind.SelectedIndex == 0 ? data.routes : data.stops);
            string term = TransitData.Normalize(search.Text.Trim());
            matches = source.Where(row => term.Length == 0 || TransitData.Normalize(row.ToString()).Contains(term))
                .OrderByDescending(row => term.Length > 0 && TransitData.Normalize(row.code) == term).ToList();
            entries.Items.Clear();
            AppendPage();
            summary.Text = matches.Count.ToString("N0", CultureInfo.GetCultureInfo("tr-TR")) + " yerel kayıt";
            ShowDetails();
        }

        private void AppendPage()
        {
            entries.BeginUpdate();
            entries.Items.AddRange(matches.Skip(entries.Items.Count).Take(100).Cast<object>().ToArray());
            entries.EndUpdate();
            next.Enabled = matches.Count > entries.Items.Count;
        }

        private void ShowDetails()
        {
            var selected = entries.SelectedItem as Entry;
            var current = city.SelectedItem as City;
            bool isStop = kind.SelectedIndex == 1;
            maps.Enabled = selected != null && (isStop ? selected.Valid :
                data != null && (data.links.ContainsKey(selected.id.ToString(CultureInfo.InvariantCulture)) ||
                File.Exists(ShapeFile(selected.id))));
            goLive.Enabled = isStop && current != null && current.provider != "static" && selected != null &&
                Regex.IsMatch(selected.code, "^[0-9]{1,8}$");
            if (selected == null || data == null) { detail.Clear(); return; }
            var text = new StringBuilder(selected + Environment.NewLine);
            if (!isStop)
            {
                if (data.schedules.TryGetValue(selected.code, out Schedule schedule))
                {
                    string[] labels = { "Hafta içi", "Cumartesi", "Pazar" };
                    for (int i = 0; i < Math.Min(labels.Length, schedule.days.Length); i++)
                    {
                        text.AppendLine().AppendLine(labels[i] + " · EGO planlı saatleri");
                        foreach (var departure in schedule.days[i]) text.AppendLine(departure);
                    }
                }
                if (data.links.TryGetValue(selected.id.ToString(CultureInfo.InvariantCulture), out List<long> links))
                {
                    var stops = data.stops.ToDictionary(row => row.id);
                    text.AppendLine().AppendLine("Sıralı duraklar");
                    foreach (var id in links)
                        if (stops.TryGetValue(id, out Entry stop)) text.AppendLine(stop.ToString());
                }
            }
            detail.Text = text.ToString();
        }

        private static void StyleAction(Button button, bool primary = false)
        {
            button.FlatStyle = FlatStyle.Flat;
            button.Padding = new Padding(9, 3, 9, 3);
            button.FlatAppearance.BorderColor = primary ? Ink : Color.FromArgb(203, 216, 229);
            button.BackColor = primary ? Ink : Color.White;
            button.ForeColor = primary ? Color.White : Ink;
        }

        private void DrawEntry(object sender, DrawItemEventArgs e)
        {
            if (e.Index < 0 || e.Index >= entries.Items.Count) return;
            e.DrawBackground();
            var item = (Entry)entries.Items[e.Index];
            bool selected = (e.State & DrawItemState.Selected) != 0;
            var flags = TextFormatFlags.VerticalCenter | TextFormatFlags.EndEllipsis | TextFormatFlags.SingleLine;
            var code = new Rectangle(e.Bounds.X + 8, e.Bounds.Y, 84, e.Bounds.Height);
            var label = new Rectangle(code.Right + 5, e.Bounds.Y, Math.Max(0, e.Bounds.Width - 105), e.Bounds.Height);
            TextRenderer.DrawText(e.Graphics, item.code, e.Font, code, selected ? SystemColors.HighlightText : Accent, flags);
            TextRenderer.DrawText(e.Graphics, item.name, e.Font, label, selected ? SystemColors.HighlightText : Ink, flags);
            e.DrawFocusRectangle();
        }

        private void MakeMapTab()
        {
            var plus = new Button { Text = "+", AccessibleName = "Yakınlaştır", AutoSize = true };
            var minus = new Button { Text = "−", AccessibleName = "Uzaklaştır", AutoSize = true };
            var credit = new LinkLabel { Text = "© OpenStreetMap contributors", AutoSize = true };
            plus.Click += (sender, args) => map.ChangeZoom(1);
            minus.Click += (sender, args) => map.ChangeZoom(-1);
            credit.LinkClicked += (sender, args) =>
            {
                try { Process.Start(new ProcessStartInfo("https://www.openstreetmap.org/copyright") { UseShellExecute = true }); }
                catch (System.ComponentModel.Win32Exception ex) { MessageBox.Show(ex.Message, "Harita"); }
            };
            var attribution = Bar(credit);
            attribution.Dock = DockStyle.Bottom;
            mapPage.Controls.Add(map);
            mapPage.Controls.Add(attribution);
            mapPage.Controls.Add(Bar(plus, minus, mapCaption));
            tabs.TabPages.Add(mapPage);
        }

        private static string ShapeFile(long id)
        {
            return Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "data",
                "shape-" + id.ToString(CultureInfo.InvariantCulture) + ".json.gz");
        }

        private void ShowEntryMap()
        {
            var item = entries.SelectedItem as Entry;
            if (item == null || data == null) return;
            if (kind.SelectedIndex == 1)
            {
                if (item.Valid) OpenMap(item, null, null, item.ToString());
                return;
            }
            var file = ShapeFile(item.id);
            if (File.Exists(file))
            {
                try
                {
                    var shape = TransitData.Read<List<MapPoint>>(Path.GetFileName(file));
                    if (shape.Count >= 2) { OpenMap(null, null, shape, item.code + " · Kaynak güzergâhı"); return; }
                }
                catch (IOException) { }
            }
            if (data.links.TryGetValue(item.id.ToString(CultureInfo.InvariantCulture), out List<long> links))
            {
                var lookup = data.stops.ToDictionary(row => row.id);
                var route = links.Where(lookup.ContainsKey).Select(id => lookup[id]).Where(row => row.Valid).ToList();
                if (route.Count >= 2) { OpenMap(null, null, route, item.code + " · Durakların şematik bağlantısı"); return; }
            }
            MessageBox.Show("Bu hat için harita çizmeye yetecek koordinat bulunmuyor.", "Harita");
        }

        private void OpenMap(MapPoint stop, IEnumerable<VehiclePoint> vehicles, IEnumerable<MapPoint> shape, string caption)
        {
            mapCaption.Text = caption;
            tabs.SelectedTab = mapPage;
            map.ShowData(stop, vehicles, shape);
        }

        private void ResetTracking()
        {
            pending?.Cancel();
            liveTimer.Stop();
            trackedQuery = null;
            latest = null;
            notified = false;
            lastSnapshot = 0;
            liveMap.Enabled = false;
            arrivals.Items.Clear();
            liveStatus.Text = "";
        }

        private void ScheduleRefresh()
        {
            if (!alerts.Checked || trackedQuery == null) return;
            liveTimer.Interval = (city.SelectedItem as City)?.id == 1 ? 60000 : 30000;
            liveTimer.Start();
        }

        private void MaybeNotify(ArrivalResult result)
        {
            if (!alerts.Checked || notified || result.receivedAt == lastSnapshot) return;
            lastSnapshot = result.receivedAt;
            var age = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() - result.receivedAt;
            if (age < -30000 || age > 120000) return;
            var near = result.rows.FirstOrDefault(row => !row.passed && row.etaSeconds >= 0 && row.etaSeconds <= 300);
            if (near == null) return;
            notified = true;
            tray.ShowBalloonTip(6000, "Transit TR · Durak " + stopCode.Text.Trim(), near.line + " · " + near.eta, ToolTipIcon.Info);
        }

        private async Task QueryAsync(bool manual = true)
        {
            var current = city.SelectedItem as City;
            if (current == null || current.provider == "static") return;
            string stop = stopCode.Text.Trim(), line = lineCode.Text.Trim();
            if (!Regex.IsMatch(stop, current.id == 0 ? "^[0-9]{5}$" : "^[0-9]{1,8}$") ||
                !Regex.IsMatch(line, "^[A-Za-z0-9ÇĞİÖŞÜçğıöşü_ -]{0,80}$"))
            {
                liveStatus.Text = current.id == 0 ? "5 haneli durak numarası ve geçerli bir hat kodu gir."
                    : "Geçerli bir durak numarası ve hat kodu gir.";
                return;
            }
            string queryKey = current.id + ":" + stop + ":" + line.ToUpperInvariant();
            liveTimer.Stop();
            if (trackedQuery != queryKey) { notified = false; lastSnapshot = 0; }
            trackedQuery = queryKey;
            pending?.Cancel();
            var request = new CancellationTokenSource();
            pending = request;
            if (manual)
            {
                arrivals.Items.Clear();
                liveStatus.Text = "Varışlar alınıyor…";
                latest = null;
                liveMap.Enabled = false;
            }
            string url = "https://transit-tr.vercel.app/api/v2/live?city=" + current.id +
                "&stop=" + stop + "&line=" + Uri.EscapeDataString(line);
            try
            {
                using (var response = await Client.GetAsync(url, request.Token))
                {
                    var body = await response.Content.ReadAsStringAsync();
                    if (request.IsCancellationRequested || IsDisposed) return;
                    if (!response.IsSuccessStatusCode)
                    {
                        liveStatus.Text = "Canlı kaynak yanıt vermedi (HTTP " + (int)response.StatusCode + ").";
                        return;
                    }
                    var result = TransitData.Parse<ArrivalResult>(body);
                    if (result?.rows == null)
                    {
                        liveStatus.Text = "Canlı kaynak geçersiz yanıt verdi.";
                        return;
                    }
                    var timestamp = DateTimeOffset.FromUnixTimeMilliseconds(result.receivedAt).ToOffset(TimeSpan.FromHours(3));
                    liveStatus.Text = result.source + " · " + timestamp.ToString("dd.MM.yyyy HH:mm") + " TSİ";
                    if (!string.IsNullOrWhiteSpace(result.message)) liveStatus.Text += " · " + result.message;
                    latest = result;
                    liveMap.Enabled = result.stop?.Valid == true || (result.mapRows?.Any(row => row.Valid) ?? false);
                    MaybeNotify(result);
                    arrivals.Items.Clear();
                    if (result.rows.Count == 0) liveStatus.Text += " · Kaynak yaklaşan araç göstermedi.";
                    foreach (var row in result.rows)
                        arrivals.Items.Add(new ListViewItem(new[] { row.line, row.eta, row.name }));
                }
            }
            catch (OperationCanceledException)
            {
                if (!request.IsCancellationRequested && !IsDisposed) liveStatus.Text = "Sorgu zaman aşımına uğradı.";
            }
            catch (Exception ex) when (ex is HttpRequestException || ex is InvalidOperationException || ex is ArgumentException)
            {
                if (!request.IsCancellationRequested && !IsDisposed)
                    liveStatus.Text = "Bağlantı kurulamadı. Ağ ve TLS ayarlarını kontrol et.";
            }
            finally
            {
                if (ReferenceEquals(pending, request))
                {
                    pending = null;
                    if (!request.IsCancellationRequested && !IsDisposed) ScheduleRefresh();
                }
                request.Dispose();
            }
        }
    }
}
