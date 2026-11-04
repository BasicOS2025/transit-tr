package main

import (
	"encoding/json"
	"fmt"
	"io"
	"log"
	"math"
	"net/http"
	"net/url"
	"os"
	"path/filepath"
	"sort"
	"strconv"
	"strings"
	"sync"
	"time"
)

type Place struct {
	Name     string  `json:"name"`
	Type     string  `json:"type"`
	Lat      float64 `json:"lat"`
	Lon      float64 `json:"lon"`
	Distance int     `json:"distance_m"`
}
type Entry struct {
	Data []byte
	At   time.Time
}

var mu sync.Mutex
var cache = map[string]Entry{}
var upstreamGate = make(chan struct{}, 1)
var upstreamURL = "https://overpass.private.coffee/api/interpreter"
var client = &http.Client{Timeout: 12 * time.Second}

func nearby(w http.ResponseWriter, r *http.Request) {
	if r.Method != "GET" {
		http.Error(w, "method not allowed", 405)
		return
	}
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	lat, e1 := strconv.ParseFloat(r.URL.Query().Get("lat"), 64)
	lon, e2 := strconv.ParseFloat(r.URL.Query().Get("lon"), 64)
	if e1 != nil || e2 != nil || math.Abs(lat) > 90 || math.Abs(lon) > 180 || math.IsNaN(lat) || math.IsNaN(lon) {
		http.Error(w, `{"error":"invalid coordinates"}`, 400)
		return
	}
	kind := r.URL.Query().Get("type")
	if kind == "" {
		kind = "all"
	}
	if kind != "all" && kind != "transport" && kind != "pharmacy" && kind != "hospital" && kind != "market" && kind != "atm" && kind != "park" {
		http.Error(w, `{"error":"invalid type"}`, 400)
		return
	}
	// Grid cache avoids repeated requests to the community Overpass service.
	key := fmt.Sprintf("%.5f:%.5f:%s", lat, lon, kind)
	mu.Lock()
	v, ok := cache[key]
	mu.Unlock()
	if ok && time.Since(v.At) < 10*time.Minute {
		w.Header().Set("X-Transit-Cache", "HIT")
		w.Write(v.Data)
		return
	}
	select {
	case upstreamGate <- struct{}{}:
		defer func() { <-upstreamGate }()
	default:
		w.Header().Set("Retry-After", "30")
		http.Error(w, `{"error":"busy; retry later"}`, 429)
		return
	}
	filters := map[string]string{"transport": `["public_transport"]`, "pharmacy": `["amenity"="pharmacy"]`, "hospital": `["amenity"~"hospital|clinic"]`, "market": `["shop"~"supermarket|convenience"]`, "atm": `["amenity"="atm"]`, "park": `["leisure"="park"]`}
	var parts []string
	if kind == "all" {
		for _, k := range []string{"transport", "pharmacy", "hospital", "market", "atm", "park"} {
			parts = append(parts, fmt.Sprintf("nwr(around:1200,%.5f,%.5f)%s;", lat, lon, filters[k]))
		}
	} else {
		parts = []string{fmt.Sprintf("nwr(around:1200,%.5f,%.5f)%s;", lat, lon, filters[kind])}
	}
	query := "[out:json][timeout:10];(" + strings.Join(parts, "") + ");out center 100;"
	req, _ := http.NewRequest("POST", upstreamURL, strings.NewReader("data="+url.QueryEscape(query)))
	req.Header.Set("User-Agent", "TransitTR/0.4 (keyless nearby prototype)")
	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	resp, err := client.Do(req)
	if err != nil {
		http.Error(w, `{"error":"upstream unavailable"}`, 502)
		return
	}
	defer resp.Body.Close()
	if resp.StatusCode != 200 {
		http.Error(w, `{"error":"upstream busy"}`, 503)
		return
	}
	var raw struct {
		Elements []struct {
			Lat    float64 `json:"lat"`
			Lon    float64 `json:"lon"`
			Center *struct {
				Lat float64 `json:"lat"`
				Lon float64 `json:"lon"`
			} `json:"center"`
			Tags map[string]string `json:"tags"`
		} `json:"elements"`
	}
	if err = json.NewDecoder(io.LimitReader(resp.Body, 2<<20)).Decode(&raw); err != nil {
		http.Error(w, `{"error":"invalid upstream response"}`, 502)
		return
	}
	places := []Place{}
	for _, el := range raw.Elements {
		a, b := el.Lat, el.Lon
		if el.Center != nil {
			a, b = el.Center.Lat, el.Center.Lon
		}
		if a == 0 && b == 0 {
			continue
		}
		name := el.Tags["name"]
		if name == "" {
			continue
		}
		typ := el.Tags["public_transport"]
		if typ == "" {
			typ = el.Tags["amenity"]
		}
		if typ == "" {
			typ = el.Tags["shop"]
		}
		if typ == "" {
			typ = el.Tags["leisure"]
		}
		d := haversine(lat, lon, a, b)
		places = append(places, Place{name, typ, a, b, int(d)})
	}
	sort.Slice(places, func(i, j int) bool { return places[i].Distance < places[j].Distance })
	if len(places) > 40 {
		places = places[:40]
	}
	data, _ := json.Marshal(map[string]interface{}{"source": "OpenStreetMap contributors / Overpass", "live": false, "places": places, "radius_m": 1200, "limited_results": true, "distance_kind": "straight_line", "attribution_url": "https://www.openstreetmap.org/copyright"})
	mu.Lock()
	if len(cache) > 1000 {
		cache = map[string]Entry{}
	}
	cache[key] = Entry{data, time.Now()}
	mu.Unlock()
	w.Write(data)
}
func haversine(a, b, c, d float64) float64 {
	const R = 6371000
	rad := math.Pi / 180
	dl := (c - a) * rad
	dn := (d - b) * rad
	x := math.Sin(dl/2)*math.Sin(dl/2) + math.Cos(a*rad)*math.Cos(c*rad)*math.Sin(dn/2)*math.Sin(dn/2)
	return 2 * R * math.Atan2(math.Sqrt(x), math.Sqrt(1-x))
}
func main() {
	mux := http.NewServeMux()
	mux.HandleFunc("/api/v1/nearby", nearby)
	mux.HandleFunc("/api/v1/statuses", statuses)
	mux.HandleFunc("/api/v1/ankara/stops", ankaraStops)
	mux.HandleFunc("/api/v1/ankara/arrivals", egoArrivals)
	mux.HandleFunc("/api/v1/cities", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{"mode": "static", "catalogs": []string{"ankara-ego-osm", "istanbul-iett", "istanbul-metro", "izmir-metro", "izmir-tram", "izmir-izban-archived", "konya-gtfs"}, "cities": []string{"ankara", "istanbul", "izmir", "kocaeli", "mugla", "eskisehir", "konya"}})
	})
	mux.HandleFunc("/api/v1/health", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.Write([]byte(`{"status":"ok","live":false}`))
	})
	web := os.Getenv("WEB_DIR")
	if web == "" {
		web = "../web"
	}
	web, _ = filepath.Abs(web)
	mux.Handle("/", http.FileServer(http.Dir(web)))
	p := os.Getenv("PORT")
	if p == "" {
		p = "8080"
	}
	log.Fatal((&http.Server{Addr: ":" + p, Handler: mux, ReadHeaderTimeout: 5 * time.Second, ReadTimeout: 10 * time.Second, WriteTimeout: 40 * time.Second, IdleTimeout: 30 * time.Second, MaxHeaderBytes: 8192}).ListenAndServe())
}
