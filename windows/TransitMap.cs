using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.IO;
using System.Linq;
using System.Net.Http;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace TransitTR.Windows
{
    public class MapPoint
    {
        public double? lat { get; set; }
        public double? lon { get; set; }
        public bool Valid => lat.HasValue && lon.HasValue && Math.Abs(lat.Value) < 85 &&
            Math.Abs(lon.Value) < 180 && lat.Value != 0 && lon.Value != 0;
    }

    public sealed class VehiclePoint : MapPoint
    {
        public string line { get; set; }
    }

    internal sealed class TransitMap : Control
    {
        private static readonly Color Ink = Color.FromArgb(18, 42, 67);
        private static readonly HttpClient Tiles = new HttpClient { Timeout = TimeSpan.FromSeconds(10) };
        private static readonly string Cache = Path.Combine(Environment.GetFolderPath(
            Environment.SpecialFolder.LocalApplicationData), "Transit TR", "tiles");
        private readonly Dictionary<string, Bitmap> images = new Dictionary<string, Bitmap>();
        private readonly HashSet<string> pending = new HashSet<string>();
        private readonly Dictionary<string, DateTime> failures = new Dictionary<string, DateTime>();
        private readonly List<VehiclePoint> vehicles = new List<VehiclePoint>();
        private List<MapPoint> path = new List<MapPoint>();
        private MapPoint stop;
        private int zoom = 13;
        private double latitude = 39.9255, longitude = 32.8538;
        private Point? drag;
        private bool active;
        private bool scheduled;

        static TransitMap()
        {
            Tiles.DefaultRequestHeaders.UserAgent.ParseAdd("TransitTR-Windows/1.0 (+https://github.com/BasicOS2025/transit-tr)");
        }

        internal TransitMap()
        {
            DoubleBuffered = true;
            Dock = DockStyle.Fill;
            BackColor = Color.FromArgb(232, 238, 242);
            TabStop = true;
            AccessibleName = "OpenStreetMap haritası";
            try
            {
                Directory.CreateDirectory(Cache);
                foreach (var stale in new DirectoryInfo(Cache).GetFiles("*.png")
                    .OrderByDescending(f => f.LastWriteTimeUtc).Skip(600)) stale.Delete();
            }
            catch (IOException) { }
            catch (UnauthorizedAccessException) { }
        }

        internal void ShowData(MapPoint selected, IEnumerable<VehiclePoint> positions, IEnumerable<MapPoint> route)
        {
            stop = selected;
            vehicles.Clear();
            if (positions != null) vehicles.AddRange(positions.Where(p => p.Valid).Take(250));
            path = route == null ? new List<MapPoint>() : route.Where(p => p.Valid).ToList();
            var center = stop != null && stop.Valid ? stop : path.FirstOrDefault() ?? vehicles.FirstOrDefault();
            if (center != null)
            {
                latitude = center.lat.Value;
                longitude = center.lon.Value;
                zoom = 14;
                var points = path.Concat(vehicles).ToList();
                while (zoom > 4 && points.Any(p =>
                {
                    var c = World(latitude, longitude);
                    var q = World(p.lat.Value, p.lon.Value);
                    return Math.Abs(c.X - q.X) > Math.Max(100, Width / 2 - 22) ||
                        Math.Abs(c.Y - q.Y) > Math.Max(100, Height / 2 - 22);
                })) zoom--;
            }
            active = true;
            Invalidate();
        }

        internal void ClearData()
        {
            active = false;
            stop = null;
            vehicles.Clear();
            path.Clear();
            Invalidate();
        }

        private PointF World(double lat, double lon)
        {
            double side = 256 * (1 << zoom);
            double sine = Math.Sin(Math.Max(-85, Math.Min(85, lat)) * Math.PI / 180);
            return new PointF((float)((lon + 180) / 360 * side),
                (float)((0.5 - Math.Log((1 + sine) / (1 - sine)) / (4 * Math.PI)) * side));
        }

        private PointF Screen(MapPoint p, PointF center)
        {
            var xy = World(p.lat.Value, p.lon.Value);
            return new PointF(xy.X - center.X + Width / 2f, xy.Y - center.Y + Height / 2f);
        }

        protected override void OnPaint(PaintEventArgs e)
        {
            base.OnPaint(e);
            if (!active)
            {
                TextRenderer.DrawText(e.Graphics, "Haritada görmek için bir durak, hat veya canlı sorgu seç.",
                    Font, ClientRectangle, Ink, TextFormatFlags.HorizontalCenter | TextFormatFlags.VerticalCenter |
                    TextFormatFlags.WordBreak);
                return;
            }

            var c = World(latitude, longitude);
            double left = c.X - Width / 2.0, top = c.Y - Height / 2.0;
            int firstX = (int)Math.Floor(left / 256), firstY = (int)Math.Floor(top / 256);
            int lastX = (int)Math.Floor((left + Width - 1) / 256), lastY = (int)Math.Floor((top + Height - 1) / 256);
            int count = 1 << zoom;
            for (int y = Math.Max(0, firstY); y <= Math.Min(count - 1, lastY); y++)
                for (int x = firstX; x <= lastX; x++)
                {
                    int wrapped = (x % count + count) % count;
                    string key = zoom + "-" + wrapped + "-" + y;
                    int px = (int)Math.Floor(x * 256 - left), py = (int)Math.Floor(y * 256 - top);
                    if (images.TryGetValue(key, out Bitmap image))
                        e.Graphics.DrawImageUnscaled(image, px, py);
                    else if (pending.Count < 6 && !pending.Contains(key) &&
                        (!failures.TryGetValue(key, out DateTime retry) || retry <= DateTime.UtcNow))
                        LoadTile(key, zoom, wrapped, y);
                }

            e.Graphics.SmoothingMode = SmoothingMode.AntiAlias;
            if (path.Count >= 2)
                using (var pen = new Pen(Color.FromArgb(0, 108, 181), 4) { LineJoin = LineJoin.Round })
                    e.Graphics.DrawLines(pen, path.Select(p => Screen(p, c)).ToArray());
            foreach (var vehicle in vehicles)
                DrawMarker(e.Graphics, Screen(vehicle, c), Brushes.RoyalBlue, vehicle.line);
            if (stop != null && stop.Valid)
                DrawMarker(e.Graphics, Screen(stop, c), Brushes.Black, null);
        }

        private void DrawMarker(Graphics g, PointF point, Brush brush, string label)
        {
            g.FillEllipse(Brushes.White, point.X - 7, point.Y - 7, 14, 14);
            g.FillEllipse(brush, point.X - 5, point.Y - 5, 10, 10);
            if (string.IsNullOrWhiteSpace(label)) return;
            var size = TextRenderer.MeasureText(label, Font);
            var target = new Rectangle((int)point.X + 8, (int)point.Y - size.Height / 2, size.Width, size.Height);
            g.FillRectangle(Brushes.White, target);
            TextRenderer.DrawText(g, label, Font, target, Ink, TextFormatFlags.NoPadding);
        }

        private async void LoadTile(string key, int z, int x, int y)
        {
            pending.Add(key);
            try
            {
                var bitmap = await Task.Run(async () =>
                {
                    string file = Path.Combine(Cache, key + ".png");
                    byte[] bytes;
                    if (File.Exists(file) && File.GetLastWriteTimeUtc(file) > DateTime.UtcNow.AddDays(-7))
                        bytes = File.ReadAllBytes(file);
                    else
                    {
                        using (var response = await Tiles.GetAsync("https://tile.openstreetmap.org/" + z + "/" + x + "/" + y + ".png"))
                        {
                            if (!response.IsSuccessStatusCode) return null;
                            bytes = await response.Content.ReadAsByteArrayAsync();
                            if (bytes.Length > 1024 * 1024) return null;
                            var policy = response.Headers.CacheControl;
                            if (policy == null || (!policy.NoStore && !policy.NoCache))
                            {
                                try
                                {
                                    File.WriteAllBytes(file, bytes);
                                    var age = policy?.MaxAge ?? (response.Content.Headers.Expires - DateTimeOffset.UtcNow);
                                    if (age.HasValue)
                                        File.SetLastWriteTimeUtc(file, DateTime.UtcNow.Add(age.Value).AddDays(-7));
                                }
                                catch (IOException) { }
                                catch (UnauthorizedAccessException) { }
                            }
                        }
                    }
                    using (var stream = new MemoryStream(bytes))
                    using (var item = Image.FromStream(stream))
                        return new Bitmap(item);
                });
                if (bitmap == null) failures[key] = DateTime.UtcNow.AddMinutes(2);
                else if (IsDisposed) bitmap.Dispose();
                else
                {
                    if (images.Count >= 96)
                    {
                        string remove = images.Keys.First();
                        images[remove].Dispose();
                        images.Remove(remove);
                    }
                    images[key] = bitmap;
                    failures.Remove(key);
                }
            }
            catch (Exception ex) when (ex is IOException || ex is UnauthorizedAccessException ||
                ex is HttpRequestException || ex is TaskCanceledException || ex is ArgumentException)
            {
                failures[key] = DateTime.UtcNow.AddMinutes(2);
            }
            finally
            {
                pending.Remove(key);
                if (IsHandleCreated && !IsDisposed && !scheduled)
                {
                    scheduled = true;
                    BeginInvoke(new Action(() => { scheduled = false; Invalidate(); }));
                }
            }
        }

        protected override void OnMouseDown(MouseEventArgs e)
        {
            base.OnMouseDown(e);
            Focus();
            if (active && e.Button == MouseButtons.Left) drag = e.Location;
        }

        protected override void OnMouseMove(MouseEventArgs e)
        {
            base.OnMouseMove(e);
            if (!drag.HasValue || e.Button != MouseButtons.Left) return;
            MoveBy(drag.Value.X - e.X, drag.Value.Y - e.Y);
            drag = e.Location;
        }

        protected override void OnMouseUp(MouseEventArgs e) { base.OnMouseUp(e); drag = null; }

        private void MoveBy(int x, int y)
        {
            var c = World(latitude, longitude);
            double side = 256 * (1 << zoom);
            longitude = (((c.X + x) % side + side) % side) / side * 360 - 180;
            latitude = Math.Atan(Math.Sinh(Math.PI * (1 - 2 * Math.Max(0, Math.Min(side, c.Y + y)) / side))) * 180 / Math.PI;
            Invalidate();
        }

        internal void ChangeZoom(int delta)
        {
            if (!active) return;
            zoom = Math.Min(19, Math.Max(4, zoom + delta));
            Invalidate();
        }

        protected override void OnMouseWheel(MouseEventArgs e)
        {
            base.OnMouseWheel(e);
            ChangeZoom(Math.Sign(e.Delta));
        }

        protected override void Dispose(bool disposing)
        {
            if (disposing)
            {
                foreach (var image in images.Values) image.Dispose();
                images.Clear();
            }
            base.Dispose(disposing);
        }
    }
}
