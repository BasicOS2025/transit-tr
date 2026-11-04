package main

import (
	"encoding/json"
	"math"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

func TestCoordinates(t *testing.T) {
	for _, q := range []string{"lat=NaN&lon=1", "lat=91&lon=1", "lat=0&lon=Inf", "lat=0&lon=0&type=evil", "lat=&lon=1"} {
		r := httptest.NewRecorder()
		nearby(r, httptest.NewRequest("GET", "/?"+q, nil))
		if r.Code != 400 {
			t.Fatalf("%s: %d", q, r.Code)
		}
	}
}
func TestDistance(t *testing.T) {
	if haversine(1, 1, 1, 1) != 0 || math.Abs(haversine(0, 0, 0, 1)-111195) > 2 {
		t.Fatal("distance")
	}
}
func TestNearbyUpstreamAndCache(t *testing.T) {
	calls := 0
	s := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		calls++
		if r.Header.Get("User-Agent") == "" {
			t.Error("no UA")
		}
		r.ParseForm()
		if !strings.Contains(r.Form.Get("data"), "around:1200") {
			t.Error("query")
		}
		w.Write([]byte(`{"elements":[{"lat":39.921,"lon":32.85,"tags":{"name":"A","public_transport":"platform"}},{"lat":39.9201,"lon":32.85,"tags":{"name":"B","amenity":"pharmacy"}}]}`))
	}))
	defer s.Close()
	old := upstreamURL
	upstreamURL = s.URL
	defer func() { upstreamURL = old }()
	cache = map[string]Entry{}
	for i := 0; i < 2; i++ {
		w := httptest.NewRecorder()
		nearby(w, httptest.NewRequest("GET", "/?lat=39.92&lon=32.85", nil))
		if w.Code != 200 {
			t.Fatal(w.Code)
		}
		var d struct{ Places []Place }
		if json.Unmarshal(w.Body.Bytes(), &d) != nil || len(d.Places) != 2 || d.Places[0].Name != "B" {
			t.Fatal(w.Body.String())
		}
		if i == 1 && w.Header().Get("X-Transit-Cache") != "HIT" {
			t.Fatal("cache")
		}
	}
	if calls != 1 {
		t.Fatal(calls)
	}
}
func TestUpstreamFailure(t *testing.T) {
	s := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) { w.WriteHeader(503) }))
	defer s.Close()
	old := upstreamURL
	upstreamURL = s.URL
	defer func() { upstreamURL = old }()
	cache = map[string]Entry{}
	w := httptest.NewRecorder()
	nearby(w, httptest.NewRequest("GET", "/?lat=40&lon=30", nil))
	if w.Code != 503 {
		t.Fatal(w.Code)
	}
}
