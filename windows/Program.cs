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
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            if (Environment.OSVersion.Version.Major == 6 && Environment.OSVersion.Version.Minor == 1)
                ServicePointManager.SecurityProtocol = SecurityProtocolType.Tls12;
            try { Application.Run(new TransitForm()); return 0; }
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

    public sealed class Entry
    {
        public long id { get; set; }
        public string code { get; set; }
        public string name { get; set; }
        public string type { get; set; }
        public double? lat { get; set; }
        public double? lon { get; set; }
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
    }

    public sealed class ArrivalResult
    {
        public string source { get; set; }
        public long receivedAt { get; set; }
        public string message { get; set; }
        public List<Arrival> rows { get; set; }
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
        private readonly Button maps = new Button { Text = "OpenStreetMap’te aç", AutoSize = true };
        private readonly Button goLive = new Button { Text = "Durak varışlarına git", AutoSize = true };
        private readonly Label summary = new Label { AutoSize = true };
        private readonly Label liveStatus = new Label { AutoSize = true, MaximumSize = new Size(740, 0) };
        private readonly TextBox stopCode = new TextBox { Width = 110, AccessibleName = "Durak kodu", MaxLength = 8 };
        private readonly TextBox lineCode = new TextBox { Width = 130, AccessibleName = "Hat kodu", MaxLength = 80 };
        private readonly ListView arrivals = new ListView { Dock = DockStyle.Fill, View = View.Details, FullRowSelect = true };
        private readonly System.Windows.Forms.Timer searchDelay = new System.Windows.Forms.Timer { Interval = 180 };
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

            var title = new Label { Text = "Transit TR", AutoSize = true, Font = new Font(Font.FontFamily, 20, FontStyle.Bold) };
            var header = Bar(title, new Label { Text = "Şehir", AutoSize = true, Margin = new Padding(22, 10, 6, 0) }, city);
            header.BackColor = Color.FromArgb(229, 239, 252);
            Controls.Add(tabs);
            Controls.Add(header);
            MakeCatalogueTab();
            MakeLiveTab();

            city.DataSource = TransitData.Read<List<City>>("cities.json.gz");
            city.SelectedIndexChanged += async (sender, args) => await ChangeCityAsync();
            search.TextChanged += (sender, args) => { searchDelay.Stop(); searchDelay.Start(); };
            searchDelay.Tick += (sender, args) => { searchDelay.Stop(); Filter(); };
            Shown += async (sender, args) => await ChangeCityAsync();
            FormClosed += (sender, args) => { pending?.Cancel(); searchDelay.Dispose(); };
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
            maps.Click += (sender, args) =>
            {
                var item = entries.SelectedItem as Entry;
                if (item == null || !item.lat.HasValue || !item.lon.HasValue) return;
                string link = string.Format(CultureInfo.InvariantCulture,
                    "https://www.openstreetmap.org/?mlat={0}&mlon={1}#map=17/{0}/{1}", item.lat.Value, item.lon.Value);
                try { Process.Start(new ProcessStartInfo(link) { UseShellExecute = true }); }
                catch (System.ComponentModel.Win32Exception ex) { MessageBox.Show(ex.Message, "Harita"); }
            };
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
            arrivals.Columns.Add("Varış", 120);
            arrivals.Columns.Add("Güzergâh", 490);
            var statusBar = Bar(liveStatus);
            statusBar.Dock = DockStyle.Bottom;
            track.Click += async (sender, args) => await QueryAsync();
            page.Controls.Add(arrivals);
            page.Controls.Add(statusBar);
            page.Controls.Add(Bar(new Label { Text = "Durak kodu", AutoSize = true }, stopCode,
                new Label { Text = "Hat (isteğe bağlı)", AutoSize = true }, lineCode, track));
            tabs.TabPages.Add(page);
        }

        private async Task ChangeCityAsync()
        {
            pending?.Cancel();
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
            maps.Enabled = isStop && selected?.lat != null && selected?.lon != null;
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

        private async Task QueryAsync()
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
            pending?.Cancel();
            var request = new CancellationTokenSource();
            pending = request;
            arrivals.Items.Clear();
            liveStatus.Text = "Varışlar alınıyor…";
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
                    var timestamp = DateTimeOffset.FromUnixTimeMilliseconds(result.receivedAt).ToOffset(TimeSpan.FromHours(3));
                    liveStatus.Text = result.source + " · " + timestamp.ToString("dd.MM.yyyy HH:mm") + " TSİ";
                    if (!string.IsNullOrWhiteSpace(result.message)) liveStatus.Text += " · " + result.message;
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
                if (ReferenceEquals(pending, request)) pending = null;
                request.Dispose();
            }
        }
    }
}
